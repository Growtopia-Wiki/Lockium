package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.service.client.WikiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WikiDataService {
    private static final Logger logger = LoggerFactory.getLogger(WikiDataService.class);
    private final WikiClient client;

    public WikiDataService(WikiClient client) {
        this.client = client;
    }

    // Methods to actually call

    @Cacheable(value = "items")
    public ItemsResponse getItems() {
        logger.info("Items cache is empty, fetching...");
        return client.getItems();
    }

    @Cacheable(value = "itemIndex", key = "'byName'", sync = true)
    public Map<String, ItemCatalogue> getNameIndex() {
        return buildIndex(getItems());
    }
    
    public ItemDetailResponse getItemDetail(int id) {
        return client.getItemDetail(id);
    }

    public void health() {
        client.health();
    }

    // Background Writes

    @CachePut(value = "items")
    public ItemsResponse refreshItems() {
        return client.getItems();
    }

    @CachePut(value = "itemIndex", key = "'byName'")
    public Map<String, ItemCatalogue> refreshNameIndex(ItemsResponse items) {
        return buildIndex(items);
    }

    private Map<String, ItemCatalogue> buildIndex(ItemsResponse itemsResponse) {
        return itemsResponse.items().values().stream()
                .flatMap(item -> {
                    Stream<Map.Entry<String, ItemCatalogue>> base =
                            Stream.of(Map.entry(item.itemName(), item));
                    
                    if (item.seedName() != null) {
                        return Stream.concat(base,
                                Stream.of(Map.entry(item.seedName(), item)));
                    }
                    return base;
                })
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, _) -> first
                ));
    }

}
