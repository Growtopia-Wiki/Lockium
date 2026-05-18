package dev.skullition.lockium.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("growtopia.api")
public record WikiApiProperties(String key, String url) {
}
