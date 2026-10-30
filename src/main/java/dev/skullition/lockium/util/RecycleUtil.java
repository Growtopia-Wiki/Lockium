package dev.skullition.lockium.util;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemDetailResponse;
import java.util.Map;

/**
 * Recycling calculation helpers.
 *
 * <p>Ports the game's gem-value logic for recycling items: the per-item gem range is derived from
 * rarity (with a 25% reduction at rarity ≤30 and a cap of 250). Known fixed values take precedence
 * over rarity and splicing eligibility; other items without a splicing recipe or with rarity 999
 * recycle for nothing.
 */
public class RecycleUtil {
  /**
   * Reported fixed gems per item, keyed by in-game item ID, not Wiki catalogue or seed ID.
   *
   * <p>Names were resolved against the Wiki item cache. Only explicitly reported values are
   * included. Chemical variants and the five specified Super Logos are expanded individually to
   * avoid matching unrelated items.
   */
  private static final Map<Integer, Integer> FIXED_GEM_VALUES =
      Map.<Integer, Integer>ofEntries(
          Map.entry(202, 5), // Small Lock
          Map.entry(206, 50), // Huge Lock
          Map.entry(226, 200), // Signal Jammer
          Map.entry(228, 4), // Grow Spray Fertilizer
          Map.entry(364, 50), // Diaper
          Map.entry(366, 100), // Heartbow
          Map.entry(390, 10), // Heartstone Block
          Map.entry(540, 10), // Green Beer
          Map.entry(574, 2_500), // Turtle Hat
          Map.entry(672, 250), // Growtopia Lunchbox
          Map.entry(712, 10), // Habanero Pepper
          Map.entry(714, 10), // Olmec Head
          Map.entry(716, 1), // Adobe Block
          Map.entry(720, 50), // Pinata
          Map.entry(764, 250), // g-Virus
          Map.entry(768, 250), // Sawed-Off Shotgun
          Map.entry(830, 50), // Beach Blast
          Map.entry(836, 10), // Summer Surprise
          Map.entry(838, 10), // Women's Swimsuit
          Map.entry(844, 10), // Swim Fins
          Map.entry(868, 1), // Milk
          Map.entry(874, 1), // Egg
          Map.entry(882, 100), // Butterfly Leash
          Map.entry(898, 750), // Dear John Tractor
          Map.entry(900, 5_000), // Dragon Hand
          Map.entry(910, 100), // Reanimator Remote
          Map.entry(914, 1), // Chemical G
          Map.entry(916, 1), // Chemical R
          Map.entry(918, 1), // Chemical P
          Map.entry(920, 1), // Chemical B
          Map.entry(922, 1), // Mysterious Chemical
          Map.entry(924, 1), // Chemical Y
          Map.entry(1012, 2_500), // Stick Horse
          Map.entry(1018, 2_500), // Tiny Appaloosa
          Map.entry(1150, 2_500), // Zorbnik Leash
          Map.entry(1152, 250), // Zorbnik DNA
          Map.entry(1180, 4_000), // Spiritual Resonator
          Map.entry(1190, 2_500), // Black Cat Leash
          Map.entry(1214, 2_500), // Familiar Leash
          Map.entry(1232, 100), // Sonar Bracelet
          Map.entry(1240, 400), // Heart Monitor
          Map.entry(1250, 100), // Bride Of Reanimator Remote
          Map.entry(1272, 2_500), // Ambulance
          Map.entry(1320, 2_500), // Riding Raptor
          Map.entry(1378, 5_000), // Ice Dragon Hand
          Map.entry(1400, 4_000), // Mini Growtopian
          Map.entry(1402, 1_500), // Thermonuclear Blast
          Map.entry(1436, 500), // Security Camera
          Map.entry(1482, 250), // Wall Clock
          Map.entry(1540, 3_000), // Mid-Pacific Owl
          Map.entry(1542, 10), // Battle Trout
          Map.entry(1648, 5_000), // Unicorn Garland
          Map.entry(1714, 167), // Tambourine
          Map.entry(1962, 1), // Haunted Chemical
          Map.entry(1964, 1), // Devilfruit
          Map.entry(1976, 1), // Teeny Tiny Skull
          Map.entry(1988, 1), // Doppelganger Potion
          Map.entry(2034, 1), // Comet Dust
          Map.entry(2038, 1), // Unearthly Chemical
          Map.entry(2204, 2_500), // Geiger Counter
          Map.entry(2206, 1), // Radioactive Chemical
          Map.entry(2214, 4_000), // Ice Horse
          Map.entry(2370, 400), // Super Logo - ?
          Map.entry(2372, 400), // Super Logo - Z
          Map.entry(2374, 400), // Super Logo - Yerfdog
          Map.entry(2376, 400), // Super Logo - RTSoft
          Map.entry(2378, 400), // Super Logo - D
          Map.entry(2950, 20_000), // Robotic Lock
          Map.entry(3166, 10_000), // Pet Slime
          Map.entry(3168, 10_000), // Strawberry Slime
          Map.entry(3562, 3_000), // Cave Blast
          Map.entry(3624, 10), // Pineapple Hair
          Map.entry(3710, 0), // Spectral Goggles
          Map.entry(4152, 5_000), // Apocalypse Steed
          Map.entry(4354, 5), // Wolf Ticket
          Map.entry(4444, 1), // Synthetic Chemical
          Map.entry(4490, 1), // Mutated Cell
          Map.entry(4612, 50), // Mac n' Cheese Star
          Map.entry(4960, 5_000), // Megalodon
          Map.entry(5540, 100), // Maidmare
          Map.entry(5642, 100), // Cupid Leash
          Map.entry(5644, 100), // Heart Shirt
          Map.entry(5654, 100), // Weather Machine - Valentine's
          Map.entry(6054, 100), // Pinata Lasso
          Map.entry(6310, 250), // Watermelon Slice
          Map.entry(6800, 100), // Pet Rock
          Map.entry(7342, 100), // Thanksgiving Bell
          Map.entry(7588, 1_000), // Treasure Blast
          Map.entry(7864, 250), // Luxurious Wall Clock
          Map.entry(8196, 1), // Building Blocks Machine
          Map.entry(8556, 250), // SurgWorld Blast
          Map.entry(8612, 10), // Floppy Straw Sun Hat
          Map.entry(8976, 100), // Droid Pet
          Map.entry(9134, 2_500), // Riding Bell
          Map.entry(10102, 250), // Golden Cuff Beard
          Map.entry(10404, 3_000)); // Sledding Penguin

  private RecycleUtil() {}

  /**
   * Whether an item's fixed recycle value is known, including an explicitly reported zero.
   *
   * @param itemId the in-game item ID
   * @return whether the item has a fixed value
   */
  public static boolean hasFixedRecycleValue(int itemId) {
    return FIXED_GEM_VALUES.containsKey(itemId);
  }

  /**
   * Calculates the gems received for recycling a stack of items.
   *
   * @param itemDetail the item detail from the Wiki API
   * @param itemCount how many of the item you are recycling
   * @return the per-item gem range and the calculated total gem count
   */
  public static RecycleResult getRecycleValueForItem(
      ItemDetailResponse itemDetail, long itemCount) {

    GrowtopiaObject item = itemDetail.item();
    Integer fixedValue = FIXED_GEM_VALUES.get(item.id());
    if (fixedValue != null) {
      return new RecycleResult(fixedValue, fixedValue, itemCount * fixedValue);
    }
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
   * @param rangeMax upper displayed gem value; equals {@code rangeMin} for fixed values, otherwise
   *     includes the original calculation's extra gem chance
   * @param gemCount total gems you would receive for {@code itemCount} items
   */
  public record RecycleResult(int rangeMin, int rangeMax, long gemCount) {}
}
