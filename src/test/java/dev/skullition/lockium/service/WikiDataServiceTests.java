package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.client.WikiClient;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemsResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests construction of the cached item-and-seed name index. */
class WikiDataServiceTests {

  @Test
  void nameIndexIncludesSeedsOmitsNullsAndKeepsFirstDuplicate() {
    ItemCatalogue first = new ItemCatalogue(1, 2, 3, "Dirt", "Shared Name");
    ItemCatalogue duplicate = new ItemCatalogue(2, 4, 5, "Shared Name", "Lava Seed");
    ItemCatalogue withoutSeed = new ItemCatalogue(3, 6, 0, "Bedrock", null);
    Map<Integer, ItemCatalogue> items = new LinkedHashMap<>();
    items.put(first.catalogueId(), first);
    items.put(duplicate.catalogueId(), duplicate);
    items.put(withoutSeed.catalogueId(), withoutSeed);
    ItemsResponse response = new ItemsResponse(items);
    WikiClient client = mock(WikiClient.class);
    when(client.getItems()).thenReturn(response);

    Map<String, ItemCatalogue> index = new WikiDataService(client).getNameIndex();

    assertEquals(4, index.size());
    assertSame(first, index.get("Dirt"));
    assertSame(first, index.get("Shared Name"));
    assertSame(duplicate, index.get("Lava Seed"));
    assertSame(withoutSeed, index.get("Bedrock"));
    verify(client).getItems();
  }
}
