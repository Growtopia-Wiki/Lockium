package dev.skullition.lockium.config;

import dev.skullition.lockium.properties.WikiApiProperties;
import dev.skullition.lockium.client.WikiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    private final WikiApiProperties properties;

    public ClientConfig(WikiApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WikiClient wikiClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(properties.url())
                .defaultHeaders(headers -> headers.setBearerAuth(properties.key()))
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(WikiClient.class);
    }
}
