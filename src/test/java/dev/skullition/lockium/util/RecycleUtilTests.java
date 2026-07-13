package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests recycling eligibility, rarity boundaries, and the per-item cap. */
class RecycleUtilTests {

  @Test
  void raritylessItemRecyclesForNothing() {
    assertEquals(new RecycleUtil.RecycleResult(0, 0, 0), calculate(999, true, 100));
  }

  @Test
  void itemWithoutSplicingRecipeRecyclesForNothing() {
    assertEquals(new RecycleUtil.RecycleResult(0, 0, 0), calculate(100, false, 100));
  }

  @ParameterizedTest
  @CsvSource({
    "30, 6, 205",
    "31, 8, 305",
    "100, 26, 1205",
    "2000, 251, 12505"
  })
  void calculatesRarityRangeAndTotal(int rarity, int rangeMax, long gemCount) {
    assertEquals(
        new RecycleUtil.RecycleResult(0, rangeMax, gemCount), calculate(rarity, true, 100));
  }

  private static RecycleUtil.RecycleResult calculate(
      int rarity, boolean hasRecipe, long itemCount) {
    GrowtopiaObject item = mock(GrowtopiaObject.class);
    GrowtopiaObject seed = mock(GrowtopiaObject.class);
    ItemDetailResponse detail = mock(ItemDetailResponse.class);
    when(item.rarity()).thenReturn(rarity);
    when(seed.recipe1()).thenReturn(hasRecipe ? new GrowtopiaObject.RecipeInfo(1) : null);
    when(detail.item()).thenReturn(item);
    when(detail.seed()).thenReturn(seed);
    return RecycleUtil.getRecycleValueForItem(detail, itemCount);
  }
}
