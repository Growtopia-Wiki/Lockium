package dev.skullition.lockium.scheduler;

import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the proxy's {@code /leaderboard} route.
 *
 * <p>Runs at {@code ${lockium.proxy.leaderboard-poll-interval}}, matching the proxy's own refresh
 * cadence. {@link GrowtopiaLeaderboardService#refresh()} handles its own failures, so a proxy
 * outage never cancels the schedule.
 */
@Component
public class GrowtopiaLeaderboardScheduler {
  private static final Logger logger = LoggerFactory.getLogger(GrowtopiaLeaderboardScheduler.class);

  private final GrowtopiaLeaderboardService leaderboardService;

  /**
   * Creates the scheduler.
   *
   * @param leaderboardService service that polls and publishes the leaderboards
   */
  public GrowtopiaLeaderboardScheduler(GrowtopiaLeaderboardService leaderboardService) {
    this.leaderboardService = leaderboardService;
  }

  /** Refreshes the leaderboard snapshot. */
  @Scheduled(fixedRateString = "${lockium.proxy.leaderboard-poll-interval}")
  public void poll() {
    logger.debug("Scheduled Growtopia leaderboard poll triggered");
    leaderboardService.refresh();
  }
}
