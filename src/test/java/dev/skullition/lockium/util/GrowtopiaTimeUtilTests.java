package dev.skullition.lockium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests English ordinal suffix boundaries used by Growtopia timestamps. */
class GrowtopiaTimeUtilTests {

  @Test
  void formatsTimeFromAnExplicitClock() {
    Clock clock = Clock.fixed(Instant.parse("2026-07-04T17:22:00Z"), ZoneOffset.UTC);

    assertEquals(
        "Growtopia Time (EDT/UTC-4): July 4th, 13:22.", GrowtopiaTimeUtil.nowString(clock));
  }

  @ParameterizedTest
  @CsvSource({
    "1, st", "2, nd", "3, rd", "4, th", "11, th", "12, th", "13, th", "21, st", "22, nd", "23, rd",
    "31, st"
  })
  void returnsEnglishDaySuffix(int day, String suffix) {
    assertEquals(suffix, GrowtopiaTimeUtil.getDaySuffix(day));
  }
}
