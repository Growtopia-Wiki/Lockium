package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.util.ItemUtils;
import java.util.Map;
import org.jspecify.annotations.Nullable;
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
  private final WikiDataService wiki;

  /**
   * Takes in a {@code wiki} param to call the cached data service.
   */
  public WikiService(WikiDataService wiki) {
    this.wiki = wiki;
  }

  /**
   * Finds an item by name with fallback to normalized prefix search.
   *
   * <p>1. Exact match against the index.<br>
   * 2. If not found, performs an O(N) scan using {@link ItemUtils#norm(String)} and returns the
   * first prefix match.
   *
   * @param itemName user input (case-insensitive)
   * @return matching catalogue entry, or {@code null} if not found
   */
  @Nullable
  public ItemCatalogue findByName(String itemName) {

    var index = wiki.getNameIndex();
    ItemCatalogue exactMatch = index.get(itemName);
    if (exactMatch != null) {
      return exactMatch;
    }

    // Fallback O(N) lookup - Might remove if laggy.
    return index.entrySet().stream()
        .filter(entry -> ItemUtils.norm(entry.getKey()).startsWith(itemName))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  public ItemsResponse getItems() {
    return wiki.getItems();
  }

  /** Takes in id and returns a {@link ItemDetailResponse}. */
  public ItemDetailResponse getItemDetail(int id) {
    return wiki.getItemDetail(id);
  }

  /**
   * Convenience overload that extracts the ID from a catalogue entry.
   *
   * @param itemCatalogue catalogue entry from {@link #findByName(String)}
   * @return detail response
   */
  public ItemDetailResponse getItemDetail(ItemCatalogue itemCatalogue) {
    return wiki.getItemDetail(itemCatalogue.catalogueId());
  }

  public Map<String, ItemCatalogue> getNameIndex() {
    return wiki.getNameIndex();
  }

  /** Delegates health check to the data layer. */
  public void health() {
    wiki.health();
  }
}
