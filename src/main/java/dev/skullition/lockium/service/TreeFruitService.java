package dev.skullition.lockium.service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * In-memory lookup for "tree fruit max drops".
 *
 * <p>Data is loaded once at startup from {@code classpath:data/TreeFruitMaxDrop.txt} (format {@code
 * itemId|drops}). The file is static reference data – not from the Wiki API – so we keep it in an
 * immutable {@link Map} to avoid disk I/O on every command that needs it (e.g. {@code /gt harvest}
 * and {@code /gt break}).
 *
 * <p>Lines starting with {@code //} or blank lines are ignored. Malformed lines are logged and
 * skipped.
 */
@Service
public class TreeFruitService {
  private final Logger logger = LoggerFactory.getLogger(TreeFruitService.class);
  private volatile Map<Integer, Integer> maxDrops = Map.of();

  /**
   * Loads the file into memory. Invoked automatically by Spring after construction.
   *
   * @throws IllegalStateException if the resource cannot be read
   */
  @PostConstruct
  public void load() {
    Map<Integer, Integer> map = new HashMap<>();
    var resource = new ClassPathResource("data/TreeFruitMaxDrop.txt");

    try (var reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      reader
          .lines()
          .map(String::trim)
          .filter(l -> !l.isEmpty() && !l.startsWith("//"))
          .forEach(
              line -> {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                  try {
                    int id = Integer.parseInt(parts[0].trim());
                    int drops = Integer.parseInt(parts[1].trim());
                    map.put(id, drops);
                  } catch (NumberFormatException ignored) {
                    logger.info("Ignoring line: {}", line);
                  }
                }
              });
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load TreeFruitMaxDrop.txt", e);
    }

    maxDrops = Collections.unmodifiableMap(map);
    logger.info("Loaded {} tree fruit entries.", maxDrops.size());
  }

  /**
   * Returns the maximum fruit yield for a tree.
   *
   * @param itemId the item ID of the tree block
   * @return configured max drops, or {@code 4} if the ID is not in the file (4 is the vanilla
   *     Growtopia default)
   */
  public int getMaxDrop(int itemId) {
    return maxDrops.getOrDefault(itemId, 4); // 4 = default not in file.
  }

  /** Reloads the file at runtime. Useful for admin commands without a restart. */
  public void reload() {
    load();
  }

  /** Returns the number of entries currently loaded. */
  public int size() {
    return maxDrops.size();
  }
}
