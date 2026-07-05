package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the official {@code growtopiagame.com/detail} status feed.
 *
 * <p>Maps the fields Lockium consumes from the JSON blob; other fields in the payload are ignored.
 * Fetched via {@code GrowtopiaDetailClient} and served with a fallback by {@code
 * GrowtopiaDetailService}.
 *
 * @param onlineUsers amount of users currently online, as a numeric string; JSON property {@code
 *     "online_user"}
 * @param wotd current World of the Day render images; JSON property {@code "world_day_images"}
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
      @JsonProperty("full_size") String fullSize, @JsonProperty("resize") String resize) {}
}
