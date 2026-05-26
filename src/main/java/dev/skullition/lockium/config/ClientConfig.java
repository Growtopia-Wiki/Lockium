package dev.skullition.lockium.config;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.properties.WikiApiProperties;
import dev.skullition.lockium.client.WikiClient;
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

import java.util.List;

@Configuration
public class ClientConfig {

    private final WikiApiProperties apiProperties;
    private final LockiumProperties lockiumProperties;

    public ClientConfig(WikiApiProperties apiProperties, LockiumProperties lockiumProperties) {
        this.apiProperties = apiProperties;
        this.lockiumProperties = lockiumProperties;
    }

    @Bean
    public WikiClient wikiClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(apiProperties.url())
                .defaultHeaders(headers -> headers.setBearerAuth(apiProperties.key()))
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(WikiClient.class);
    }

    @Bean
    public GrowtopiaDetailClient growtopiaDetailClient(JsonMapper mapper) {
        var converter = new JacksonJsonHttpMessageConverter(mapper);
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));

        RestTemplate template = new RestTemplate(List.of(converter));
        RestClient restClient = RestClient.create(template) // inherits the converters
                .mutate()
                .baseUrl(lockiumProperties.detailUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(GrowtopiaDetailClient.class);
    }
}
