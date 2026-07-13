package dev.skullition.lockium.scheduler;

import dev.skullition.lockium.service.WikiCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring-managed scheduler that keeps the Wiki API cache warm.
 *
 * <p>Delegates to {@link WikiCacheService#refreshCaches()} on a fixed-rate schedule. This prevents
 * the first user after a cold start from paying the ~300KB download cost for {@code /v1/items}.
 *
 * <p>Schedule: every 30 minutes, with an initial execution at application startup (Spring invokes
 * {@code @Scheduled} methods after context refresh).
 *
 * @see WikiCacheService
 */
@Component
public class WikiCacheScheduler {
  private static final Logger logger = LoggerFactory.getLogger(WikiCacheScheduler.class);

  private final WikiCacheService cacheService;

  /**
   * Creates the scheduler.
   *
   * @param cacheService service that performs the actual refresh; never {@code null}
   */
  public WikiCacheScheduler(WikiCacheService cacheService) {
    this.cacheService = cacheService;
  }

  /**
   * Triggers a cache refresh.
   *
   * <p>Runs every 30 minutes as defined by {@code fixedRateString = "30m"}, blocking the scheduler
   * thread for the duration of the API call. Exceptions propagate to Spring's scheduler, which logs
   * them without cancelling future runs.
   */
  @Scheduled(fixedRateString = "30m")
  public void refreshCaches() {
    logger.debug("Scheduled Wiki cache refresh triggered");
    cacheService.refreshCaches();
  }
}
