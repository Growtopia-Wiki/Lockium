package dev.skullition.lockium.scheduler;

import dev.skullition.lockium.properties.ProxyProperties;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.PlayerCountService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the proxy's {@code /detail} route and prunes expired player-count samples.
 *
 * <p>Runs at {@code ${lockium.proxy.detail-poll-interval}}, matching the proxy's own refresh
 * cadence — polling faster only re-reads its cache. Spring runs the first execution shortly after
 * the context refreshes, so a snapshot is usually available within a minute of startup.
 *
 * <p>{@link GrowtopiaDetailService#refresh()} handles its own failures, so a proxy outage never
 * cancels the schedule.
 */
@Component
public class GrowtopiaDetailScheduler {
  private static final Logger logger = LoggerFactory.getLogger(GrowtopiaDetailScheduler.class);

  private final GrowtopiaDetailService detailService;
  private final PlayerCountService playerCountService;
  private final ProxyProperties proxyProperties;

  /**
   * Creates the scheduler.
   *
   * @param detailService service that polls and publishes the detail snapshot
   * @param playerCountService store holding the online-count history
   * @param proxyProperties provides the sample retention window
   */
  public GrowtopiaDetailScheduler(
      GrowtopiaDetailService detailService,
      PlayerCountService playerCountService,
      ProxyProperties proxyProperties) {
    this.detailService = detailService;
    this.playerCountService = playerCountService;
    this.proxyProperties = proxyProperties;
  }

  /** Refreshes the detail snapshot and drops samples that have aged out. */
  @Scheduled(fixedRateString = "${lockium.proxy.detail-poll-interval}")
  public void poll() {
    logger.debug("Scheduled Growtopia detail poll triggered");
    detailService.refresh();
    playerCountService.prune(Instant.now().minus(proxyProperties.sampleRetention()));
  }
}
