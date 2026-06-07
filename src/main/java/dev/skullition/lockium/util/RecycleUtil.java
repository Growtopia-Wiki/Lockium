package dev.skullition.lockium.util;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemDetailResponse;

/** Helper util class to get gem value from recycled item. */
public class RecycleUtil {
  private RecycleUtil() {}

  /**
   * Code to calculate amount of gems received for recycling items.
   *
   * @param itemDetail the itemDetail object from the Wiki API
   * @param itemCount how many of the item you are recycling
   * @return range and calculated gemCount
   */
  public static RecycleResult getRecycleValueForItem(
      ItemDetailResponse itemDetail, long itemCount) {

    GrowtopiaObject item = itemDetail.item();
    GrowtopiaObject seed = itemDetail.seed();
    boolean r;
    int rangeMin = 0;
    int rangeMax = 0;

    // NOTE: original C# accessed item.Rarity before null-check – we guard first
    int rarity = item.rarity();

    if (rarity == 999) {
      r = true;
    } else if (rarity == 1) {
      r = false;
    } else {
      // item is null OR has splice components → use rarity-based range
      if (seed.recipe1() != null || seed.recipe2() != null) {
        rangeMax = rarity / 4;

        if (rarity <= 30) {
          rangeMax = (3 * rangeMax) / 4;
        }

        if (rangeMax > 250) {
          rangeMax = 250;
        }
        r = false;
      } else {
        r = true;
      }
    }

    long count;
    if (r) {
      count = 0;
      return new RecycleResult(rangeMin, rangeMax, count);
    } else {
      int med = (rangeMin + rangeMax) / 2;

      double randGems = itemCount * 0.05d; // 5% chance
      count = (long) ((itemCount * med) + randGems);

      return new RecycleResult(rangeMin, rangeMax + 1, count);
    }
  }

  /**
   * Result of the recycle calculation.
   *
   * @param rangeMin minimum gems per item (inclusive)
   * @param rangeMax maximum gems per item (exclusive in the original code, the method returns +1
   *     for the 5% gem chance)
   * @param gemCount total gems you would receive for {@code itemCount} items
   */
  public record RecycleResult(int rangeMin, int rangeMax, long gemCount) {}
}
