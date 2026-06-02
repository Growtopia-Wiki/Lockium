package dev.skullition.lockium.service;

import dev.skullition.lockium.client.WikiClient;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Low-level data access layer for the Growtopia Wiki API.
 *
 * <p>Wraps {@link WikiClient} with Spring Cache abstractions:
 *
 * <ul>
 *   <li>{@code items} – cached for 6h, stores the full {@link ItemsResponse}
 *   <li>{@code itemIndex} – cached map {@code name → ItemCatalogue}, key {@code 'byName'}
 * </ul>
 *
 * <p>This class performs no business logic; it only fetches and caches.
 */
@Service
public class WikiDataService {
  private static final Logger logger = LoggerFactory.getLogger(WikiDataService.class);
  private final WikiClient client;

  /** Takes in client for declarative HTTP client. */
  public WikiDataService(WikiClient client) {
    this.client = client;
  }

  // Methods to actually call

  /**
   * Returns the cached item list, fetching from the API on cache miss.
   *
   * @return full items response; never {@code null}
   */
  @Cacheable(value = "items")
  public ItemsResponse getItems() {
    logger.info("Items cache is empty, fetching...");
    return client.getItems();
  }

  @Cacheable(value = "itemIndex", key = "'byName'", sync = true)
  public Map<String, ItemCatalogue> getNameIndex() {
    return buildIndex(getItems());
  }

  /**
   * Fetches detailed data for a single item. Not cached – details change rarely but are requested
   * infrequently.
   *
   * @param id item ID
   * @return detail response containing item and seed
   */
  public ItemDetailResponse getItemDetail(int id) {
    return client.getItemDetail(id);
  }

  /** Simple health probe – delegates to {@link WikiClient#health()}. */
  public void health() {
    client.health();
  }

  // Background Writes

  /**
   * Forces a refresh of the {@code items} cache.
   *
   * @return fresh items response from the API
   */
  @CachePut(value = "items")
  public ItemsResponse refreshItems() {
    logger.info("Refreshing items...");
    return client.getItems();
  }

  /**
   * Forces a refresh of the {@code items} cache.
   *
   * @return fresh items response from the API
   */
  @CachePut(value = "itemIndex", key = "'byName'")
  public Map<String, ItemCatalogue> refreshNameIndex(ItemsResponse items) {
    return buildIndex(items);
  }

  private Map<String, ItemCatalogue> buildIndex(ItemsResponse itemsResponse) {
    return itemsResponse.items().values().stream()
        .flatMap(
            item -> {
              Stream<Map.Entry<String, ItemCatalogue>> base =
                  Stream.of(Map.entry(item.itemName(), item));

              if (item.seedName() != null) {
                return Stream.concat(base, Stream.of(Map.entry(item.seedName(), item)));
              }
              return base;
            })
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, Map.Entry::getValue, (first, _) -> first));
  }
}
