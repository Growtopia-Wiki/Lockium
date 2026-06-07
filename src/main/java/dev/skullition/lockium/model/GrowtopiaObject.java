package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.EnumSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Immutable representation of a Growtopia item or seed as returned by {@code /v1/items/{id}}.
 *
 * <p>This record maps 1:1 to the Wiki API JSON. It is used for both the {@code item} and {@code
 * seed} fields in {@link ItemDetailResponse}. All reference types are non-null unless explicitly
 * annotated {@code @Nullable}.
 *
 * @param id unique in-game identifier
 * @param name display name shown in-game
 * @param description in-game description text
 * @param rarity rarity
 * @param categoryInfo primary category; JSON property {@code "category"}
 * @param clothingCategoryInfo clothing sub-category; present only when {@code categoryInfo.id ==
 *     20}, otherwise {@code null}; JSON property {@code "clothingType"}
 * @param collisionType collision behavior
 * @param textureType texture behavior
 * @param propFlag primary bitflags (see {@link ItemProperty})
 * @param propFlag2 secondary bitflags (see {@link ItemProperty2})
 * @param hardness number of hits required to break
 * @param cookTime seconds to cook in an oven, or {@code null} if not cookable
 * @param restoreTime seconds until the block resets hits animation
 * @param growTime seconds for a tree to mature
 * @param baseStyle base sprite variant index
 * @param overStyle overlay sprite variant index
 * @param baseColor base tint color
 * @param overColor overlay tint color
 * @param recipe1 first splicing recipe of an item
 * @param recipe2 second splicing recipe of an item
 * @see ItemCategory
 * @see ClothingType
 */
