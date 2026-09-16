package dev.skullition.lockium.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Core Lockium application settings.
 *
 * <p>Bound from properties prefixed with {@code lockium}. Proxy settings live separately in {@link
 * ProxyProperties}.
 *
 * @param status the Discord presence text shown by the bot (e.g. "Playing Growtopia")
 * @param renderUrl base URL for world/item renders used by {@code /gt world}; must end with a
 *     trailing slash
 * @param itemsCacheDuration TTL of the {@code items} and {@code itemIndex} caches registered in
 *     {@code CacheConfig}; parsed as a {@link Duration} (e.g. {@code 1h}, {@code PT30M})
 * @param wikiRawUrl base URL for public raw Growtopia Wiki pages
 * @param scrapedEffectsPath external additions-only file containing effects scraped at runtime
 */
@ConfigurationProperties("lockium")
public record LockiumProperties(
    String status,
    String renderUrl,
    Duration itemsCacheDuration,
    String wikiRawUrl,
    String scrapedEffectsPath) {}
