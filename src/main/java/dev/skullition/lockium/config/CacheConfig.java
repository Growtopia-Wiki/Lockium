package dev.skullition.lockium.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    private final int itemsCacheDuration;

    public CacheConfig(@Value("${lockium.items-cache-duration}") int itemsCacheDuration) {
        this.itemsCacheDuration = itemsCacheDuration;
    }

    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("items",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(itemsCacheDuration))
                        .maximumSize(1)
                        .recordStats()
                        .build());

        cacheManager.registerCustomCache("itemIndex",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(itemsCacheDuration))
                        .maximumSize(1)
                        .build());

        return cacheManager;
    }
}
