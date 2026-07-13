package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Tests stable US-locale number formatting. */
class FormatUtilTests {

  @Test
  void formattingDoesNotDependOnJvmDefaultLocale() {
    Locale original = Locale.getDefault();
    try {
      Locale.setDefault(Locale.GERMANY);

      assertEquals("1,234,567", FormatUtil.formatNumber(1_234_567L));
      assertEquals("1,234,568", FormatUtil.formatNumber(1_234_567.89));
      assertEquals("1,234.50", FormatUtil.formatDecimal(1_234.5, 2));
    } finally {
      Locale.setDefault(original);
    }
  }
}
