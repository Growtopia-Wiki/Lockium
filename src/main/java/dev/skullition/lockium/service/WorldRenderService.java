package dev.skullition.lockium.service;

import dev.skullition.lockium.properties.LockiumProperties;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Service for looking up world renders on the official Growtopia S3 bucket.
 *
 * <p>Renders live at {@code ${lockium.render-url}{world}.png} and only exist after a world owner
 * runs {@code /renderworld} in-game. A {@code HEAD} request is used to check existence without
 * downloading the image; the {@code Last-Modified} header tells when the world was last rendered.
 */
@Service
public class WorldRenderService {
  private static final Logger logger = LoggerFactory.getLogger(WorldRenderService.class);

  private final RestClient restClient;
  private final String renderUrl;

  /**
   * Result of a successful render lookup.
   *
   * @param url absolute URL of the render image
   * @param lastModified when the world was last rendered, or {@code null} if the header was
   *     missing or unparseable
   */
  public record WorldRender(String url, @Nullable Instant lastModified) {}

  /**
   * Creates the service.
   *
   * @param builder the autoconfigured {@link RestClient.Builder} from Spring Boot
   * @param lockiumProperties provides the render base URL
   */
  public WorldRenderService(RestClient.Builder builder, LockiumProperties lockiumProperties) {
    this.renderUrl = lockiumProperties.renderUrl();
    this.restClient =
        builder.baseUrl(renderUrl).requestFactory(new JdkClientHttpRequestFactory()).build();
  }

  /**
   * Checks whether a render exists for the given world.
   *
   * @param worldName the world name; case-insensitive, stored lower-case on the bucket
   * @return the render URL and last render time, or empty if the world has no render or the
   *     request failed
   */
  public Optional<WorldRender> fetchWorldRender(String worldName) {
    String fileName = worldName.toLowerCase(Locale.US) + ".png";
    try {
      var response = restClient.head().uri(fileName).retrieve().toBodilessEntity();
      long lastModifiedMillis = response.getHeaders().getLastModified();
      Instant lastModified =
          lastModifiedMillis > 0 ? Instant.ofEpochMilli(lastModifiedMillis) : null;
      return Optional.of(new WorldRender(renderUrl + fileName, lastModified));
    } catch (RestClientException e) {
      logger.debug("No render found for world {}: {}", worldName, e.getMessage());
      return Optional.empty();
    }
  }
}
