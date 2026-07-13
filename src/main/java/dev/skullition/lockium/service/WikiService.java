package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.util.ItemUtils;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * High-level facade used by Discord commands.
 *
 * <p>Provides convenient lookup methods over the cached data from {@link WikiDataService}. Handles
 * exact and normalized name matching, delegating all I/O and caching to the lower layer.
 *
 * <p>This class contains no caching annotations itself – it is safe to call from multiple command
 * threads.
 */
@Service
public class WikiService {
  private static final Logger logger = LoggerFactory.getLogger(WikiService.class);
  private final WikiDataService wiki;

  /**
   * Creates the facade.
   *
   * @param wiki the cached data service that performs all I/O
   */
  public WikiService(WikiDataService wiki) {
    this.wiki = wiki;
  }

  /**
   * Finds an item by name with fallback to normalized prefix search.
   *
   * <p>1. Exact (case-sensitive) match against the index.<br>
   * 2. If not found, performs an O(N) scan comparing each name normalized with {@link
   * ItemUtils#norm(String)} against the normalized input, returning the first prefix match.
   *
   * @param itemName user input; matched case-insensitively by the fallback scan
   * @return matching catalogue entry, or {@code null} if not found
   */
  @Nullable
  public ItemCatalogue findByName(String itemName) {

    var index = wiki.getNameIndex();
    ItemCatalogue exactMatch = index.get(itemName);
    if (exactMatch != null) {
      logger.debug(
          "findByName: exact match '{}' resolved to itemId={}", itemName, exactMatch.itemId());
      return exactMatch;
    }

    logger.debug("findByName: no exact match for '{}', falling back to prefix scan", itemName);

    // Fallback O(N) lookup - Might remove if laggy.
    String normalized = ItemUtils.norm(itemName);
    ItemCatalogue prefixMatch =
        index.entrySet().stream()
            .filter(entry -> ItemUtils.norm(entry.getKey()).startsWith(normalized))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    if (prefixMatch == null) {
      logger.debug("findByName: no prefix match for '{}'", normalized);
    } else {
      logger.debug(
          "findByName: prefix '{}' resolved to itemId={} ({})",
          normalized,
          prefixMatch.itemId(),
          prefixMatch.itemName());
    }
    return prefixMatch;
  }

  /**
   * Returns the cached full item catalogue.
   *
   * @return the complete items response; never {@code null}
   */
  public ItemsResponse getItems() {
    return wiki.getItems();
  }

  /**
   * Fetches detailed data for a catalogue entry.
   *
   * @param itemCatalogue catalogue entry from {@link #findByName(String)}
   * @return detail response containing item and seed
   */
  public ItemDetailResponse getItemDetail(ItemCatalogue itemCatalogue) {
    return wiki.getItemDetail(itemCatalogue.catalogueId());
  }

  /**
   * Returns the cached name index used for lookups and autocomplete.
   *
   * @return unmodifiable map of display name (item and seed) to catalogue entry
   */
  public Map<String, ItemCatalogue> getNameIndex() {
    return wiki.getNameIndex();
  }

  /** Delegates health check to the data layer. */
  public void health() {
    wiki.health();
  }
}
