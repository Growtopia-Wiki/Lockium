package dev.skullition.lockium.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Tests Wiki item-index JSON conversion and name sanitization. */
class ItemsResponseTests {

  @Test
  void deserializesStringKeysPreservesOrderAndStripsColorCodes() throws Exception {
    String json =
        """
        {
          "items": {
            "10": {
              "itemId": 20,
              "seedId": 21,
              "itemName": "`6Dirt",
              "seedName": "`2Dirt Seed"
            },
            "11": {
              "itemId": 22,
              "seedId": 0,
              "itemName": "Bedrock"
            }
          }
        }
        """;

    ItemsResponse response = JsonMapper.builder().build().readValue(json, ItemsResponse.class);

    assertEquals(List.of(10, 11), response.items().keySet().stream().toList());
    assertEquals(
        new ItemCatalogue(10, 20, 21, "Dirt", "Dirt Seed"), response.items().get(10));
    assertEquals("Bedrock", response.items().get(11).itemName());
    assertNull(response.items().get(11).seedName());
  }
}
