package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests English ordinal suffix boundaries used by Growtopia timestamps. */
class GrowtopiaTimeUtilTests {

  @ParameterizedTest
  @CsvSource({
    "1, st", "2, nd", "3, rd", "4, th", "11, th", "12, th", "13, th", "21, st", "22, nd",
    "23, rd", "31, st"
  })
  void returnsEnglishDaySuffix(int day, String suffix) {
    assertEquals(suffix, GrowtopiaTimeUtil.getDaySuffix(day));
  }
}
