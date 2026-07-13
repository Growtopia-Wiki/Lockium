package dev.skullition.lockium.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests compact RGBA color conversion used by Components V2 containers. */
class GrowtopiaObjectTests {

  @Test
  void missingColorIsTransparent() {
    assertEquals(0, new GrowtopiaObject.ColorInfo(0, null).intOrTransparent());
  }

  @ParameterizedTest
  @CsvSource({
    "#123, FF112233",
    "#1234, 44112233",
    "#112233, FF112233",
    "#11223344, 44112233"
  })
  void convertsRgbaHexToArgb(String hex, String expectedHex) {
    int expected = (int) Long.parseLong(expectedHex, 16);

    assertEquals(expected, new GrowtopiaObject.ColorInfo(0, hex).intOrTransparent());
  }
}
