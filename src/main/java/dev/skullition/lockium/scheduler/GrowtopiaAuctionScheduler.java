package dev.skullition.lockium.scheduler;

import dev.skullition.lockium.service.GrowtopiaAuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the proxy's {@code /auction} route for one shared snapshot. */
@Component
public class GrowtopiaAuctionScheduler {
  private static final Logger logger = LoggerFactory.getLogger(GrowtopiaAuctionScheduler.class);

  private final GrowtopiaAuctionService auctionService;

  /** Creates the scheduled auction poller. */
  public GrowtopiaAuctionScheduler(GrowtopiaAuctionService auctionService) {
    this.auctionService = auctionService;
  }

  /**
   * Polls at the configured interval and attempts the first refresh as soon as scheduling starts.
   */
  @Scheduled(fixedDelayString = "${lockium.proxy.auction.poll-interval}", initialDelayString = "0")
  public void poll() {
    logger.debug("Scheduled Growtopia auction poll triggered");
    auctionService.refreshIfDue();
  }
}
