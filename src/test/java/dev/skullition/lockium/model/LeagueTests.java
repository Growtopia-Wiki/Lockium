package dev.skullition.lockium.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests the league constants and their mapping to the proxy's keys. */
class LeagueTests {

  /** Discord permits at most 25 predefined choices on a slash-command option. */
  private static final int DISCORD_CHOICE_LIMIT = 25;

  @Test
  void hasTheEighteenBoardsTheProxyPublishes() {
    assertEquals(18, League.values().length);
  }

  @Test
  void apiKeysRoundTrip() {
    for (League league : League.values()) {
      assertSame(league, League.fromApiKey(league.getApiKey()));
    }
  }

  @Test
  void fromApiKeyReturnsNullForUnknownOrMissingKey() {
    assertNull(League.fromApiKey("mythril_9"));
    assertNull(League.fromApiKey(null));
  }

  @Test
  void spansBronzeThroughSapphire() {
    assertEquals("bronze_1", League.BRONZE_1.getApiKey());
    assertEquals("Bronze I", League.BRONZE_1.getDisplayName());
    assertEquals("sapphire_3", League.SAPPHIRE_3.getApiKey());
    assertEquals("Sapphire III", League.SAPPHIRE_3.getDisplayName());
  }

  @Test
  void choiceCountFitsDiscordLimit() {
    assertTrue(League.values().length <= DISCORD_CHOICE_LIMIT);
  }

  @Test
  void apiKeysAreDistinct() {
    long distinct = Arrays.stream(League.values()).map(League::getApiKey).distinct().count();
    assertEquals(League.values().length, distinct);
  }
}
