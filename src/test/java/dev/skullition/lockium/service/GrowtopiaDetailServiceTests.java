package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.model.GrowtopiaDetail;
import dev.skullition.lockium.model.ProxyPayload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/** Tests snapshot publication, sample recording, and bounded snapshot expiry. */
class GrowtopiaDetailServiceTests {

  private static final Instant START = Instant.parse("2026-09-16T12:00:00Z");

  @Test
  void refreshPublishesTheFreshSnapshot() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    PlayerCountService playerCountService = mock(PlayerCountService.class);
    ProxyPayload<GrowtopiaDetail> payload = live(37_308);
    when(client.getDetail()).thenReturn(payload);
    GrowtopiaDetailService service =
        new GrowtopiaDetailService(client, playerCountService, fixedClock());

    service.refresh();

    var snapshot = service.getSnapshot();
    assertSame(payload.data(), snapshot.detail());
    assertEquals(37_308, snapshot.detail().onlineCount());
  }

  @Test
  void refreshRecordsTheOnlineCount() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    PlayerCountService playerCountService = mock(PlayerCountService.class);
    when(client.getDetail()).thenReturn(live(1_234));
    GrowtopiaDetailService service =
        new GrowtopiaDetailService(client, playerCountService, fixedClock());

    service.refresh();

    verify(playerCountService).record(START, 1_234);
  }

  @Test
  void failedRefreshKeepsTheSnapshotAndRecordsNothing() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    PlayerCountService playerCountService = mock(PlayerCountService.class);
    ProxyPayload<GrowtopiaDetail> payload = live(500);
    when(client.getDetail()).thenReturn(payload).thenThrow(new RestClientException("Forbidden"));
    GrowtopiaDetailService service =
        new GrowtopiaDetailService(client, playerCountService, fixedClock());

    service.refresh();
    service.refresh();

    // A failed poll must never write a zero, which would be a fake trough in the graph.
    verify(playerCountService).record(any(), anyInt());
    verify(playerCountService, never()).record(any(), org.mockito.ArgumentMatchers.eq(0));
    assertSame(payload.data(), service.getSnapshot().detail());
  }

  @Test
  void getSnapshotPerformsNoRequest() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    PlayerCountService playerCountService = mock(PlayerCountService.class);
    GrowtopiaDetailService service =
        new GrowtopiaDetailService(client, playerCountService, fixedClock());

    assertNull(service.getSnapshot());

    // Commands read the published snapshot; they never trigger a fetch of their own.
    verifyNoInteractions(client);
  }

  @Test
  void snapshotExpiresAtTwentyFourHours() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    PlayerCountService playerCountService = mock(PlayerCountService.class);
    when(client.getDetail()).thenReturn(live(999));
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(START, START.plus(Duration.ofHours(24)));
    GrowtopiaDetailService service = new GrowtopiaDetailService(client, playerCountService, clock);

    service.refresh();

    assertNull(service.getSnapshot());
  }

  @Test
  void snapshotReportsProxyStaleness() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    PlayerCountService playerCountService = mock(PlayerCountService.class);
    var stale =
        new ProxyPayload.Stale("origin", "origin /detail returned 403", START.minusSeconds(600));
    when(client.getDetail())
        .thenReturn(new ProxyPayload<>(START, List.of(), new GrowtopiaDetail(42, null), stale));
    GrowtopiaDetailService service =
        new GrowtopiaDetailService(client, playerCountService, fixedClock());

    service.refresh();

    assertTrue(service.getSnapshot().payload().isStale());
    assertEquals("origin", service.getSnapshot().payload().stale().reason());
  }

  private static Clock fixedClock() {
    return Clock.fixed(START, ZoneOffset.UTC);
  }

  private static ProxyPayload<GrowtopiaDetail> live(int onlineCount) {
    return new ProxyPayload<>(
        START,
        List.of(),
        new GrowtopiaDetail(
            onlineCount, "https://s3.amazonaws.com/world.growtopiagame.com/veilora.png"),
        null);
  }
}
