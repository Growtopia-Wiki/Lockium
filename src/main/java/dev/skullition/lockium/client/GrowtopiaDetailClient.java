package dev.skullition.lockium.client;

import dev.skullition.lockium.model.GrowtopiaDetail;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for the official Growtopia status feed.
 *
 * <p>Targets the public endpoint at {@code https://growtopiagame.com/detail}. The service returns a
 * small JSON blob with the current server version, maintenance flags, and online player counts. It
 * requires no authentication.
 *
 * <p>This interface is a pure HTTP facade – Spring creates the implementation at runtime via {@code
 * HttpServiceProxyFactory}. The base URL is supplied in {@code application.yml} ({@code
 * growtopia.detail.base-url}); the method itself has no path because the endpoint lives at the
 * root.
 *
 * <p>Because the feed changes at most once per minute, callers should cache the result (see {@code
 * GrowtopiaDetailService}) rather than invoking this client directly on every command.
 *
 * @see GrowtopiaDetail
 */
@HttpExchange
public interface GrowtopiaDetailClient {

  /**
   * Retrieves the current Growtopia server details.
   *
   * <p>Performs {@code GET /} and deserializes the response into {@link GrowtopiaDetail}. The call
   * is synchronous and will block the calling thread until the HTTP round-trip completes.
   *
   * @return the latest detail payload; never {@code null} on a successful 2xx response
   */
  @GetExchange
  GrowtopiaDetail getGrowtopiaDetail();
}
