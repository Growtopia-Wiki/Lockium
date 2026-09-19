package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.model.GrowtopiaLeaderboard;
import dev.skullition.lockium.model.LeaderboardEntry;
import dev.skullition.lockium.model.League;
import dev.skullition.lockium.model.ProxyPayload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/** Tests leaderboard snapshot publication and per-league lookups. */
class GrowtopiaLeaderboardServiceTests {

  private static final Instant START = Instant.parse("2026-09-16T12:00:00Z");

  @Test
  void refreshPublishesTheBoards() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    when(client.getLeaderboard()).thenReturn(payload());
    GrowtopiaLeaderboardService service = new GrowtopiaLeaderboardService(client, fixedClock());

    service.refresh();

    var snapshot = service.getSnapshot();
    assertEquals(2, snapshot.overall().size());
    assertEquals("Castor", snapshot.overall().getFirst().name());
    assertEquals(1, snapshot.board(League.SAPPHIRE_3).size());
  }

  @Test
  void returnsNullForBoardTheProxyDidNotSend() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    when(client.getLeaderboard()).thenReturn(payload());
    GrowtopiaLeaderboardService service = new GrowtopiaLeaderboardService(client, fixedClock());

    service.refresh();

    assertNull(service.getSnapshot().board(League.GOLD_2));
  }

  @Test
  void failedRefreshKeepsThePreviousSnapshot() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    when(client.getLeaderboard())
        .thenReturn(payload())
        .thenThrow(new RestClientException("Forbidden"));
    GrowtopiaLeaderboardService service = new GrowtopiaLeaderboardService(client, fixedClock());

    service.refresh();
    service.refresh();

    assertEquals(2, service.getSnapshot().overall().size());
  }

  @Test
  void getSnapshotPerformsNoRequest() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    GrowtopiaLeaderboardService service = new GrowtopiaLeaderboardService(client, fixedClock());

    assertNull(service.getSnapshot());

    verifyNoInteractions(client);
  }

  @Test
  void snapshotExpiresAtTwentyFourHours() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    when(client.getLeaderboard()).thenReturn(payload());
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(START, START.plus(Duration.ofHours(24)));
    GrowtopiaLeaderboardService service = new GrowtopiaLeaderboardService(client, clock);

    service.refresh();

    assertNull(service.getSnapshot());
  }

  @Test
  void overallEntriesCarryTheLeagueTheyCameFrom() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    when(client.getLeaderboard()).thenReturn(payload());
    GrowtopiaLeaderboardService service = new GrowtopiaLeaderboardService(client, fixedClock());

    service.refresh();

    LeaderboardEntry top = service.getSnapshot().overall().getFirst();
    assertEquals(League.SAPPHIRE_3, League.fromApiKey(top.league()));
    // Entries read out of a per-league board carry no league of their own.
    assertNull(service.getSnapshot().board(League.BRONZE_1).getFirst().league());
    assertTrue(service.getSnapshot().payload().warnings().isEmpty());
  }

  private static Clock fixedClock() {
    return Clock.fixed(START, ZoneOffset.UTC);
  }

  private static ProxyPayload<GrowtopiaLeaderboard> payload() {
    var leaderboard =
        new GrowtopiaLeaderboard(
            List.of(
                new LeaderboardEntry(1, "Castor", 257_500, "sapphire_3"),
                new LeaderboardEntry(2, "Onuzu", 198_000, "sapphire_3")),
            Map.of(
                "bronze_1", List.of(new LeaderboardEntry(1, "pituharaLdd", 32_000, null)),
                "sapphire_3", List.of(new LeaderboardEntry(1, "Castor", 257_500, null))));
    return new ProxyPayload<>(START, List.of(), leaderboard, null);
  }
}
