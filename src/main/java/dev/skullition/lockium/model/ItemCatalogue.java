package dev.skullition.lockium.model;

import org.jspecify.annotations.Nullable;

/**
 * Lightweight index entry from {@code GET /v1/items}.
 *
 * <p>Represents one entry in the catalogue map where the key is the internal catalogue index and
 * the value contains the minimal data needed to resolve an item and its seed.
 *
 * @param catalogueId the map key used by the Wiki API (not the in-game ID)
 * @param itemId the in-game item ID for the placed block
 * @param seedId the in-game item ID for the seed form
 * @param itemName display name of the item; never {@code null}
 * @param seedName name of a separate seed Wiki page; {@code null} or empty if the item has no
 *     separate seed page (including ordinary items such as Dirt)
 * @since 0.1.0
 */
public record ItemCatalogue(
    int catalogueId, int itemId, int seedId, String itemName, @Nullable String seedName) {}
