package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import dev.skullition.lockium.properties.AuctionProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Tests snapshot publication, expiry, failure recovery, retry timing, and overlap prevention. */
class GrowtopiaAuctionServiceTests {
  private static final Instant START = Instant.parse("2026-10-21T14:45:02Z");
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void acceptsStaleResponseAndExpiresFromFetchedAt() throws Exception {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    MutableClock clock = new MutableClock(START);
    JsonNode json = payloadJson(START, "[" + listingJson() + "]", staleJson());
    when(client.getAuction())
        .thenReturn(
            ResponseEntity.status(HttpStatus.NON_AUTHORITATIVE_INFORMATION)
                .header("x-cache", "STALE")
                .header("x-shape-warnings", "0")
                .body(json));
    GrowtopiaAuctionService service = new GrowtopiaAuctionService(client, properties(), clock);

    service.refresh();

    ProxyPayload<GrowtopiaAuction> accepted = service.getState().payload();
    assertTrue(accepted.isStale());
    assertEquals(START, accepted.fetchedAt());
    assertEquals(1, accepted.data().auctions().size());
    assertNull(service.getState().failedAt());

    clock.advance(Duration.ofMinutes(10));

    assertNull(service.getState().payload());
  }

  @Test
  void failedRefreshKeepsAcceptedPayloadAndNextSuccessClearsFailure() throws Exception {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    MutableClock clock = new MutableClock(START);
    when(client.getAuction())
        .thenReturn(ResponseEntity.ok(payloadJson(START, "[]", "")))
        .thenThrow(new RestClientException("Forbidden"))
        .thenReturn(ResponseEntity.ok(payloadJson(START.plusSeconds(1), "[]", "")));
    GrowtopiaAuctionService service = new GrowtopiaAuctionService(client, properties(), clock);

    service.refresh();
    ProxyPayload<GrowtopiaAuction> first = service.getState().payload();
    clock.advance(Duration.ofSeconds(1));
    service.refresh();

    assertEquals(first, service.getState().payload());
    assertEquals(START.plusSeconds(1), service.getState().failedAt());

    service.refresh();

    assertEquals(START.plusSeconds(1), service.getState().payload().fetchedAt());
    assertNull(service.getState().failedAt());
  }

  @Test
  void retainsValidSnapshotAcrossHttpTimeoutShapeBodyAndUnexpectedStatusFailures()
      throws Exception {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    MutableClock clock = new MutableClock(START);
    JsonNode existing = payloadJson(START, "[" + listingJson() + "]", "");
    when(client.getAuction())
        .thenReturn(ResponseEntity.ok(existing))
        .thenThrow(httpFailure(HttpStatus.BAD_GATEWAY, "{\"error\":\"origin unavailable\"}"))
        .thenThrow(httpFailure(HttpStatus.NOT_FOUND, "{\"error\":\"route missing\"}"))
        .thenThrow(httpFailure(HttpStatus.FORBIDDEN, "<html>Cloudflare WAF</html>"))
        .thenThrow(new ResourceAccessException("Read timed out"))
        .thenReturn(ResponseEntity.ok(payloadJson(START, "[{}]", "")))
        .thenReturn(ResponseEntity.ok().build())
        .thenReturn(
            ResponseEntity.status(HttpStatus.CREATED)
                .body(payloadJson(START.plusSeconds(7), "[]", "")))
        .thenReturn(ResponseEntity.ok(payloadJson(START.plusSeconds(8), "[]", "")));
    GrowtopiaAuctionService service = new GrowtopiaAuctionService(client, properties(), clock);

    service.refresh();
    ProxyPayload<GrowtopiaAuction> accepted = service.getState().payload();
    assertEquals(1, accepted.data().auctions().size());

    for (int failure = 1; failure <= 7; failure++) {
      clock.advance(Duration.ofSeconds(1));
      service.refresh();

      assertEquals(accepted, service.getState().payload());
      assertEquals(clock.instant(), service.getState().failedAt());
    }

    clock.advance(Duration.ofSeconds(1));
    service.refresh();

    assertTrue(service.getState().payload().data().auctions().isEmpty());
    assertEquals(START.plusSeconds(8), service.getState().payload().fetchedAt());
    assertNull(service.getState().failedAt());
  }

  @Test
  void acceptedSnapshotResetsConsecutiveFailureBackoff() throws Exception {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    MutableClock clock = new MutableClock(START);
    when(client.getAuction())
        .thenThrow(new RestClientException("first failure"))
        .thenThrow(new RestClientException("second failure"))
        .thenReturn(ResponseEntity.ok(payloadJson(START.plusSeconds(60), "[]", "")))
        .thenThrow(new RestClientException("failure after success"))
        .thenThrow(new RestClientException("failure after reset retry"));
    GrowtopiaAuctionService service = new GrowtopiaAuctionService(client, properties(), clock);

    service.refreshIfDue();
    clock.advance(Duration.ofMinutes(1));
    service.refreshIfDue();
    service.refresh();
    service.refresh();

    clock.advance(Duration.ofMinutes(1).minusSeconds(1));
    service.refreshIfDue();
    verify(client, times(4)).getAuction();

    clock.advance(Duration.ofSeconds(1));
    service.refreshIfDue();
    verify(client, times(5)).getAuction();
  }

