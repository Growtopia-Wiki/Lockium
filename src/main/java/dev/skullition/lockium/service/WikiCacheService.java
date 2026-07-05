package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemsResponse;
import org.springframework.stereotype.Service;

/**
 * Orchestrates manual refresh of Wiki caches.
 *
 * <p>Lockium caches two expensive structures:
 *
 * <ol>
 *   <li>{@code items} – the raw {@link ItemsResponse} from {@code /v1/items}
 *   <li>{@code itemIndex} – a name→{@link dev.skullition.lockium.model.ItemCatalogue} map.
 * </ol>
 *
 * <p>This service ensures both are updated atomically with a single API call, avoiding a thundering
 * herd on the Wiki API.
 *
 * <p>Intended for use by admin commands or scheduled tasks.
 */
@Service
public class WikiCacheService {
  private final WikiDataService dataService;

  /**
   * Creates the cache orchestrator.
   *
   * @param dataService the low-level data service with {@code @CachePut} methods
   */
  public WikiCacheService(WikiDataService dataService) {
    this.dataService = dataService;
  }

  /**
   * Refreshes both caches with a single API call.
   *
   * <p>Fetches fresh data via {@link WikiDataService#refreshItems()} (updating the {@code items}
   * cache), then rebuilds the {@code itemIndex} cache from that same response.
   */
  public void refreshCaches() {
    // 1 API call gets the new data and updates the "items" cache
    ItemsResponse freshItems = dataService.refreshItems();

    // We pass that fresh data to update the "itemIndex" cache instantly
    dataService.refreshNameIndex(freshItems);
  }
}
