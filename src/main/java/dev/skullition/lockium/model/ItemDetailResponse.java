package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemDetailResponse(
        @JsonProperty("item") GrowtopiaObject item,
        @JsonProperty("seed") GrowtopiaObject seed
) {
}
