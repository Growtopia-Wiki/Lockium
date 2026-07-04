package dev.skullition.lockium.service;

import dev.skullition.lockium.model.Chi;
import dev.skullition.lockium.util.ItemUtils;
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
 * In-memory lookup for item chi (elements).
 *
 * <p>Data is loaded once at startup from {@code classpath:data/ChiList.txt} (format {@code
 * itemId|CHI}). The dataset is incomplete – items without an entry simply have no known chi. On
 * every (re)load the map is pushed into {@link ItemUtils#setChiMap(Map)} so the shared item
 * container header can display the chi without the command call sites knowing about this service.
 *
 * <p>Lines starting with {@code //} or blank lines are ignored. Malformed lines are logged and
 * skipped.
 */
@Service
public class ChiService {
  private final Logger logger = LoggerFactory.getLogger(ChiService.class);
  private volatile Map<Integer, Chi> chiMap = Map.of();

  /**
   * Loads the file into memory and publishes it to {@link ItemUtils}. Invoked automatically by
   * Spring after construction.
   *
   * @throws IllegalStateException if the resource cannot be read
   */
  @PostConstruct
  public void load() {
    Map<Integer, Chi> map = new HashMap<>();
    var resource = new ClassPathResource("data/ChiList.txt");

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
                    map.put(id, Chi.fromString(parts[1]));
                  } catch (NumberFormatException ignored) {
                    logger.info("Ignoring line: {}", line);
                  }
                }
              });
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load ChiList.txt", e);
    }

    chiMap = Collections.unmodifiableMap(map);
    ItemUtils.setChiMap(chiMap);
    logger.info("Loaded {} chi entries.", chiMap.size());
  }

  /**
   * Returns the chi of an item.
   *
   * @param itemId the in-game item ID
   * @return the chi, or {@link Chi#NONE} if the item is not in the dataset
   */
  public Chi getChi(int itemId) {
    return chiMap.getOrDefault(itemId, Chi.NONE);
  }

  /** Reloads the file at runtime. Useful for admin commands without a restart. */
  public void reload() {
    load();
  }

  /** Returns number of entries currently loaded. */
  public int size() {
    return chiMap.size();
  }
}
