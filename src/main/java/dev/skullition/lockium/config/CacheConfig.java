package dev.skullition.lockium.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import dev.skullition.lockium.properties.LockiumProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    private final LockiumProperties properties;

    public CacheConfig(LockiumProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("items",
                Caffeine.newBuilder()
                        .expireAfterWrite(properties.itemsCacheDuration())
                        .maximumSize(1)
                        .recordStats()
                        .build());

        cacheManager.registerCustomCache("itemIndex",
                Caffeine.newBuilder()
                        .expireAfterWrite(properties.itemsCacheDuration())
                        .maximumSize(1)
                        .build());

        return cacheManager;
    }
}
