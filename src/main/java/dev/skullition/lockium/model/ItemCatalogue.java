package dev.skullition.lockium.model;

import org.jspecify.annotations.Nullable;

public record ItemCatalogue(
        int catalogueId,
        int itemId,
        int seedId,
        String itemName,
        @Nullable String seedName) {
}
