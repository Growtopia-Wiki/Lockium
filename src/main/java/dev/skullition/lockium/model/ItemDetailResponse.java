package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level DTO for {@code GET /v1/items/{id}}.
 *
 * <p>The Wiki API always returns two objects in one payload:
 * <ul>
 *   <li>{@code item} – the block as placed in the world</li>
 *   <li>{@code seed} – the plantable seed form</li>
 * </ul>
 * Both share the same structure ({@link GrowtopiaObject}); even items without
 * a functional seed return a placeholder seed object.
 *
 * <p>This record is immutable and maps 1:1 to the JSON response. It is fetched through {@code
 * WikiService} on demand and is not cached, unlike the item index.
 *
 * @param item the placed item data; never {@code null}
 * @param seed the seed data; never {@code null}
 *
 * @see GrowtopiaObject
 * @since 0.1.0
 */
public record ItemDetailResponse(
    @JsonProperty("item") GrowtopiaObject item, @JsonProperty("seed") GrowtopiaObject seed) {}
