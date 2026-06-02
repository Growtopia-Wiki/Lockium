package dev.skullition.lockium.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Growtopia Wiki API client.
 *
 * <p>Bound from properties prefixed with {@code growtopia.api}.
 *
 * <p>Used by {@code WikiClient} to authenticate requests via the {@code X-Api-Key} header.
 *
 * @param key the API key issued by the Wiki; sent as {@code X-Api-Key}
 * @param url the base URL of the Wiki API (no trailing slash recommended)
 */
@ConfigurationProperties("growtopia.api")
public record WikiApiProperties(String key, String url) {}
