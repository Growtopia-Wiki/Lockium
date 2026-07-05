package dev.skullition.lockium.client;

import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.service.WikiService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative client for the Growtopia Wiki public API.
 *
 * <p>This is a thin HTTP facade – it performs no caching, no mapping, and no retry logic. All calls
 * are executed by Spring's {@code HttpServiceProxyFactory} on the {@code RestClient} configured in
 * {@code ClientConfig}.
 *
 * <p><b>Base URL:</b> {@code ${wiki.api.url}}<br>
 * <b>Auth:</b> {@code Authorization: Bearer ${wiki.api.key}}
 *
 * <p>All responses are JSON and map directly to the DTOs in {@code dev.skullition.lockium.model}.
 * For performance, use {@link WikiService}, which reads through the Caffeine caches (TTL from
 * {@code lockium.items-cache-duration}) added by the {@code WikiDataService} layer.
 */
@HttpExchange
public interface WikiClient {

  /**
   * Simple liveness probe.
   *
   * <p>Calls {@code GET /health}. The endpoint returns 200 with an empty body when the API is up.
   * Used by the {@code /ping} command via {@link WikiService#health()}.
   */
  @GetExchange("/health")
  void health();

  /**
   * Fetches the full item index.
   *
   * <p>Calls {@code GET /v1/items}. The payload is a single JSON object containing a map of
   * internal catalogue indexes to minimal item data ({@code itemId}, {@code seedId}, {@code
   * itemName}, and optional {@code seedName}).
   *
   * <p>This response is large and changes rarely, so callers should cache it.
   *
   * @return the complete index; never {@code null} – API failures surface as {@code
   *     RestClientException}
   * @see ItemsResponse
   */
  @GetExchange("/v1/items")
  ItemsResponse getItems();

  /**
   * Fetches detailed data for one item.
   *
   * <p>Calls {@code GET /v1/items/{id}}. Returns both the placed block ({@code item}) and its seed
   * form ({@code seed}) with full properties, categories, flags, and colors.
   *
   * @param id the catalogue index from {@code ItemCatalogue#catalogueId()} (not the in-game item
   *     ID); the API returns 404 for unknown IDs
   * @return the detail response; fields inside {@code item} and {@code seed} follow the nullability
   *     contract defined in {@link ItemDetailResponse}
   * @see ItemDetailResponse
   */
  @GetExchange("/v1/items/{id}")
  ItemDetailResponse getItemDetail(@PathVariable int id);
}
