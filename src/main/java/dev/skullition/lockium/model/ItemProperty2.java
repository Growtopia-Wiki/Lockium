package dev.skullition.lockium.model;

import dev.skullition.lockium.util.AppEmojis;
import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Secondary property flags for {@code propFlag2} (Growtopia property bitmask).
 *
 * <p>Unlike {@link ItemProperty}, most of these bits are internal mechanics with no player-facing
 * text. Only {@link #GEMLESS} and {@link #TRANSMUTABLE} currently have descriptions; the rest are
 * kept for completeness and future use.
 *
 * <p>Flag groups:
 *
 * <ul>
 *   <li>0x1 – 0x100: robot behavior
 *   <li>0x200 – 0x400: guild items
 *   <li>0x800 – 0x2000: starship parts
 *   <li>0x4000+: misc mechanics
 * </ul>
 *
 * <p>Use {@link #fromInt(int)} to decode and {@link #toDisplay(int)} to get the Discord-ready
 * string (returns {@code null} when nothing is displayable).
 */
public enum ItemProperty2 {
  ROBOT_DEADLY(0x1, null),
  ROBOT_SHOOT_LEFT(0x2, null),
  ROBOT_SHOOT_RIGHT(0x4, null),
  ROBOT_SHOOT_DOWN(0x8, null),
  ROBOT_SHOOT_UP(0x10, null),
  ROBOT_CAN_SHOOT(0x20, null),
  ROBOT_LAVA(0x40, null),
  ROBOT_POINTY(0x80, null),
  ROBOT_SHOOT_DEADLY(0x100, null),
  GUILD_ITEM(0x200, null),
  GUILD_FLAG(0x400, null),
  STARSHIP_HELM(0x800, null),
  STARSHIP_REACTOR(0x1000, null),
  STARSHIP_VIEWSCREEN(0x2000, null),
  SMOD(0x4000, null),
  TILE_DEADLY_IF_ON(0x8000, null),
  LONG_HAND_ITEM64x32(0x10000, null),
  GEMLESS(0x20000, "%s This item never drops gems.".formatted(AppEmojis.GEM)),
  TRANSMUTABLE(0x40000, "%s This item can be transmuted.".formatted(AppEmojis.TRANSMUTABLE)),
  ;

  private final int mask;
  @Nullable private final String description;

  ItemProperty2(int mask, @Nullable String description) {
    this.mask = mask;
    this.description = description;
  }

  /**
   * Decodes a raw integer bitmask into a set of flags.
   *
   * @param flags value from {@code propFlag2.raw}
   * @return an {@link EnumSet} with all matching constants; never {@code null}, may be empty
   */
  public static EnumSet<ItemProperty2> fromInt(int flags) {
    EnumSet<ItemProperty2> set = EnumSet.noneOf(ItemProperty2.class);
    for (var p : values()) {
      if ((flags & p.mask) != 0) {
        set.add(p);
      }
    }
    return set;
  }

  /**
   * Formats displayable secondary flags as a bullet list.
   *
   * <p>Only flags with a non-null description are included. If no flags are set or none have
   * descriptions, the method returns {@code null} – this allows callers to skip the "Extra Flags"
   * field entirely.
   *
   * @param flags raw propFlag2 value
   * @return string starting with "\n• " followed by each description, or {@code null} when nothing
   *     should be shown
   */
  @Nullable
  public static String toDisplay(int flags) {
    if (flags == 0) {
      return null;
    }
    String result =
        fromInt(flags).stream()
            .map(ItemProperty2::description)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n• ", "\n• ", ""));
    if (result.equals("\n• ")) {
      return null;
    }
    return result;
  }

  /**
   * Returns the stored description.
   *
   * @return emoji-prefixed text, or {@code null} for non-displayable flags
   */
  @Nullable
  public String description() {
    return description;
  }
}
