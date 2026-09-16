package dev.skullition.lockium.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests World of the Day name extraction from the proxy's absolute render URL. */
class GrowtopiaDetailTests {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          https://s3.amazonaws.com/world.growtopiagame.com/veilora.png | VEILORA
          https://s3.amazonaws.com/world.growtopiagame.com/THEDRAGON.png | THEDRAGON
          https://s3.amazonaws.com/world.growtopiagame.com/mixedCase.PNG | MIXEDCASE
          https://example.com/renders/world_1.jpeg | WORLD_1
          """)
  void extractsWotdNameFromAbsoluteUrl(String url, String expected) {
    assertEquals(expected, new GrowtopiaDetail(100, url).wotdName());
  }

  @Test
  void extractsWotdNameWhenTheFileHasNoExtension() {
    assertEquals("VEILORA", new GrowtopiaDetail(100, "https://example.com/veilora").wotdName());
  }

  @Test
  void returnsNullWotdNameWhenUnset() {
    assertNull(new GrowtopiaDetail(100, null).wotdName());
  }
}
