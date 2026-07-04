package dev.skullition.lockium.model;

import dev.skullition.lockium.util.AppEmojis;
import java.util.Locale;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import org.jspecify.annotations.Nullable;

/**
 * The chi (element) of a Growtopia item, as used by pet battles and surgery.
 *
 * <p>Loaded from {@code data/ChiList.txt} by {@link
 * dev.skullition.lockium.service.ChiService}. The dataset also contains {@link #AIR} (an alias of
 * wind, rendered with the same emoji) and {@link #NONE} for items explicitly known to have no chi.
 */
public enum Chi {
  EARTH,
  WIND,
  FIRE,
  WATER,
  AIR,
  NONE;

  /**
   * Returns the emoji representing this chi.
   *
   * <p>Resolved lazily (like {@link RoleType}) because {@link AppEmojis} constants are only
   * available once BotCommands has loaded the application emojis.
   *
   * @return the application emoji, or {@code null} for {@link #NONE}
   */
  @Nullable
  public ApplicationEmoji getEmoji() {
    return switch (this) {
      case EARTH -> AppEmojis.EARTH;
      case WIND, AIR -> AppEmojis.WIND;
      case FIRE -> AppEmojis.FIRE;
      case WATER -> AppEmojis.WATER;
      case NONE -> null;
    };
  }

  /**
   * Parses a chi name from the dataset.
   *
   * @param value the raw value, e.g. {@code "EARTH"}; case-insensitive
   * @return the matching constant, or {@link #NONE} if the value is unknown
   */
  public static Chi fromString(String value) {
    try {
      return valueOf(value.trim().toUpperCase(Locale.US));
    } catch (IllegalArgumentException e) {
      return NONE;
    }
  }
}
