package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Envelope returned by every route of the internal Growtopia proxy.
 *
 * <p>The proxy wraps each route's payload in this identical structure, so one generic record covers
 * {@code /detail} and {@code /leaderboard} alike. Spring derives a {@code
 * ParameterizedTypeReference} from the declared return type of the client method, so the type
 * variable resolves correctly through {@code RestClient}.
 *
 * <p>When the origin site is unreachable or unparseable the proxy serves its last good copy, up to
 * 24 hours old, as HTTP 203 with {@link #stale()} populated. 203 is a success status, so the status
 * code alone is easy to overlook — check {@link #isStale()} instead, which also carries the reason.
 *
 * @param <T> the route-specific payload type
 * @param fetchedAt when the proxy last fetched the origin; JSON property {@code "fetched_at"}
 * @param warnings non-fatal shape complaints, meaning the origin page parsed but looked odd; empty
 *     on a clean parse
 * @param data the route payload
 * @param stale details of a cached response, or {@code null} when the data is live
 */
public record ProxyPayload<T>(
    @JsonProperty("fetched_at") Instant fetchedAt,
    @JsonProperty("warnings") List<String> warnings,
    @JsonProperty("data") T data,
    @JsonProperty("stale") @Nullable Stale stale) {

  /**
   * Whether the proxy served this from its cache rather than a live origin fetch.
   *
   * @return {@code true} when {@link #stale()} is present
   */
  public boolean isStale() {
    return stale != null;
  }

  /**
   * Details of a stale response.
   *
   * @param reason {@code "origin"} when the site errored, which recovers by itself, or {@code
   *     "parse"} when the site's HTML changed and the proxy needs fixing
   * @param message human-readable explanation from the proxy
   * @param servedAt when the cached copy was originally fetched; JSON property {@code "served_at"}
   */
  public record Stale(
      @JsonProperty("reason") String reason,
      @JsonProperty("message") String message,
      @JsonProperty("served_at") Instant servedAt) {}
}
