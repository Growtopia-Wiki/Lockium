package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemsResponse;
import org.springframework.stereotype.Service;

@Service
public class WikiCacheService {
    private final WikiDataService dataService;

    public WikiCacheService(WikiDataService dataService) {
        this.dataService = dataService;
    }

    public void refreshCaches() {
        // 1 API call gets the new data and updates the "items" cache
        ItemsResponse freshItems = dataService.refreshItems();

        // We pass that fresh data to update the "itemIndex" cache instantly
        dataService.refreshNameIndex(freshItems);
    }
}
