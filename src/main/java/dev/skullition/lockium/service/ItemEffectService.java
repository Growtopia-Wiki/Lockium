package dev.skullition.lockium.service;

import dev.skullition.lockium.client.GrowtopiaWikiClient;
import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCategory;
import dev.skullition.lockium.model.ItemEffect;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.util.ItemUtils;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * In-memory lookup for item effects (in-game "mods"), lazily backfilled from the Growtopia wiki.
 *
 * <p>Effects are loaded at startup from two sources:
 *
 * <ul>
 *   <li><b>Seed</b> – the curated {@code classpath:data/Effects.txt}; unreadable seed data fails
 *       startup.
 *   <li><b>Overlay</b> – the external file at {@code ${lockium.scraped-effects-path}}, holding
 *       only rows scraped at runtime; missing or unreadable overlay data is tolerated.
 * </ul>
 *
 * <p>Both files use {@code itemId|name|applyMessage|removeMessage}. Blank lines and lines beginning
 * with {@code //} are ignored. Exact effect records are deduplicated while distinct effects for an
 * item are retained in seed-first order.
 *
 * <p>When an item has no entry and belongs to {@link ItemCategory#CONSUMABLES} or {@link
 * ItemCategory#CLOTHES}, {@link #getEffects(GrowtopiaObject)} scrapes the item's raw wiki page. A
 * 404 or page without an {@code Item/Mod} template is negative-cached as an immutable empty list.
 * Transient HTTP failures are not cached and can be retried by a later lookup.
 *
 * <p>Thread safety: immutable effect lists are held in a {@link ConcurrentHashMap}. Atomic
 * {@code computeIfAbsent} calls serialize scrapes for the same ID, a read/write lock prevents a
 * runtime reload from racing with a scrape, and appends to the overlay are serialized separately.
 */
@Service
public class ItemEffectService {
  private static final Logger logger = LoggerFactory.getLogger(ItemEffectService.class);

  private static final int MAX_SCRAPED_FIELD_LENGTH = 1_000;
  private static final Pattern MOD_PATTERN =
      Pattern.compile("\\{\\{\\s*Item/Mod\\s*\\|([^|{}]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern ADDED_PATTERN =
      Pattern.compile("\\{\\{\\s*Added\\s*\\|([^|{}]*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern REMOVED_PATTERN =
      Pattern.compile("\\{\\{\\s*Removed\\s*\\|([^|{}]*)", Pattern.CASE_INSENSITIVE);

  private final Object fileLock = new Object();
  private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
  private final GrowtopiaWikiClient wikiClient;
  private final LockiumProperties lockiumProperties;
  private volatile ConcurrentHashMap<Integer, List<ItemEffect>> effects =
      new ConcurrentHashMap<>();

  /**
   * Creates the service.
   *
   * @param wikiClient client fetching raw wikitext from the public Growtopia wiki
   * @param lockiumProperties properties providing the scraped-effects overlay path
   */
  public ItemEffectService(GrowtopiaWikiClient wikiClient, LockiumProperties lockiumProperties) {
    this.wikiClient = wikiClient;
    this.lockiumProperties = lockiumProperties;
  }

  private static boolean qualifiesForScrape(GrowtopiaObject item) {
    ItemCategory category = item.getItemCategory();
    return category == ItemCategory.CONSUMABLES || category == ItemCategory.CLOTHES;
  }

  /** Extracts effect names and their index-paired apply/remove messages from raw wikitext. */
  static List<ItemEffect> parseWikitext(String wikitext) {
    List<String> names = findFirstParams(MOD_PATTERN, wikitext);
    List<String> applyMessages = findFirstParams(ADDED_PATTERN, wikitext);
    List<String> removeMessages = findFirstParams(REMOVED_PATTERN, wikitext);

    List<ItemEffect> parsed = new ArrayList<>(names.size());
    for (int i = 0; i < names.size(); i++) {
      String name = sanitize(names.get(i));
      if (name.isEmpty()) {
        continue;
      }
      parsed.add(
          new ItemEffect(
              name,
              i < applyMessages.size() ? sanitize(applyMessages.get(i)) : "",
              i < removeMessages.size() ? sanitize(removeMessages.get(i)) : ""));
    }
    return List.copyOf(parsed);
  }

  private static List<String> findFirstParams(Pattern pattern, String wikitext) {
    List<String> params = new ArrayList<>();
    Matcher matcher = pattern.matcher(wikitext);
    while (matcher.find()) {
      params.add(matcher.group(1).trim());
    }
    return params;
  }

  private static boolean needsLeadingLineBreak(Path path) throws IOException {
    if (!Files.exists(path) || Files.size(path) == 0) {
      return false;
    }
    try (var channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
      channel.position(channel.size() - 1);
      var buffer = ByteBuffer.allocate(1);
      channel.read(buffer);
      byte last = buffer.array()[0];
      return last != '\n' && last != '\r';
    }
  }

  /** Strips delimiters and line breaks and bounds untrusted scraped fields. */
  private static String sanitize(String field) {
    String sanitized = field.replace('|', ' ').replace('\r', ' ').replace('\n', ' ').trim();
    if (sanitized.length() <= MAX_SCRAPED_FIELD_LENGTH) {
      return sanitized;
    }
    return sanitized.substring(0, MAX_SCRAPED_FIELD_LENGTH).trim();
  }

  /**
   * Loads the seed and overlay files into memory. Invoked automatically by Spring after
   * construction.
   *
   * @throws IllegalStateException if the seed resource cannot be read
   */
  @PostConstruct
  public void load() {
    var writeLock = stateLock.writeLock();
    writeLock.lock();
    try {
      Map<Integer, List<ItemEffect>> map = new HashMap<>();
      int seedCount = loadSeed(map);
      int overlayCount = loadOverlay(map);

      map.replaceAll((_, list) -> List.copyOf(list));
      effects = new ConcurrentHashMap<>(map);
      logger.info(
          "Loaded {} item effects for {} items ({} from scrape overlay).",
          seedCount + overlayCount,
          effects.size(),
          overlayCount);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Returns whether looking up this item's effects would hit the wiki.
   *
   * <p>Command handlers use this to defer their reply before an uncached HTTP round-trip.
   *
   * @param item the placed item data
   * @return {@code true} if the item has no cached entry and its category qualifies for scraping
   */
  public boolean requiresScrape(GrowtopiaObject item) {
    var readLock = stateLock.readLock();
    readLock.lock();
    try {
      return !effects.containsKey(item.id()) && qualifiesForScrape(item);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Returns the effects of an item, scraping the wiki on a cache miss when the category qualifies.
   *
   * @param item the placed item data
   * @return the item's effects; empty if it has none or none could be determined
   */
  public List<ItemEffect> getEffects(GrowtopiaObject item) {
    var readLock = stateLock.readLock();
    readLock.lock();
    try {
      List<ItemEffect> known = effects.get(item.id());
      if (known != null) {
        return known;
      }
      if (!qualifiesForScrape(item)) {
        return List.of();
      }

      List<ItemEffect> resolved = effects.computeIfAbsent(item.id(), _ -> scrape(item));
      return resolved == null ? List.of() : resolved;
    } finally {
      readLock.unlock();
    }
  }

  /** Reloads the seed and overlay files at runtime. */
  public void reload() {
    load();
  }

  /** Returns the number of items with a cached entry, including negative-cached items. */
  public int size() {
    var readLock = stateLock.readLock();
    readLock.lock();
    try {
      return effects.size();
    } finally {
      readLock.unlock();
    }
  }

  private int loadSeed(Map<Integer, List<ItemEffect>> map) {
    var resource = new ClassPathResource("data/Effects.txt");
    try (var reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      return loadFrom(reader, map);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load Effects.txt", e);
    }
  }

  private int loadOverlay(Map<Integer, List<ItemEffect>> map) {
    Path overlayPath = Path.of(lockiumProperties.scrapedEffectsPath());
    if (!Files.exists(overlayPath)) {
      logger.debug("Scraped effects overlay does not exist at {}", overlayPath);
      return 0;
    }

    try (var reader = Files.newBufferedReader(overlayPath, StandardCharsets.UTF_8)) {
      return loadFrom(reader, map);
    } catch (IOException e) {
      logger.warn("Failed to load scraped effects overlay {}: {}", overlayPath, e.getMessage());
      return 0;
    }
  }

  /** Parses effect rows into a mutable staging map and returns the number of records added. */
  private int loadFrom(BufferedReader reader, Map<Integer, List<ItemEffect>> map)
      throws IOException {
    int added = 0;
    String rawLine;
    while ((rawLine = reader.readLine()) != null) {
      String line = rawLine.trim();
      if (line.isEmpty() || line.startsWith("//")) {
        continue;
      }

      String[] parts = line.split("\\|", -1);
      int id;
      try {
        id = Integer.parseInt(parts[0].trim());
      } catch (NumberFormatException e) {
        logger.warn("Ignoring malformed effects line: {}", line);
        continue;
      }
      if (parts.length < 2 || parts[1].trim().isEmpty()) {
        logger.warn("Ignoring effects line without a name: {}", line);
        continue;
      }

      var effect =
          new ItemEffect(
              parts[1].trim(),
              parts.length > 2 ? parts[2].trim() : "",
              parts.length > 3 ? parts[3].trim() : "");
      List<ItemEffect> existing = map.computeIfAbsent(id, _ -> new ArrayList<>());
      if (!existing.contains(effect)) {
        existing.add(effect);
        added++;
      }
    }
    return added;
  }

  /** Returns {@code null} only when a transient failure should not be cached. */
  private @Nullable List<ItemEffect> scrape(GrowtopiaObject item) {
    logger.debug("Scraping wiki effects for item {} ({}).", item.id(), item.name());

    String wikitext;
    try {
      wikitext = wikiClient.getRawPage(ItemUtils.getWikiItemName(item.name()));
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        logger.debug(
            "No wiki page for item {} ({}); caching as effectless.", item.id(), item.name());
        return List.of();
      }
      logger.warn(
          "Got RestClientResponseException for item {} ({}): {}",
          item.id(),
          item.name(),
          e.getMessage());
      return null;
    } catch (RestClientException e) {
      logger.warn(
          "Failed to scrape wiki page for item {} ({}): {}",
          item.id(),
          item.name(),
          e.getMessage());
      return null;
    }

    List<ItemEffect> parsed = parseWikitext(wikitext);
    if (parsed.isEmpty()) {
      logger.debug(
          "Wiki page for item {} ({}) has no mod templates; caching as effectless.",
          item.id(),
          item.name());
      return List.of();
    }

    if (appendScraped(item.id(), parsed)) {
      logger.info(
          "Scraped and persisted {} effect(s) for item {} ({}).",
          parsed.size(),
          item.id(),
          item.name());
    }
    return parsed;
  }

  /** Appends scraped effects to the overlay file so they survive restarts. */
  private boolean appendScraped(int itemId, List<ItemEffect> scraped) {
    var rows = new StringBuilder();
    for (ItemEffect effect : scraped) {
      rows.append(
          "%d|%s|%s|%s\n"
              .formatted(
                  itemId,
                  sanitize(effect.name()),
                  sanitize(effect.applyMessage()),
                  sanitize(effect.removeMessage())));
    }

    Path path = Path.of(lockiumProperties.scrapedEffectsPath());
    synchronized (fileLock) {
      try {
        Path parent = path.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        String prefix = needsLeadingLineBreak(path) ? "\n" : "";
        Files.writeString(
            path,
            prefix + rows,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
        return true;
      } catch (IOException e) {
        logger.warn(
            "Failed to persist scraped effects for item {} to {}: {}",
            itemId,
            path,
            e.getMessage());
        return false;
      }
    }
  }
}
