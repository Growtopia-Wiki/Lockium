package dev.skullition.lockium.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

public record ItemsResponse(
        @JsonProperty Map<Integer, ItemCatalogue> items
) {
    @JsonCreator
    public static ItemsResponse fromJson(
            @JsonProperty("items") Map<String, JsonNode> rawItems
    ) {
        Map<Integer, ItemCatalogue> items = new LinkedHashMap<>();
        
        rawItems.forEach((key, node) -> {
            int catalogueId = Integer.parseInt(key);
            var item = new ItemCatalogue(
                    catalogueId,
                    node.get("itemId").asInt(),
                    node.get("seedId").asInt(),
                    node.get("itemName").asString(),
                    node.get("seedName") != null ? node.get("seedName").asString() : null
            );
            items.put(catalogueId, item);
        });
        
        return new ItemsResponse(items);
    }
}