  @Test
  void invalidShapeIsFailureAndValidEmptySnapshotReplacesPriorListings() throws Exception {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    MutableClock clock = new MutableClock(START);
    when(client.getAuction())
        .thenReturn(ResponseEntity.ok(payloadJson(START, "[{}]", "")))
        .thenReturn(ResponseEntity.ok(payloadJson(START.plusSeconds(1), "[]", "")));
    GrowtopiaAuctionService service = new GrowtopiaAuctionService(client, properties(), clock);

    service.refresh();

    assertNull(service.getState().payload());
    assertEquals(START, service.getState().failedAt());

    clock.advance(Duration.ofSeconds(1));
    service.refresh();

    assertTrue(service.getState().payload().data().auctions().isEmpty());
    assertNull(service.getState().failedAt());
  }

  @Test
  void scheduledRefreshUsesExponentialBackoffWhileManualRefreshBypassesIt() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    MutableClock clock = new MutableClock(START);
    when(client.getAuction()).thenThrow(new RestClientException("Forbidden"));
    GrowtopiaAuctionService service = new GrowtopiaAuctionService(client, properties(), clock);

    service.refreshIfDue();
    verify(client, times(1)).getAuction();

    int attempts = 1;
    for (Duration delay :
        new Duration[] {
          Duration.ofMinutes(1),
          Duration.ofMinutes(2),
          Duration.ofMinutes(4),
          Duration.ofMinutes(8),
          Duration.ofMinutes(10)
        }) {
      clock.advance(delay.minusSeconds(1));
      service.refreshIfDue();
      verify(client, times(attempts)).getAuction();

      clock.advance(Duration.ofSeconds(1));
      service.refreshIfDue();
      attempts++;
      verify(client, times(attempts)).getAuction();
    }

    service.refreshIfDue();
    verify(client, times(attempts)).getAuction();

    service.refresh();
    verify(client, times(attempts + 1)).getAuction();
  }

  @Test
  void disabledPollingDoesNotCallProxy() {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    AuctionProperties disabled =
        new AuctionProperties(
            false, Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofMinutes(10));
    GrowtopiaAuctionService service =
        new GrowtopiaAuctionService(client, disabled, new MutableClock(START));

    service.refresh();
    service.refreshIfDue();

    verifyNoInteractions(client);
  }

  @Test
  void concurrentManualAndScheduledRefreshesDoNotOverlap() throws Exception {
    GrowtopiaProxyClient client = mock(GrowtopiaProxyClient.class);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    when(client.getAuction())
        .thenAnswer(
            invocation -> {
              calls.incrementAndGet();
              entered.countDown();
              if (!release.await(5, TimeUnit.SECONDS)) {
                throw new RestClientException("Mock request timed out");
              }
              return ResponseEntity.ok(payloadJson(START, "[]", ""));
            });
    GrowtopiaAuctionService service =
        new GrowtopiaAuctionService(client, properties(), new MutableClock(START));
    Thread refreshThread = new Thread(service::refresh);

    refreshThread.start();
    assertTrue(entered.await(5, TimeUnit.SECONDS));
    service.refreshIfDue();
    release.countDown();
    refreshThread.join(5_000);

    assertFalse(refreshThread.isAlive());
    assertEquals(1, calls.get());
  }

  private static AuctionProperties properties() {
    return new AuctionProperties(
        true, Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofMinutes(10));
  }

  private static JsonNode payloadJson(Instant fetchedAt, String auctions, String stale)
      throws Exception {
    return MAPPER.readTree(
        """
        {"fetched_at":"%s","warnings":[],"data":{"auctions":%s}%s}
        """
            .formatted(fetchedAt, auctions, stale));
  }

  private static String staleJson() {
    return ",\"stale\":{\"reason\":\"origin\",\"message\":\"origin unavailable\","
        + "\"served_at\":\"2026-10-21T14:46:02Z\"}";
  }

  private static String listingJson() {
    return "{\"name\":\"Megaphone\",\"owner\":\"ThePlane\",\"world\":\"PINATALAND\","
        + "\"bid_wl\":1000,\"remaining_seconds\":34}";
  }

  private static RestClientResponseException httpFailure(HttpStatus status, String body) {
    return new RestClientResponseException(
        status.toString(),
        status,
        status.getReasonPhrase(),
        new HttpHeaders(),
        body.getBytes(StandardCharsets.UTF_8),
        StandardCharsets.UTF_8);
  }

  private static final class MutableClock extends Clock {
    private final AtomicReference<Instant> instant;

    private MutableClock(Instant instant) {
      this.instant = new AtomicReference<>(instant);
    }

    private void advance(Duration duration) {
      instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant.get();
    }
  }
}
