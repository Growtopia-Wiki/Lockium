package dev.skullition.lockium.model;

import dev.skullition.lockium.util.AppEmojis;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Primary property flags for {@code propFlag} (Growtopia property bitmask).
 *
 * <p>Each constant represents one bit. The enum stores:
 *
 * <ol>
 *   <li>the bit mask
 *   <li>a pre-formatted, emoji-prefixed description for Discord replies
 * </ol>
 *
 * <p>Three flags have no standalone description because their text depends on context or doesn't
 * show in game:
 *
 * <ul>
 *   <li>{@link #PERMANENT} – combined with {@link #AUTO_PICKUP}
 *   <li>{@link #NO_SHADOW}
 *   <li>{@link #FOREGROUND}
 * </ul>
 *
 * <p>Use {@link #fromInt(int)} to decode, and {@link #toDisplay(int)} for the final user-facing
 * string.
 *
 * @see ItemProperty2 the secondary flags (propFlag2)
 */
public enum ItemProperty {
  MULTI_FACING(
      0x01,
      "%s This item can be placed in two directions, depending on the direction you're facing."
          .formatted(AppEmojis.MULTI_FACING)),
  WRENCHABLE(
      0x02,
      "%s This item has special properties you can adjust with the Wrench."
          .formatted(AppEmojis.WRENCHABLE)),
  NO_SEED(0x04, "%s This item never drops any seeds.".formatted(AppEmojis.NO_SEED)),
  PERMANENT(0x08, null),
  NO_DROP(0x10, "%s This item never drops anything.".formatted(AppEmojis.NO)),
  NO_SELF(0x20, "%s This item can't be used on yourself.".formatted(AppEmojis.NO)),
  NO_SHADOW(0x40, null),
  WORLD_LOCK(
      0x80,
      "%s This item can only be used in World-Locked worlds.".formatted(AppEmojis.WORLD_LOCK)),
  BETA(0x100, "%s This item can only be placed in the world BETA.".formatted(AppEmojis.BETA)),
  AUTO_PICKUP(
      0x200,
      ("%s This item can't be destroyed - "
              + "smashing it will return it to your backpack if you have room!")
          .formatted(AppEmojis.FIST)),
  MOD(0x400, "%s This item can only be picked up by mods.".formatted(AppEmojis.MOD)),
  RANDOM_GROW(
      0x800, "%s A tree of this type can bear surprising fruit!".formatted(AppEmojis.TRACTOR)),
  PUBLIC(
      0x1000,
      "%s This item is PUBLIC: Even if it's locked, anyone can smash it."
          .formatted(AppEmojis.GARBAGE)),
  FOREGROUND(0x2000, null),
  HOLIDAY(
      0x4000,
      "%s This item can only be created during its event release.".formatted(AppEmojis.EASTER)),
  UNTRADABLE(0x8000, "%s This item cannot be dropped or traded.".formatted(AppEmojis.UNTRADEABLE));

  private final int mask;
  @Nullable private final String description;

  ItemProperty(int mask, @Nullable String description) {
    this.mask = mask;
    this.description = description;
  }

  /**
   * Decodes a raw integer bitmask into a set of flags.
   *
   * @param flags value from {@code propFlag.raw}
   * @return an {@link EnumSet} containing all matching constants; never {@code null}, may be empty
   */
  public static EnumSet<ItemProperty> fromInt(int flags) {
    EnumSet<ItemProperty> set = EnumSet.noneOf(ItemProperty.class);
    for (ItemProperty p : values()) {
      if ((flags & p.mask) != 0) {
        set.add(p);
      }
    }
    return set;
  }

  /**
   * Formats a bitmask into a Discord-ready bullet list.
   *
   * <p>Implements the original game logic for indestructible items:
   *
   * <ul>
   *   <li>PERMANENT only → "smashing it will always yield a new one."
   *   <li>PERMANENT + AUTO_PICKUP → "smashing it will return it to your backpack..."
   *   <li>AUTO_PICKUP alone → "Auto-pickup"
   * </ul>
   *
   * @param flags raw propFlag value
   * @return formatted string prefixed with '• '; returns "This item has no properties." when no
   *     displayable flags are set; never {@code null}
   */
  public static String toDisplay(int flags) {
    if (flags == 0) {
      return "This item has no properties.";
    }

    var set = fromInt(flags);
    List<String> lines = new ArrayList<>();

    boolean permanent = set.remove(PERMANENT);
    boolean autoPickup = set.remove(AUTO_PICKUP);

    // Classic SethHam spaghetti
    if (permanent) {
      if (autoPickup) {
        lines.add(AUTO_PICKUP.description());
      } else {
        lines.add(
            ("%s This item can't be destroyed - " + "smashing it will always yield a new one.")
                .formatted(AppEmojis.FIST));
      }
    } else if (autoPickup) {
      // AUTO_PICKUP without PERMANENT – keep its normal meaning
      lines.add("Auto-pickup");
    }

    // everything else is independent
    for (var p : set) {
      if (p.description() != null) {
        lines.add(p.description());
      }
    }

    if (lines.isEmpty()) {
      return "This item has no properties.";
    }
    return "• " + String.join("\n• ", lines);
  }

  /**
   * Returns the stored description for this flag.
   *
   * @return emoji-prefixed text, or {@code null} for flags handled in {@link #toDisplay(int)}
   */
  @Nullable
  public String description() {
    return description;
  }
}
