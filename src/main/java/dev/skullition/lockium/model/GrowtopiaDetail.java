package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Record to store response from Growtopia API.
 *
 * @param onlineUsers amount of users currently online
 * @param wotd current WOTD render
 */
public record GrowtopiaDetail(
        @JsonProperty("online_user") String onlineUsers,
        @JsonProperty("world_day_images") WotdImages wotd) {
    /**
     * Record to store WOTD world names.
     *
     * @param fullSize render image in full size
     * @param resize resized render image
     */
    public record WotdImages(
            @JsonProperty("full_size") String fullSize,
            @JsonProperty("resize") String resize) {}
}