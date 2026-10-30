package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests fixed recycle values, recycling eligibility, rarity boundaries, and the per-item cap. */
class RecycleUtilTests {

  @ParameterizedTest
  @CsvSource({
    "1400, 4000",
    "3710, 0",
    "2950, 20000",
    "1714, 167",
    "2034, 1",
    "7588, 1000",
    "4960, 5000",
    "914, 1",
    "916, 1",
    "918, 1",
    "920, 1",
    "922, 1",
    "924, 1",
    "1962, 1",
    "2038, 1",
    "2206, 1",
    "4444, 1",
    "2370, 400",
    "2372, 400",
    "2374, 400",
    "2376, 400",
    "2378, 400"
  })
  void fixedValuesOverrideMissingRarityAndRecipe(int itemId, int gems) {
    assertTrue(RecycleUtil.hasFixedRecycleValue(itemId));
    assertEquals(
        new RecycleUtil.RecycleResult(gems, gems, 3L * gems), calculate(itemId, 999, false, 3));
  }

  @Test
  void fixedValuesOverrideRarityEstimates() {
    assertEquals(new RecycleUtil.RecycleResult(4_000, 4_000, 8_000), calculate(1400, 100, true, 2));
  }

  @Test
  void fixedValuesMultiplyWithoutNarrowingTheTotal() {
    assertEquals(
        new RecycleUtil.RecycleResult(20_000, 20_000, 4_000_000_000L),
        calculate(2950, 999, false, 200_000));
  }

  @ParameterizedTest
  @CsvSource({"5208", "1401", "9078", "2194"})
  void missingValuesSeedsAndUnlistedVariantsAreNotAssumed(int itemId) {
    assertFalse(RecycleUtil.hasFixedRecycleValue(itemId));
    assertEquals(new RecycleUtil.RecycleResult(0, 0, 0), calculate(itemId, 999, false, 100));
  }

  @Test
  void raritylessItemRecyclesForNothing() {
    assertEquals(new RecycleUtil.RecycleResult(0, 0, 0), calculate(999, true));
  }

  @Test
  void itemWithoutSplicingRecipeRecyclesForNothing() {
    assertEquals(new RecycleUtil.RecycleResult(0, 0, 0), calculate(100, false));
  }

  @ParameterizedTest
  @CsvSource({"30, 6, 205", "31, 8, 305", "100, 26, 1205", "2000, 251, 12505"})
  void calculatesRarityRangeAndTotal(int rarity, int rangeMax, long gemCount) {
    assertEquals(new RecycleUtil.RecycleResult(0, rangeMax, gemCount), calculate(rarity, true));
  }

  private static RecycleUtil.RecycleResult calculate(int rarity, boolean hasRecipe) {
    return calculate(0, rarity, hasRecipe, 100);
  }

  private static RecycleUtil.RecycleResult calculate(
      int itemId, int rarity, boolean hasRecipe, long itemCount) {
    GrowtopiaObject item = mock(GrowtopiaObject.class);
    GrowtopiaObject seed = mock(GrowtopiaObject.class);
    ItemDetailResponse detail = mock(ItemDetailResponse.class);
    when(item.id()).thenReturn(itemId);
    when(item.rarity()).thenReturn(rarity);
    when(seed.recipe1()).thenReturn(hasRecipe ? new GrowtopiaObject.RecipeInfo(1) : null);
    when(detail.item()).thenReturn(item);
    when(detail.seed()).thenReturn(seed);
    return RecycleUtil.getRecycleValueForItem(detail, itemCount);
  }
}
