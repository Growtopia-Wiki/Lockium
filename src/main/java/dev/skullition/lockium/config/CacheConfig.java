package dev.skullition.lockium.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import dev.skullition.lockium.properties.LockiumProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central Spring caching configuration for Lockium.
 *
 * <p>Enables annotation-driven caching ({@code @Cacheable}, {@code @CacheEvict}) and provides a
 * single {@link CaffeineCacheManager} with application-specific caches tuned for the Wiki API.
 *
 * <h2>Caches</h2>
 *
 * <ul>
 *   <li><b>items</b> – holds the full {@code /v1/items} payload ({@link
 *       dev.skullition.lockium.model.ItemsResponse}). Configured with {@code maximumSize=1} because
 *       the response is a single large object. Expires {@code expireAfterWrite} using {@link
 *       LockiumProperties#itemsCacheDuration()}, and records stats for {@code /actuator/caches}.
 *   <li><b>itemIndex</b> – holds the lightweight index used for autocomplete and ID lookups. Shares
 *       the same TTL as {@code items} but does not record stats to reduce overhead.
 * </ul>
 *
 * <p>All TTLs are externalized to avoid recompiles when tuning cache behavior.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  private final LockiumProperties properties;

  /**
   * Creates the cache configuration.
   *
   * @param properties typed properties containing cache durations; never {@code null}
   */
  public CacheConfig(LockiumProperties properties) {
    this.properties = properties;
  }

  /**
   * Primary {@link CacheManager} used by Spring's caching abstraction.
   *
   * <p>Registers custom Caffeine caches programmatically.
   *
   * @return a configured {@link CaffeineCacheManager} with {@code items} and {@code itemIndex}
   *     caches pre-registered
   */
  @Bean
  public CacheManager cacheManager() {
    var cacheManager = new CaffeineCacheManager();
    cacheManager.registerCustomCache(
        "items",
        Caffeine.newBuilder()
            .expireAfterWrite(properties.itemsCacheDuration())
            .maximumSize(1)
            .recordStats()
            .build());

    cacheManager.registerCustomCache(
        "itemIndex",
        Caffeine.newBuilder()
            .expireAfterWrite(properties.itemsCacheDuration())
            .maximumSize(1)
            .build());

    return cacheManager;
  }
}
