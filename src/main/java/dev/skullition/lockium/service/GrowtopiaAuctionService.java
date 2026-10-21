package dev.skullition.lockium.service;

import dev.skullition.lockium.client.AuctionPayloadParser;
import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import dev.skullition.lockium.properties.AuctionProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * Publishes the most recently accepted auction snapshot for commands to browse.
 *
 * <p>A scheduler performs one shared proxy poll for every viewer. A failed refresh is recorded
 * separately and leaves the last valid payload untouched. Snapshots expire according to the proxy's
 * {@code fetched_at} timestamp, including cached and stale responses.
 */
@Service
public class GrowtopiaAuctionService {
  private static final Logger logger = LoggerFactory.getLogger(GrowtopiaAuctionService.class);

  private final GrowtopiaProxyClient client;
  private final AuctionProperties properties;
  private final Clock clock;
  private final AtomicBoolean refreshInProgress = new AtomicBoolean();
  private final AtomicReference<AuctionState> state =
      new AtomicReference<>(new AuctionState(null, null));
  private final AtomicReference<@Nullable Instant> nextAttemptAt = new AtomicReference<>();
  private final AtomicInteger consecutiveFailures = new AtomicInteger();

  /** Creates the auction service using the system UTC clock. */
  @Autowired
  public GrowtopiaAuctionService(GrowtopiaProxyClient client, AuctionProperties properties) {
    this(client, properties, Clock.systemUTC());
  }

  /** Creates the service with an explicit time source for deterministic expiry and retry tests. */
  GrowtopiaAuctionService(GrowtopiaProxyClient client, AuctionProperties properties, Clock clock) {
    this.client = client;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Fetches a new snapshot immediately, bypassing the retry backoff.
   *
   * <p>Disabled polling and an already active refresh are respected. Expected HTTP and validation
   * failures are logged and captured in {@link #getState()} rather than thrown.
   */
  public void refresh() {
    refreshSnapshot(true);
  }

  /** Fetches a new snapshot only when the poll interval or retry delay has elapsed. */
  public void refreshIfDue() {
    refreshSnapshot(false);
  }

  private void refreshSnapshot(boolean manual) {
    if (!properties.enabled()) {
      logger.debug("Auction polling is disabled");
      return;
    }
    if (!refreshInProgress.compareAndSet(false, true)) {
      logger.debug("Skipping auction refresh because another refresh is active");
      return;
    }

    try {
      Instant startedAt = clock.instant();
      Instant nextAttempt = nextAttemptAt.get();
      if (!manual && nextAttempt != null && startedAt.isBefore(nextAttempt)) {
        logger.debug("Skipping auction refresh until {}", nextAttempt);
        return;
      }
      fetchSnapshot();
    } finally {
      refreshInProgress.set(false);
    }
  }

  private void fetchSnapshot() {
    try {
      ResponseEntity<JsonNode> response = client.getAuction();
      int status = response.getStatusCode().value();
      if (status != 200 && status != 203) {
        throw new IllegalArgumentException("unexpected HTTP status " + status);
      }
      @Nullable JsonNode body = response.getBody();
      if (body == null) {
        throw new IllegalArgumentException("/auction response body was empty");
      }

      @Nullable String cacheStatus = response.getHeaders().getFirst("x-cache");
      @Nullable String warningCount = response.getHeaders().getFirst("x-shape-warnings");
      logger.debug(
          "Received /auction response: x-cache={}, x-shape-warnings={}", cacheStatus, warningCount);

      ProxyPayload<GrowtopiaAuction> accepted = AuctionPayloadParser.parse(body);
      state.set(new AuctionState(accepted, null));
      consecutiveFailures.set(0);
      nextAttemptAt.set(clock.instant().plus(properties.pollInterval()));

      if (!accepted.warnings().isEmpty()) {
        logger.warn("Proxy reported {} shape warning(s) on /auction", accepted.warnings().size());
      }
      ProxyPayload.@Nullable Stale stale = accepted.stale();
      if (stale != null) {
        logger.warn(
            "Proxy served stale /auction data: reason={}, fetchedAt={}",
            stale.reason(),
            accepted.fetchedAt());
      }
      logger.debug(
          "Published auction snapshot with {} listing(s)", accepted.data().auctions().size());
    } catch (RestClientResponseException e) {
      failed("HTTP " + e.getStatusCode());
    } catch (RestClientException e) {
      // Response exception messages may include an entire Cloudflare HTML body; log its type only.
      failed(e.getClass().getSimpleName());
    } catch (IllegalArgumentException e) {
      failed(e.getMessage() == null ? "invalid response" : e.getMessage());
    }
  }

  private void failed(String reason) {
    Instant now = clock.instant();
    state.updateAndGet(current -> new AuctionState(current.payload(), now));
    int failures = consecutiveFailures.incrementAndGet();
    Duration delay = retryDelay(failures);
    nextAttemptAt.set(now.plus(delay));
    logger.warn("Failed to refresh /auction ({}); retrying after {}", reason, delay);
  }

  private Duration retryDelay(int failures) {
    Duration maximum = properties.maxBackoff();
    if (failures >= 5) {
      return maximum;
    }
    Duration exponential = Duration.ofMinutes(1L << (failures - 1));
    return exponential.compareTo(maximum) > 0 ? maximum : exponential;
  }

  /**
   * Returns the accepted snapshot and most recent refresh failure without making a request.
   *
   * @return current state; the payload is {@code null} before the first success or once its {@code
   *     fetched_at} age reaches the configured maximum age
   */
  public AuctionState getState() {
    AuctionState currentState = state.get();
    ProxyPayload<GrowtopiaAuction> current = currentState.payload();
    if (current != null) {
      Duration age = Duration.between(current.fetchedAt(), clock.instant());
      if (age.compareTo(properties.maxAge()) >= 0) {
        current = null;
      }
    }
    return new AuctionState(current, currentState.failedAt());
  }

  /**
   * Current public view of auction data and refresh health.
   *
   * @param payload latest accepted payload while it is within the configured maximum age
   * @param failedAt time of the latest failed refresh, cleared by the next accepted response
   */
  public record AuctionState(
      @Nullable ProxyPayload<GrowtopiaAuction> payload, @Nullable Instant failedAt) {}
}
