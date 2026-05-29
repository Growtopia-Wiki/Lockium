package dev.skullition.lockium.scheduler;

import dev.skullition.lockium.service.WikiCacheService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WikiCacheScheduler {

    private final WikiCacheService cacheService;

    public WikiCacheScheduler(WikiCacheService cacheService) {
        this.cacheService = cacheService;
    }

    // Warmup
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        refreshCaches();
    }

    // 2. Refresh, runs every 30 minutes (1,800,000 ms)
    @Scheduled(fixedRateString = "1800000")
    public void refreshCaches() {
        cacheService.refreshCaches();
    }
}