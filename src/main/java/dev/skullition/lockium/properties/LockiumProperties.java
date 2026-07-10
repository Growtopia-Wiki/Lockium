package dev.skullition.lockium.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Core Lockium application settings.
 *
 * <p>Bound from properties prefixed with {@code lockium}.
 *
 * @param status the Discord presence text shown by the bot (e.g. "Playing Growtopia")
 * @param detailUrl base URL for the Growtopia detail endpoint used by {@link
 *     dev.skullition.lockium.client.GrowtopiaDetailClient}
 * @param renderUrl base URL for world/item renders used in {@code /gt wotd} and render commands;
 *     must end with a trailing slash
 * @param itemsCacheDuration TTL of the {@code items} and {@code itemIndex} caches registered in
 *     {@code CacheConfig}; parsed as a {@link Duration} (e.g. {@code 1h}, {@code PT30M})
 * @param wikiRawUrl base URL for public raw Growtopia Wiki pages
 * @param scrapedEffectsPath external additions-only file containing effects scraped at runtime
 */
@ConfigurationProperties("lockium")
public record LockiumProperties(
    String status,
    String detailUrl,
    String renderUrl,
    Duration itemsCacheDuration,
    String wikiRawUrl,
    String scrapedEffectsPath) {}
