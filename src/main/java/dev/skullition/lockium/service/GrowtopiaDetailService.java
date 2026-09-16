package dev.skullition.lockium.service;

import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.model.GrowtopiaDetail;
import dev.skullition.lockium.model.ProxyPayload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Publishes the most recent Growtopia server detail and records its player count.
 *
 * <p>A scheduler calls {@link #refresh()} once a minute; commands call {@link #getSnapshot()} and
 * perform no I/O of their own. This is a deliberate change from the previous design, where every
 * interaction triggered a live HTTP request.
 *
 * <p>A failed poll keeps the previous snapshot and records nothing. Writing a zero on failure would
 * put a fake trough in the player-count graph, which is worse than a brief gap.
 *
 * <p>The snapshot is held in a single {@link AtomicReference} and expires after 24 hours, so a
 * long-dead proxy eventually stops the bot reporting stale figures as if they were current.
 */
@Service
public class GrowtopiaDetailService {
  private static final Logger logger = LoggerFactory.getLogger(GrowtopiaDetailService.class);

  /** How long a snapshot stays usable after the last successful poll. */
  private static final Duration SNAPSHOT_LIFETIME = Duration.ofHours(24);

  private final GrowtopiaProxyClient client;
  private final PlayerCountService playerCountService;
  private final Clock clock;
  private final AtomicReference<@Nullable DetailSnapshot> snapshot = new AtomicReference<>();

  /**
   * Creates the service.
   *
   * @param client declarative client for the Growtopia proxy
   * @param playerCountService store that keeps the online-count history
   */
  @Autowired
  public GrowtopiaDetailService(
      GrowtopiaProxyClient client, PlayerCountService playerCountService) {
    this(client, playerCountService, Clock.systemUTC());
  }

  /** Creates the service with an explicit time source for deterministic expiry. */
  GrowtopiaDetailService(
      GrowtopiaProxyClient client, PlayerCountService playerCountService, Clock clock) {
    this.client = client;
    this.playerCountService = playerCountService;
    this.clock = clock;
  }

  /**
   * Polls the proxy, publishes the result, and records the online count.
   *
   * <p>Never throws: a {@link RestClientException} is logged at WARN and the previous snapshot is
   * left in place. A 403 is the expected outcome when running outside the proxy's allowlisted IP.
   */
  public void refresh() {
    try {
      ProxyPayload<GrowtopiaDetail> payload = client.getDetail();
      Instant now = clock.instant();
      snapshot.set(new DetailSnapshot(payload.data(), payload, now));

      if (!payload.warnings().isEmpty()) {
        logger.warn(
            "Proxy reported {} shape warning(s) on /detail: {}",
            payload.warnings().size(),
            payload.warnings());
      }

      playerCountService.record(now, payload.data().onlineCount());
      logger.debug(
          "refresh: onlineCount={}, stale={}", payload.data().onlineCount(), payload.isStale());
    } catch (RestClientException e) {
      logger.warn("Failed to poll Growtopia detail: {}; keeping the last snapshot", e.getMessage());
    }
  }

  /**
   * Returns the most recently polled detail.
   *
   * <p>Performs no I/O — the value comes from the last successful {@link #refresh()}.
   *
   * @return the current snapshot, or {@code null} when nothing has been polled yet or the last
   *     success is older than 24 hours
   */
  @Nullable
  public DetailSnapshot getSnapshot() {
    DetailSnapshot current = snapshot.get();
    if (current == null) {
      logger.debug("getSnapshot: no detail has been polled yet");
      return null;
    }
    Duration age = Duration.between(current.storedAt(), clock.instant());
    if (age.compareTo(SNAPSHOT_LIFETIME) >= 0) {
      logger.debug("getSnapshot: discarding snapshot aged {}m", age.toMinutes());
      return null;
    }
    return current;
  }

  /**
   * A detail payload together with the envelope it arrived in.
   *
   * @param detail the parsed detail data
   * @param payload the full envelope, which carries proxy staleness and shape warnings
   * @param storedAt when Lockium received it
   */
  public record DetailSnapshot(
      GrowtopiaDetail detail, ProxyPayload<GrowtopiaDetail> payload, Instant storedAt) {}
}
