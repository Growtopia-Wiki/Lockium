package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.service.client.WikiClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class WikiService {
    private final WikiClient client;

    public WikiService(WikiClient client) {
        this.client = client;
    }

    @Cacheable(value = "items", sync = true)
    public ItemsResponse getItems() {
        return client.getItems();
    }
    
    public void health() {
        client.health();
    }
}
