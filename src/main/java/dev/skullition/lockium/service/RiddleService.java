package dev.skullition.lockium.service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * In-memory lookup for ancestral riddles.
 *
 * <p>Data is loaded once at startup from {@code classpath:data/AncestralRiddles.txt} (format
 * {@code description|itemId|count}). The file is static reference data used by {@code /gt riddle}
 * to find which block an in-game ancestral riddle requires.
 *
 * <p>Lines starting with {@code //} or blank lines are ignored. Malformed lines are logged and
 * skipped.
 */
@Service
public class RiddleService {
  private final Logger logger = LoggerFactory.getLogger(RiddleService.class);
  private volatile List<Riddle> riddles = List.of();

  /**
   * A single ancestral riddle.
   *
   * @param description the riddle text shown in-game
   * @param itemId the in-game item ID of the required block
   * @param count how many of the item the riddle requires
   */
  public record Riddle(String description, int itemId, int count) {}

  /**
   * Loads the file into memory. Invoked automatically by Spring after construction.
   *
   * @throws IllegalStateException if the resource cannot be read
   */
  @PostConstruct
  public void load() {
    List<Riddle> list = new ArrayList<>();
    var resource = new ClassPathResource("data/AncestralRiddles.txt");

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
                if (parts.length == 3) {
                  try {
                    int itemId = Integer.parseInt(parts[1].trim());
                    int count = Integer.parseInt(parts[2].trim());
                    list.add(new Riddle(parts[0].trim(), itemId, count));
                  } catch (NumberFormatException ignored) {
                    logger.info("Ignoring line: {}", line);
                  }
                } else {
                  logger.info("Ignoring line: {}", line);
                }
              });
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load AncestralRiddles.txt", e);
    }

    riddles = List.copyOf(list);
    logger.info("Loaded {} ancestral riddles.", riddles.size());
  }

  /**
   * Finds riddles whose description contains the given text.
   *
   * @param query part of the riddle text; matched case-insensitively
   * @return all matching riddles, possibly empty
   */
  public List<Riddle> search(String query) {
    String needle = query.trim().toLowerCase(Locale.US);
    return riddles.stream()
        .filter(riddle -> riddle.description().toLowerCase(Locale.US).contains(needle))
        .toList();
  }

  /** Reloads the file at runtime. Useful for admin commands without a restart. */
  public void reload() {
    load();
  }

  /** Returns the number of riddles currently loaded. */
  public int size() {
    return riddles.size();
  }
}
