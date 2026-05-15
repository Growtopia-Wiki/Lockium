package dev.skullition.lockium.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ItemsResponse(
        @JsonProperty Map<Integer, GrowtopiaItem> items
) {
}

