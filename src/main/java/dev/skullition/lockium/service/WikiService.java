package dev.skullition.lockium.service;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemsResponse;
import dev.skullition.lockium.util.ItemUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@NullMarked
public class WikiService {
    private final WikiDataService wiki;

    public WikiService(WikiDataService wiki) {
        this.wiki = wiki;
    }

    @Nullable
    public ItemCatalogue findByName(String itemName) {
        String searchKey = ItemUtils.norm(itemName);
        
        var index = wiki.getNameIndex();
        ItemCatalogue exactMatch =  index.get(searchKey);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Fallback O(N) lookup - Might remove if laggy.
        return index.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(searchKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
    
    public ItemsResponse getItems() {
        return wiki.getItems();
    }

    public void health() {
        wiki.health();
    }
}
