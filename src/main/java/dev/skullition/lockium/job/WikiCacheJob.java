package dev.skullition.lockium.job;

import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.service.WikiDataService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WikiCacheJob {

    private final WikiDataService dataService;

    public WikiCacheJob(WikiDataService dataService) {
        this.dataService = dataService;
    }

    // Warmup
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        refreshCaches();
    }

    // 2. Refresh, runs every 30 minutes (1,800,000 ms)
    @Scheduled(fixedRateString = "1800000")
    public void refreshCaches() {
        // 1 API call gets the new data and updates the "items" cache
        ItemsResponse freshItems = dataService.refreshItems();

        // We pass that fresh data to update the "itemIndex" cache instantly
        dataService.refreshNameIndex(freshItems);
    }
}