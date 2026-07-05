package dev.skullition.lockium.service;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.model.GrowtopiaDetail;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Service for retrieving the official Growtopia server detail with an in-memory fallback.
 *
 * <p>Each call to {@link #getDetail()} attempts a live HTTP request via {@link
 * GrowtopiaDetailClient}. On success the response is stored as the "last good" value. On any {@link
 * RestClientException} (bad status, timeout, deserialization failure), the service does not
 * propagate the exception – it logs a warning and returns the cached value if it is less than 24
 * hours old.
 *
 * <p>This cache is intentionally separate from Spring's {@code @Cacheable} infrastructure: it is
 * tiny (one object), thread-safe via a single {@link AtomicReference} holding the value together
 * with its timestamp, and survives only for the lifetime of the JVM.
 */
@Service
public class GrowtopiaDetailService {
  private static final Logger log = LoggerFactory.getLogger(GrowtopiaDetailService.class);

  private final GrowtopiaDetailClient client;
  private final AtomicReference<@Nullable CachedDetail> lastGood = new AtomicReference<>();

  /**
   * Creates the service.
   *
   * @param client declarative client for the detail endpoint
   */
  public GrowtopiaDetailService(GrowtopiaDetailClient client) {
    this.client = client;
  }

  /**
   * Returns the current Growtopia detail, using cache on failure.
   *
   * <p>Workflow:
   *
   * <ol>
   *   <li>Call the upstream API
   *   <li>On success: store the response with its timestamp, return fresh data
   *   <li>On {@link RestClientException}: log at WARN and delegate to {@link #fallback()}
   * </ol>
   *
   * @return fresh {@link GrowtopiaDetail}, or a cached copy if the request fails and the cache is
   *     younger than 24 hours, or {@code null} if no usable data exists
   */
  @Nullable
  public GrowtopiaDetail getDetail() {
    try {
      GrowtopiaDetail fresh = client.getGrowtopiaDetail();
      lastGood.set(new CachedDetail(fresh, System.currentTimeMillis()));
      return fresh;
    } catch (RestClientException e) {
      log.warn("Failed to fetch detail: {} – using cached", e.getMessage());
      return fallback();
    }
  }

  /**
   * Returns the last good response if it is younger than 24 hours.
   *
   * @return the cached detail, or {@code null} if absent or expired
   */
  @Nullable
  private GrowtopiaDetail fallback() {
    CachedDetail cached = lastGood.get();
    if (cached != null
        && System.currentTimeMillis() - cached.at() < Duration.ofHours(24).toMillis()) {
      log.info(
          "Returning cached detail (age {}m) after upstream failure",
          (System.currentTimeMillis() - cached.at()) / 60000);
      return cached.detail();
    }
    return null;
  }

  /**
   * A successfully fetched detail paired with the time it was stored.
   *
   * @param detail the last good response
   * @param at epoch milliseconds when the response was stored
   */
  private record CachedDetail(GrowtopiaDetail detail, long at) {}
}
