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
 * GrowtopiaDetailClient}. On success the response is stored as the "last good" value. On failure
 * (most commonly an HTTP 400/502 from growtopiagame.com), the service does not propagate the
 * exception – it logs a warning and returns the cached value if it is less than 24 hours old.
 *
 * <p>This cache is intentionally separate from Spring's {@code @Cacheable} infrastructure: it is
 * tiny (one object), thread-safe via {@link AtomicReference} and a {@code volatile} timestamp, and
 * survives only for the lifetime of the JVM.
 */
@Service
public class GrowtopiaDetailService {
  private static final Logger log = LoggerFactory.getLogger(GrowtopiaDetailService.class);

  private final GrowtopiaDetailClient client;
  private final AtomicReference<@Nullable GrowtopiaDetail> lastGood = new AtomicReference<>();
  private volatile long lastGoodAt = 0;

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
   *   <li>On success: update {@link #lastGood} and {@link #lastGoodAt}, return fresh data
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
      lastGood.set(fresh);
      lastGoodAt = System.currentTimeMillis();
      return fresh;
    } catch (RestClientException e) {
      // 400 – don't retry, log and use cache
      log.warn("detail returned 400: {} – using cached", e.getMessage());
      return fallback();
    }
  }

  @Nullable
  private GrowtopiaDetail fallback() {
    GrowtopiaDetail cached = lastGood.get();
    if (cached != null
        && System.currentTimeMillis() - lastGoodAt < Duration.ofHours(24).toMillis()) {
      log.info(
          "Returning cached detail (age {}m) due to {}",
          (System.currentTimeMillis() - lastGoodAt) / 60000,
          "Bad request from upstream");
      return cached;
    }
    return null;
  }
}
