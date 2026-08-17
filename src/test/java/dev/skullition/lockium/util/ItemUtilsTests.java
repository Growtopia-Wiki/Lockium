package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.GrowtopiaObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests item-name, URL, duration, and drop-calculation helpers. */
class ItemUtilsTests {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock = """
      Plain Dirt | Plain Dirt
      `6Colored Dirt | Colored Dirt
      ``Reset Dirt | Reset Dirt
      ` | ''
      """)
  void stripsGrowtopiaColorCodes(String input, String expected) {
    assertEquals(expected, ItemUtils.stripColorCodes(input));
  }

  @Test
  void normalizesNamesWithStableCaseAndWhitespaceRules() {
    assertEquals("i dirt", ItemUtils.norm("  I DIRT  "));
  }

  @Test
  void buildsSpriteAndWikiNames() {
    assertEquals("https://cdn.growtopiawiki.com/sprites/42.png", ItemUtils.getItemSpriteUrl(42));
    assertEquals(
        "https://cdn.growtopiawiki.com/sprites/43-tree.png", ItemUtils.getTreeSpriteUrl(43));
    assertEquals("Bunny_Egg", ItemUtils.getWikiItemName("Bunny Egg"));
  }

  @ParameterizedTest
  @CsvSource({"-1, 0s", "0, 0s", "59, 59s", "90, 1m 30s", "90061, 1d 1h 1m 1s"})
  void formatsDurations(int seconds, String expected) {
    assertEquals(expected, ItemUtils.toDayHourMinutesSeconds(seconds));
  }

  @ParameterizedTest
  @CsvSource({"999, 0", "1, 1", "30, 2", "31, 3", "100, 12"})
  void calculatesAverageGemDropsAtRarityBoundaries(int rarity, double expected) {
    GrowtopiaObject item = mock(GrowtopiaObject.class);
    when(item.rarity()).thenReturn(rarity);

    assertEquals(expected, ItemUtils.getAverageGemCountToDropOnTreeSmash(item));
  }

  @ParameterizedTest
  @CsvSource({"999, 0", "1, 33.333333333333336", "30, 10", "31, 10"})
  void calculatesSeedDropChance(int rarity, double expected) {
    assertEquals(expected, ItemUtils.getChanceToDropSeedOnTreeSmash(rarity), 0.0000000001);
  }

  @ParameterizedTest
  @CsvSource({"1000, 0, 20", "1000, 99, 20", "1000, 100, 0", "1000, 999, 0", "0, 50, 0"})
  void calculatesDreamcatcherStaffExtraBlocksAtRarityBoundaries(
      double fruitCount, int rarity, double expected) {
    assertEquals(
        expected, ItemUtils.getExtraBlocksFromDreamcatcherStaff(fruitCount, rarity), 0.0000000001);
  }
}
