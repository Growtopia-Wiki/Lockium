package dev.skullition.lockium.scheduler;

import dev.skullition.lockium.service.WikiCacheService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WikiCacheScheduler {

    private final WikiCacheService cacheService;

    public WikiCacheScheduler(WikiCacheService cacheService) {
        this.cacheService = cacheService;
    }

    // Refresh, runs every 30 minutes and at startup
    @Scheduled(fixedRateString = "30m")
    public void refreshCaches() {
        cacheService.refreshCaches();
    }
}