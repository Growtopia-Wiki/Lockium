package dev.skullition.lockium.client;

import dev.skullition.lockium.model.GrowtopiaDetail;
import dev.skullition.lockium.model.GrowtopiaLeaderboard;
import dev.skullition.lockium.model.ProxyPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import tools.jackson.databind.JsonNode;

/**
 * Declarative client for the internal Growtopia proxy, which serves growtopiagame.com as JSON.
 *
 * <p>Every route is a plain GET needing no authentication. The auction route also requests JSON.
 * The proxy allowlists a single IP, so requests from anywhere else are answered with HTTP 403 —
 * expect that during local development.
 *
 * <p>Responses arrive wrapped in a {@link ProxyPayload}. A stale fallback is served as HTTP 203
 * with {@code stale} populated; normal cache hits can be HTTP 200. Since 203 succeeds, the client
 * does not throw, so callers check {@link ProxyPayload#isStale()} rather than the status code.
 *
 * <p>The proxy refreshes {@code /detail} and {@code /auction} once a minute and {@code
 * /leaderboard} every four minutes; polling faster simply returns the identical cached body.
 */
@HttpExchange
public interface GrowtopiaProxyClient {

  /**
   * Fetches the current player count and World of the Day.
   *
   * @return the detail payload; never {@code null} on a successful response
   */
  @GetExchange("/detail")
  ProxyPayload<GrowtopiaDetail> getDetail();

  /**
   * Fetches the 18 league boards and the computed overall board.
   *
   * @return the leaderboard payload; never {@code null} on a successful response
   */
  @GetExchange("/leaderboard")
  ProxyPayload<GrowtopiaLeaderboard> getLeaderboard();

  /**
   * Fetches the current auction snapshot, preserving response headers for diagnostics.
   *
   * <p>The auction envelope is validated explicitly by {@link AuctionPayloadParser}; returning a
   * JSON tree prevents malformed upstream values from being coerced into Java primitives.
   *
   * @return the raw JSON response and HTTP headers
   */
  @GetExchange(value = "/auction", accept = "application/json")
  ResponseEntity<JsonNode> getAuction();

  /**
   * Checks whether the proxy can reach the origin site.
   *
   * <p>A non-2xx response surfaces as a {@code RestClientResponseException}.
   */
  @GetExchange("/health")
  void health();
}
