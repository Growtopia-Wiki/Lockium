package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests exact, normalized-prefix, and missing item lookups in {@link WikiService}. */
class WikiServiceTests {

  @Test
  void exactNameReturnsIndexedItem() {
    WikiDataService dataService = mock(WikiDataService.class);
    ItemCatalogue dirt = item(1, 2, 3, "Dirt", "Dirt Seed");
    when(dataService.getNameIndex()).thenReturn(Map.of("Dirt", dirt));

    ItemCatalogue result = new WikiService(dataService).findByName("Dirt");

    assertSame(dirt, result);
  }

  @Test
  void normalizedPrefixReturnsFirstIndexedMatch() {
    WikiDataService dataService = mock(WikiDataService.class);
    ItemCatalogue dirt = item(1, 2, 3, "Dirt", "Dirt Seed");
    ItemCatalogue dirtyBomb = item(2, 4, 5, "Dirty Bomb", "Dirty Bomb Seed");
    Map<String, ItemCatalogue> index = new LinkedHashMap<>();
    index.put("Dirt", dirt);
    index.put("Dirty Bomb", dirtyBomb);
    when(dataService.getNameIndex()).thenReturn(index);

    ItemCatalogue result = new WikiService(dataService).findByName("  dIr  ");

    assertSame(dirt, result);
  }

  @Test
  void missingNameReturnsNull() {
    WikiDataService dataService = mock(WikiDataService.class);
    when(dataService.getNameIndex()).thenReturn(Map.of("Dirt", item(1, 2, 3, "Dirt", null)));

    assertNull(new WikiService(dataService).findByName("Lava"));
  }

  @Test
  void itemDetailUsesCatalogueId() {
    WikiDataService dataService = mock(WikiDataService.class);
    ItemCatalogue item = item(42, 100, 101, "Test Item", "Test Item Seed");
    ItemDetailResponse response = mock(ItemDetailResponse.class);
    when(dataService.getItemDetail(42)).thenReturn(response);

    assertSame(response, new WikiService(dataService).getItemDetail(item));
    verify(dataService).getItemDetail(42);
  }

  private static ItemCatalogue item(
      int catalogueId, int itemId, int seedId, String itemName, String seedName) {
    return new ItemCatalogue(catalogueId, itemId, seedId, itemName, seedName);
  }
}
