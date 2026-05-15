package dev.skullition.lockium.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public record ItemsResponse(
        @JsonProperty Map<Integer, GrowtopiaItem> items
) {
}

