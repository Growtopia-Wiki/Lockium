package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.skullition.lockium.util.ItemUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Root DTO for {@code GET /v1/items}.
 *
 * <p>The Wiki API returns a JSON object where the {@code items} field is a map keyed by
 * <em>strings</em> (e.g., "2", "4"). This record normalizes those keys to {@code Integer} and
 * preserves insertion order using {@link LinkedHashMap}.
 *
 * <p>Example JSON fragment:
 *
 * <pre>{@code
 * "items": {
 *   "2": {"itemId":2,"seedId":3,"itemName":"Dirt","seedName":"Dirt Seed"}
 * }
 * }</pre>
 *
 * @param items map of catalogue index -> minimal item data; never {@code null}, maintains the order
 *     returned by the API
 * @see ItemCatalogue
 */
public record ItemsResponse(@JsonProperty Map<Integer, ItemCatalogue> items) {

  /**
   * Jackson creator that converts the raw string-keyed map into typed objects.
   *
   * <p>This method is invoked automatically during deserialization. It parses each entry's key to
   * {@code int} and extracts {@code itemId}, {@code seedId}, {@code itemName}, and optional {@code
   * seedName}. Growtopia color codes (e.g. {@code `6}) are stripped from both names.
   *
   * @param rawItems the raw {@code items} node from JSON, keyed by String
   * @return a new {@code ItemsResponse} with integer keys
   * @throws NumberFormatException if a key cannot be parsed as an integer
   */
  @JsonCreator
  public static ItemsResponse fromJson(@JsonProperty("items") Map<String, JsonNode> rawItems) {
    Map<Integer, ItemCatalogue> items = new LinkedHashMap<>();

    rawItems.forEach(
        (key, node) -> {
          int catalogueId = Integer.parseInt(key);
          var item =
              new ItemCatalogue(
                  catalogueId,
                  node.get("itemId").asInt(),
                  node.get("seedId").asInt(),
                  ItemUtils.stripColorCodes(node.get("itemName").asString()),
                  node.get("seedName") != null
                      ? ItemUtils.stripColorCodes(node.get("seedName").asString())
                      : null);
          items.put(catalogueId, item);
        });

    return new ItemsResponse(items);
  }
}
