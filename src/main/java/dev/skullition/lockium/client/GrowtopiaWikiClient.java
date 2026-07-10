package dev.skullition.lockium.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative client for the public Growtopia wiki (MediaWiki).
 *
 * <p>This is a thin HTTP facade – it performs no caching, no parsing, and no retry logic. All calls
 * are executed by Spring's {@code HttpServiceProxyFactory} on the {@code RestClient} configured in
 * {@code ClientConfig}. Parsing of the returned wikitext lives in {@code ItemEffectService}.
 *
 * <p><b>Base URL:</b> {@code ${lockium.wiki-raw-url}}<br>
 * <b>Auth:</b> none (public wiki)
 */
@HttpExchange
public interface GrowtopiaWikiClient {

  /**
   * Fetches a page's raw MediaWiki wikitext.
   *
   * <p>Calls {@code GET /index.php?action=raw&title={title}}. The response is the unrendered page
   * source (served as {@code text/x-wiki}), which contains templates such as {@code
   * {{Item/Mod|...}}}, {@code {{Added|...}}}, and {@code {{Removed|...}}}.
   *
   * @param title the wiki page title with spaces replaced by underscores, from {@code
   *     ItemUtils#getWikiItemName(String)} (e.g. {@code Bunny_Egg}); the wiki returns 404 for
   *     missing pages, surfacing as {@code RestClientResponseException}
   * @return the raw wikitext; never {@code null}
   */
  @GetExchange("/index.php?action=raw")
  String getRawPage(@RequestParam("title") String title);
}
