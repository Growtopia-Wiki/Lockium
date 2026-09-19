package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The {@code data} object of the proxy's {@code GET /detail} route.
 *
 * <p>Fetched via {@code GrowtopiaProxyClient} and published by {@code GrowtopiaDetailService}.
 *
 * <p>Two differences from the retired {@code growtopiagame.com/detail} feed this replaced: the
 * online count arrives as a real JSON number rather than a numeric string, and the World of the Day
 * is an absolute image URL rather than a {@code worlds/NAME.png} path.
 *
 * @param onlineCount number of users currently online; JSON property {@code "online_count"}
 * @param wotdUrl absolute URL of the World of the Day render, for example {@code
 *     https://s3.amazonaws.com/world.growtopiagame.com/veilora.png}; {@code null} when the site
 *     omits the image
 */
public record GrowtopiaDetail(
    @JsonProperty("online_count") int onlineCount, @JsonProperty("wotd") @Nullable String wotdUrl) {

  /**
   * Extracts the upper-cased world name from {@link #wotdUrl()}.
   *
   * <p>Takes the last path segment and drops the file extension, so {@code
   * .../world.growtopiagame.com/veilora.png} becomes {@code VEILORA}.
   *
   * @return the world name, or {@code null} when no World of the Day is set
   */
  @Nullable
  public String wotdName() {
    if (wotdUrl == null) {
      return null;
    }
    String fileName = wotdUrl.substring(wotdUrl.lastIndexOf('/') + 1);
    int extension = fileName.lastIndexOf('.');
    String name = extension < 0 ? fileName : fileName.substring(0, extension);
    return name.toUpperCase(Locale.US);
  }
}
