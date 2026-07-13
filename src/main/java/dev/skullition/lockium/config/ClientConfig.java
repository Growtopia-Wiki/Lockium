package dev.skullition.lockium.config;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.client.GrowtopiaWikiClient;
import dev.skullition.lockium.client.WikiClient;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.properties.WikiApiProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring configuration that builds the declarative HTTP clients used by Lockium.
 *
 * <p>Three external services are consumed:
 *
 * <ul>
 *   <li><b>Growtopia Wiki</b> – public raw wikitext at {@code ${lockium.wiki-raw-url}}
 *   <li><b>Wiki API</b> – authenticated JSON API at {@code ${wiki.api.url}}
 *   <li><b>Growtopia Detail</b> – public endpoint at {@code ${lockium.detail-url}} which returns
 *       JSON but serves it as {@code text/html}
 * </ul>
 *
 * <p>Both clients are created via {@link HttpServiceProxyFactory} so the interfaces remain pure
 * declarations with {@code @HttpExchange}. No caching or retry is applied here – that belongs in
 * the service layer.
 */
@Configuration
public class ClientConfig {
  private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);

  private final WikiApiProperties apiProperties;
  private final LockiumProperties lockiumProperties;

  /**
   * Creates the configuration with bound properties.
   *
   * @param apiProperties properties for the Wiki API (url and bearer key)
   * @param lockiumProperties properties for public Growtopia endpoints
   */
  public ClientConfig(WikiApiProperties apiProperties, LockiumProperties lockiumProperties) {
    this.apiProperties = apiProperties;
    this.lockiumProperties = lockiumProperties;
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
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    RestClient restClient =
        builder
            .baseUrl(lockiumProperties.wikiRawUrl())
            .defaultHeader(HttpHeaders.USER_AGENT, "Lockium/1.2 (Growtopia Wiki Discord bot)")
            .requestFactory(requestFactory)
            .build();

    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(GrowtopiaWikiClient.class);
  }

  /**
   * Builds the {@link GrowtopiaDetailClient} for {@code growtopiagame.com/detail}.
   *
   * <p>The official endpoint returns JSON with a {@code Content-Type: text/html} header, which
   * breaks the default Jackson converter. This bean configures a {@link
   * JacksonJsonHttpMessageConverter} to accept both {@code application/json} and {@code text/html}.
   *
   * <p>Uses a dedicated {@link RestTemplate} to isolate the custom converter from the Wiki client.
   *
   * @param mapper the shared {@link JsonMapper} configured for the application
   * @return a proxy implementing {@link GrowtopiaDetailClient}
   */
  @Bean
  public GrowtopiaDetailClient growtopiaDetailClient(JsonMapper mapper) {
    logger.debug(
        "Configuring Growtopia detail client with base URL {}", lockiumProperties.detailUrl());
    var converter = new JacksonJsonHttpMessageConverter(mapper);
    converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));

    RestTemplate template = new RestTemplate(List.of(converter));
    RestClient restClient =
        RestClient.create(template) // inherits the converters
            .mutate()
            .baseUrl(lockiumProperties.detailUrl())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
            .build();

    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(GrowtopiaDetailClient.class);
  }
}
