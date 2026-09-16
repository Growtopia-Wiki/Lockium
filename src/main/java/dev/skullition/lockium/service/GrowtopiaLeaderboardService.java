package dev.skullition.lockium.service;

import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.model.GrowtopiaLeaderboard;
import dev.skullition.lockium.model.League;
import dev.skullition.lockium.model.LeaderboardEntry;
import dev.skullition.lockium.model.ProxyPayload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Publishes the most recent Growtopia leaderboards.
 *
 * <p>Mirrors {@link GrowtopiaDetailService}: a scheduler calls {@link #refresh()} every few
 * minutes, and commands read the published snapshot without performing any I/O.
 */
@Service
public class GrowtopiaLeaderboardService {
  private static final Logger logger = LoggerFactory.getLogger(GrowtopiaLeaderboardService.class);

  /** How long a snapshot stays usable after the last successful poll. */
  private static final Duration SNAPSHOT_LIFETIME = Duration.ofHours(24);

  private final GrowtopiaProxyClient client;
  private final Clock clock;
  private final AtomicReference<@Nullable LeaderboardSnapshot> snapshot = new AtomicReference<>();

  /**
   * Creates the service.
   *
   * @param client declarative client for the Growtopia proxy
   */
  @Autowired
  public GrowtopiaLeaderboardService(GrowtopiaProxyClient client) {
    this(client, Clock.systemUTC());
  }

  /** Creates the service with an explicit time source for deterministic expiry. */
  GrowtopiaLeaderboardService(GrowtopiaProxyClient client, Clock clock) {
    this.client = client;
    this.clock = clock;
  }

  /**
   * Polls the proxy and publishes the boards.
   *
   * <p>Never throws; a failed poll leaves the previous snapshot in place.
   */
  public void refresh() {
    try {
      ProxyPayload<GrowtopiaLeaderboard> payload = client.getLeaderboard();
      snapshot.set(new LeaderboardSnapshot(payload.data(), payload, clock.instant()));

      if (!payload.warnings().isEmpty()) {
        logger.warn(
            "Proxy reported {} shape warning(s) on /leaderboard: {}",
            payload.warnings().size(),
            payload.warnings());
      }
      warnAboutUnexpectedLeagues(payload.data());
      logger.debug(
          "refresh: overallEntries={}, boards={}",
          payload.data().overall().size(),
          payload.data().leagues().size());
    } catch (RestClientException e) {
      logger.warn(
          "Failed to poll Growtopia leaderboard: {}; keeping the last snapshot", e.getMessage());
    }
  }

  /**
   * Returns the most recently polled leaderboards.
   *
   * @return the current snapshot, or {@code null} when nothing has been polled yet or the last
   *     success is older than 24 hours
   */
  @Nullable
  public LeaderboardSnapshot getSnapshot() {
    LeaderboardSnapshot current = snapshot.get();
    if (current == null) {
      logger.debug("getSnapshot: no leaderboard has been polled yet");
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
   * Logs when the proxy's board set no longer matches the leagues Lockium knows about.
   *
   * <p>Either direction means the upstream board set changed and {@link League} needs updating.
   *
   * @param leaderboard the freshly polled leaderboards
   */
  private void warnAboutUnexpectedLeagues(GrowtopiaLeaderboard leaderboard) {
    List<String> missing =
        Arrays.stream(League.values())
            .map(League::getApiKey)
            .filter(key -> !leaderboard.leagues().containsKey(key))
            .toList();
    if (!missing.isEmpty()) {
      logger.warn("Leaderboard response is missing known league(s): {}", missing);
    }

    List<String> unknown =
        leaderboard.leagues().keySet().stream()
            .filter(key -> League.fromApiKey(key) == null)
            .toList();
    if (!unknown.isEmpty()) {
      logger.warn("Leaderboard response contains unknown league(s): {}", unknown);
    }
  }

  /**
   * Leaderboards together with the envelope they arrived in.
   *
   * @param leaderboard the parsed boards
   * @param payload the full envelope, which carries proxy staleness and shape warnings
   * @param storedAt when Lockium received it
   */
  public record LeaderboardSnapshot(
      GrowtopiaLeaderboard leaderboard,
      ProxyPayload<GrowtopiaLeaderboard> payload,
      Instant storedAt) {

    /**
     * Returns the computed board across every league.
     *
     * @return the overall entries, each annotated with the league it came from
     */
    public List<LeaderboardEntry> overall() {
      return leaderboard.overall();
    }

    /**
     * Returns a single league's board.
     *
     * @param league the league to look up
     * @return that league's entries, or {@code null} when the proxy did not send the board
     */
    @Nullable
    public List<LeaderboardEntry> board(League league) {
      return leaderboard.leagues().get(league.getApiKey());
    }
  }
}
