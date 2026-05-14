package dev.skullition.lockium.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("secrets")
@NullMarked
public record SecretsConfig(String token, String apiUrl, String apiKey) {}
