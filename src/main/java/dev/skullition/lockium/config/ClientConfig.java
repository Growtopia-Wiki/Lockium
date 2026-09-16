package dev.skullition.lockium.config;

import dev.skullition.lockium.client.GrowtopiaProxyClient;
import dev.skullition.lockium.client.GrowtopiaWikiClient;
import dev.skullition.lockium.client.WikiClient;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.properties.ProxyProperties;
import dev.skullition.lockium.properties.WikiApiProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Spring configuration that builds the declarative HTTP clients used by Lockium.
 *
 * <p>Three external services are consumed:
 *
 * <ul>
 *   <li><b>Growtopia Wiki</b> – public raw wikitext at {@code ${lockium.wiki-raw-url}}
 *   <li><b>Wiki API</b> – authenticated JSON API at {@code ${wiki.api.url}}
 *   <li><b>Growtopia proxy</b> – internal JSON view of growtopiagame.com at {@code
 *       ${lockium.proxy.url}}
 * </ul>
 *
 * <p>All clients are created via {@link HttpServiceProxyFactory} so the interfaces remain pure
 * declarations with {@code @HttpExchange}. No caching or retry is applied here – that belongs in
 * the service layer.
 */
@Configuration
public class ClientConfig {
  private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);

  /** Identifies the bot to the operators of the services we call. */
  private static final String USER_AGENT = "Lockium/1.2 (Growtopia Wiki Discord bot)";

  private final WikiApiProperties apiProperties;
  private final LockiumProperties lockiumProperties;
  private final ProxyProperties proxyProperties;

  /**
   * Creates the configuration with bound properties.
   *
   * @param apiProperties properties for the Wiki API (url and bearer key)
   * @param lockiumProperties properties for public Growtopia endpoints
   * @param proxyProperties properties for the internal Growtopia proxy
   */
  public ClientConfig(
      WikiApiProperties apiProperties,
      LockiumProperties lockiumProperties,
      ProxyProperties proxyProperties) {
    this.apiProperties = apiProperties;
    this.lockiumProperties = lockiumProperties;
    this.proxyProperties = proxyProperties;
  }

  /**
   * Builds the {@link WikiClient} backed by a {@link RestClient}.
   *
   * <p>Configuration details:
   *
   * <ul>
   *   <li>Base URL from {@code wiki.api.url}
   *   <li>Bearer token from {@code wiki.api.key} added to every request
   *   <li>JDK HttpClient via {@link JdkClientHttpRequestFactory} for HTTP/2 support
   * </ul>
   *
   * @param builder the autoconfigured {@link RestClient.Builder} from Spring Boot
   * @return a proxy implementing {@link WikiClient}
   */
  @Bean
  public WikiClient wikiClient(RestClient.Builder builder) {
    logger.debug("Configuring Wiki API client with base URL {}", apiProperties.url());
    RestClient restClient =
        builder
            .baseUrl(apiProperties.url())
            .defaultHeaders(headers -> headers.setBearerAuth(apiProperties.key()))
            .requestFactory(new JdkClientHttpRequestFactory())
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(WikiClient.class);
  }

  /**
   * Builds the {@link GrowtopiaWikiClient} used to fetch public raw MediaWiki pages.
   *
   * <p>The client uses bounded connect and read timeouts because requests happen while a Discord
   * interaction is waiting for its deferred response. A descriptive user agent identifies the bot
   * to the wiki operator.
   *
   * @param builder the autoconfigured {@link RestClient.Builder} from Spring Boot
   * @return a proxy implementing {@link GrowtopiaWikiClient}
   */
  @Bean
  public GrowtopiaWikiClient growtopiaWikiClient(RestClient.Builder builder) {
    logger.debug(
        "Configuring Growtopia Wiki client with base URL {}", lockiumProperties.wikiRawUrl());
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    RestClient restClient =
        builder
            .baseUrl(lockiumProperties.wikiRawUrl())
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .requestFactory(requestFactory)
            .build();

    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(GrowtopiaWikiClient.class);
  }

  /**
   * Builds the {@link GrowtopiaProxyClient} for the internal Growtopia proxy.
   *
   * <p>The proxy serves correct {@code application/json}, so unlike the retired {@code
   * growtopiagame.com/detail} client this needs no custom message converter. Bounded connect and
   * read timeouts matter here because a scheduler polls this endpoint every minute, and a hung
   * proxy would otherwise occupy a scheduler thread indefinitely.
   *
   * @param builder the autoconfigured {@link RestClient.Builder} from Spring Boot
   * @return a proxy implementing {@link GrowtopiaProxyClient}
   */
  @Bean
  public GrowtopiaProxyClient growtopiaProxyClient(RestClient.Builder builder) {
    logger.debug("Configuring Growtopia proxy client with base URL {}", proxyProperties.url());
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    RestClient restClient =
        builder
            .baseUrl(proxyProperties.url())
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .requestFactory(requestFactory)
            .build();

    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(GrowtopiaProxyClient.class);
  }
}
