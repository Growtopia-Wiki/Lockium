package dev.skullition.lockium.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("lockium")
public record LockiumProperties(String status, String detailUrl, String renderUrl, Duration itemsCacheDuration) {
}
