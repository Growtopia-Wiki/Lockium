package dev.skullition.lockium.util;

import java.util.Locale;

/**
 * US-locale number formatting helpers for user-facing output.
 *
 * <p>All numbers shown to Discord users are formatted with {@link Locale#US} so the output is
 * stable regardless of the JVM's default locale (e.g. {@code 1,234,567} and {@code 3.5} rather
 * than {@code 1.234.567} and {@code 3,5}). Import these methods statically instead of calling
 * {@code String.format(Locale.US, ...)} inline.
 *
 * <p>When the codebase migrates to Kotlin, these are intended to become extension functions (e.g.
 * {@code Long.toFormattedString()}).
 */
public class FormatUtil {
  private FormatUtil() {}

  /**
   * Formats a whole number with grouping separators.
   *
   * <p>Example: {@code 1234567 → "1,234,567"}.
   *
   * @param value the number to format
   * @return the formatted number
   */
  public static String formatNumber(long value) {
    return String.format(Locale.US, "%,d", value);
  }

  /**
   * Formats a floating-point number with grouping separators and no decimal places.
   *
   * <p>Example: {@code 1234567.89 → "1,234,568"}.
   *
   * @param value the number to format
   * @return the formatted number, rounded to a whole number
   */
  public static String formatNumber(double value) {
    return String.format(Locale.US, "%,.0f", value);
  }

  /**
   * Formats a floating-point number with grouping separators and a fixed number of decimals.
   *
   * <p>Example: {@code formatDecimal(3.14159, 2) → "3.14"}.
   *
   * @param value the number to format
   * @param decimals how many decimal places to keep
   * @return the formatted number
   */
  public static String formatDecimal(double value, int decimals) {
    return String.format(Locale.US, "%,." + decimals + "f", value);
  }
}
