package dev.skullition.lockium.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ItemCatalogue(
        int itemId,
        int seedId,
        String itemName,
        @Nullable String seedName) {
}
