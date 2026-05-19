package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.util.ItemUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WikiService {
    private final WikiDataService wiki;

    public WikiService(WikiDataService wiki) {
        this.wiki = wiki;
    }

    @Nullable
    public ItemCatalogue findByName(String itemName) {

        var index = wiki.getNameIndex();
        ItemCatalogue exactMatch = index.get(itemName);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Fallback O(N) lookup - Might remove if laggy.
        return index.entrySet().stream()
                .filter(entry -> ItemUtils.norm(entry.getKey()).startsWith(itemName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public ItemsResponse getItems() {
        return wiki.getItems();
    }
    
    public ItemDetailResponse getItemDetail(int id) {
        return wiki.getItemDetail(id);
    }
    
    public ItemDetailResponse getItemDetail(ItemCatalogue itemCatalogue) {
        return wiki.getItemDetail(itemCatalogue.catalogueId());
    }

    public Map<String, ItemCatalogue> getNameIndex() {
        return wiki.getNameIndex();
    }

    public void health() {
        wiki.health();
    }
}
