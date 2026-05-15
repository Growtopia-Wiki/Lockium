package dev.skullition.lockium.properties;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NullMarked
@ConfigurationProperties("growtopia.api")
public record WikiApiProperties(String key, String url) {
}
