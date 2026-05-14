package dev.skullition.lockium;

import dev.skullition.lockium.config.SecretsConfig;
import dev.skullition.lockium.service.client.WikiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class LockiumConfiguration {
    private final SecretsConfig secretsConfig;

    public LockiumConfiguration(SecretsConfig secretsConfig) {
        this.secretsConfig = secretsConfig;
    }
    
    @Bean
    public WikiClient wikiClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl(secretsConfig.apiUrl())
                .defaultHeaders(headers -> headers.setBearerAuth(secretsConfig.token()))
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(WikiClient.class);
    }
}
