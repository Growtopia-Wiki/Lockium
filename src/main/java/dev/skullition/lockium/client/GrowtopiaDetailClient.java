package dev.skullition.lockium.client;

import dev.skullition.lockium.model.GrowtopiaDetail;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for the official Growtopia status feed.
 *
 * <p>Targets the public endpoint at {@code https://growtopiagame.com/detail}. The service returns a
 * small JSON blob with the current online user count and the World of the Day render images. It
 * requires no authentication.
 *
 * <p>This interface is a pure HTTP facade – Spring creates the implementation at runtime via {@code
 * HttpServiceProxyFactory}. The base URL is supplied in {@code application.properties} ({@code
 * lockium.detail-url}); the method itself has no path because the endpoint lives at the root. The
 * endpoint serves JSON with a {@code text/html} Content-Type, so {@code ClientConfig} wires a
 * dedicated converter for this client.
 *
 * <p>Callers should go through {@code GrowtopiaDetailService}, which keeps the last good response
 * as a fallback, rather than invoking this client directly.
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