public record GrowtopiaObject(
    int id,
    String name,
    String description,
    int rarity,
    @JsonProperty("category") CategoryInfo categoryInfo,
    @JsonProperty("clothingType") @Nullable CategoryInfo clothingCategoryInfo,
    TypeInfo collisionType,
    TypeInfo textureType,
    PropFlag propFlag,
    PropFlag propFlag2,
    int hardness,
    @Nullable Integer cookTime,
    int restoreTime,
    int growTime,
    int baseStyle,
    int overStyle,
    ColorInfo baseColor,
    ColorInfo overColor,
    @Nullable RecipeInfo recipe1,
    @Nullable RecipeInfo recipe2) {
  /**
   * Returns a human-readable description of the primary flags.
   *
   * @return formatted text from {@link ItemProperty#toDisplay(int)}; never {@code null} (returns
   *     "No special properties" when empty)
   */
  public String propFlagText() {
    return ItemProperty.toDisplay(propFlag.raw());
  }

  /**
   * Returns whether this item can have trees based on certain properties or whether its in a
   * category.
   */
  public boolean canHaveTrees() {
    EnumSet<ItemProperty> properties = ItemProperty.fromInt(propFlag.raw);
    if (properties.contains(ItemProperty.MOD)
        || properties.contains(ItemProperty.UNTRADABLE)
        || properties.contains(ItemProperty.BETA)
        || properties.contains(ItemProperty.AUTO_PICKUP)
        || properties.contains(ItemProperty.PERMANENT)) {
      return false;
    }
    var category = ItemCategory.fromId(categoryInfo.id());
    return switch (category) {
      case ARTIFACTS,
          FIST,
          WRENCH,
          GEMS,
          FISHES,
          LOCKS,
          BEDROCK,
          SUNGATE,
          BLANK,
          HEART_MONITOR,
          SECURITY_CAMERAS,
          SPOTLIGHT,
          SOLAR_COLLECTOR,
          FORGE,
          GIVING_TREE,
          SILKWORM -> // There's DEFINITELY more.
          false;
      default -> true;
    };
  }

  /**
   * Returns a human-readable description of the secondary flags.
   *
   * @return formatted text from {@link ItemProperty2#toDisplay(int)}, or {@code null} if the
   *     implementation chooses to suppress empty output
   */
  @Nullable
  public String propFlag2Text() {
    return ItemProperty2.toDisplay(propFlag2.raw());
  }

  /**
   * Maps the primary category to the {@link ItemCategory} enum.
   *
   * @return the matching enum constant; {@link ItemCategory#UNKNOWN} if the ID is unrecognized
   *     (e.g., 51 → BUNNY_EGG)
   */
  public ItemCategory getItemCategory() {
    return ItemCategory.fromId(categoryInfo.id()); // e.g. 51 -> BUNNY_EGG
  }

  /**
   * Resolves the clothing type when this object is a piece of clothing.
   *
   * @return the {@link ClothingType} derived from {@code clothingCategoryInfo}, or {@code null} if
   *     this is not a clothing item
   */
  @Nullable
  public ClothingType getClothingType() {
    if (clothingCategoryInfo == null) {
      return null;
    }
    return ClothingType.fromId(clothingCategoryInfo.id());
  }

  /**
   * Category descriptor used for both main categories and clothing types.
   *
   * @param id numeric category ID
   * @param name display name
   * @param type grouping type; {@code null} for orphan categories such as Seed, Clothes,
   *     Consumable, etc.
   */
  public record CategoryInfo(int id, String name, @Nullable String type) {}

  /**
   * Simple ID/name pair for collision and texture types.
   *
   * @param id numeric type ID
   * @param name localized name; may be {@code null} when the server returns an unmapped ID
   */
  public record TypeInfo(int id, @Nullable String name) {}

  /**
   * Raw bitmask container as returned by the API.
   *
   * <p>The {@code names} array is provided by the API for debugging only; prefer {@link
   * ItemProperty} or {@link ItemProperty2} for interpretation.
   *
   * @param raw integer bitmask
   * @param names list of flag names; never {@code null} (could be empty)
   */
  public record PropFlag(int raw, List<String> names) {}

  /**
   * RGBA color representation.
   *
   * @param raw unsigned 32-bit integer (0 – 4294967295)
   * @param hex hex string in {@code #RRGGBBAA} format; {@code null} when {@code raw == 0}
   */
  public record ColorInfo(long raw, @Nullable String hex) {
    /**
     * Converts {@code hex} to a packed ARGB {@code int} for Java2D/AWT.
     *
     * <p>Accepts 3, 4, 6, or 8 digit forms:
     *
     * <ul>
     *   <li>{@code RGB} → {@code RRGGBBFF}
     *   <li>{@code RGBA} → {@code RRGGBBAA}
     *   <li>{@code RRGGBB} → {@code RRGGBBFF}
     *   <li>{@code RRGGBBAA} → reordered to {@code AARRGGBB}
     * </ul>
     *
     * @return ARGB integer; {@code 0} (transparent) if {@code hex} is {@code null}
     */
    public int intOrTransparent() {
      if (hex == null) {
        return 0;
      }
      String h = hex.replace("#", "");
      if (h.length() == 3) {
        h =
            ""
                + h.charAt(0)
                + h.charAt(0)
                + h.charAt(1)
                + h.charAt(1)
                + h.charAt(2)
                + h.charAt(2)
                + "FF";
      }
      if (h.length() == 4) {
        h =
            ""
                + h.charAt(0)
                + h.charAt(0)
                + h.charAt(1)
                + h.charAt(1)
                + h.charAt(2)
                + h.charAt(2)
                + h.charAt(3)
                + h.charAt(3);
      }
      if (h.length() == 6) {
        h = h + "FF"; // assume RRGGBB → add alpha
      }
      // assume input is RRGGBBAA → convert to AARRGGBB
      if (h.length() == 8) {
        h = h.substring(6, 8) + h.substring(0, 6);
      }
      return (int) Long.parseLong(h, 16);
    }
  }

  /**
   * Possibly null representation of item's seed recipe.
   *
   * @param id the item's id associated with the splicing recipe.
   */
  public record RecipeInfo(int id) {}
}
