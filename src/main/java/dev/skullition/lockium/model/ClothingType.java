package dev.skullition.lockium.model;

import dev.skullition.lockium.util.AppEmojis;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import org.jspecify.annotations.Nullable;

/**
 * Clothing slot types for Growtopia wearables.
 *
 * <p>Used when {@code category.id == 20} (Clothes). Each constant maps the in-game slot index to a
 * Discord {@link ApplicationEmoji} for bot replies and a human-readable name.
 *
 * <p>The enum is intentionally dense – IDs are 0-8 and sequential, so lookup is a simple array
 * access. Unknown IDs return {@code null} rather than throwing, allowing callers to fall back
 * gracefully.
 *
 * @see AppEmojis
 */
public enum ClothingType {
  HAT(0, AppEmojis.TOP_HAT, "Hat"),
  SHIRT(1, AppEmojis.GREEN_SHIRT, "Shirt"),
  PANTS(2, AppEmojis.JEANS, "Pants"),
  FEET(3, AppEmojis.BOOTS, "Feet"),
  FACE(4, AppEmojis.SHADES, "Face"),
  HAND(5, AppEmojis.FIST, "Hand"),
  BACK(6, AppEmojis.FAIRY_WINGS, "Back"),
  HAIR(7, AppEmojis.RED_HAIR, "Hair"),
  CHEST(8, AppEmojis.GOLD_CHAIN, "Chest");

  private static final ClothingType[] BY_ID = values();
  private final int id;
  private final ApplicationEmoji icon;
  private final String itemName;

  ClothingType(int id, ApplicationEmoji icon, String itemName) {
    this.id = id;
    this.icon = icon;
    this.itemName = itemName;
  }

  /**
   * Returns the clothing type for the given in-game slot ID.
   *
   * @param id the slot index from the API (0-8)
   * @return the matching {@code ClothingType}, or {@code null} if the ID is outside the known range
   */
  @Nullable
  public static ClothingType fromId(int id) {
    return (id >= 0 && id < BY_ID.length) ? BY_ID[id] : null;
  }

  /** Returns the emoji shown next to this clothing type. */
  public ApplicationEmoji getIcon() {
    return icon;
  }

  /** Returns the human-readable slot name, e.g. {@code "Hat"}. */
  public String getItemName() {
    return itemName;
  }
}
