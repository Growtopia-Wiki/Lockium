package dev.skullition.lockium.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Time helpers for the Growtopia game clock.
 *
 * <p>Growtopia servers run on US Eastern time ({@code America/New_York}), so all in-game events
 * (daily challenges, monthly events, block rotations) are anchored to that zone. Use {@link #now()}
 * for calculations and {@link #nowString()} for user-facing output.
 */
public class GrowtopiaTimeUtil {
  private GrowtopiaTimeUtil() {}

  /** The time zone the Growtopia servers run on. */
  public static final ZoneId GROWTOPIA_ZONE = ZoneId.of("America/New_York");

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.US);
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);
  private static final DateTimeFormatter ZONE_ABBREVIATION =
      DateTimeFormatter.ofPattern("zzz", Locale.US);

  /**
   * Returns the current Growtopia time.
   *
   * @return the current time in {@link #GROWTOPIA_ZONE}
   */
  public static ZonedDateTime now() {
    return ZonedDateTime.now(GROWTOPIA_ZONE);
  }

  /**
   * Formats the current Growtopia time for display.
   *
   * <p>Example: {@code Growtopia Time (EDT/UTC-4): July 4th, 13:22.}
   *
   * @return human-readable Growtopia time, including the zone abbreviation and UTC offset
   */
  public static String nowString() {
    ZonedDateTime now = now();
    int offsetHours = now.getOffset().getTotalSeconds() / 3600;
    return "Growtopia Time (%s/UTC%d): %s %d%s, %s."
        .formatted(
            ZONE_ABBREVIATION.format(now),
            offsetHours,
            MONTH.format(now),
            now.getDayOfMonth(),
            getDaySuffix(now.getDayOfMonth()),
            TIME.format(now));
  }

  /**
   * Returns the English ordinal suffix for a day of month.
   *
   * @param day day of month (1-31)
   * @return {@code "st"}, {@code "nd"}, {@code "rd"}, or {@code "th"}
   */
  public static String getDaySuffix(int day) {
    if (day >= 11 && day <= 13) {
      return "th";
    }
    return switch (day % 10) {
      case 1 -> "st";
      case 2 -> "nd";
      case 3 -> "rd";
      default -> "th";
    };
  }
}
