package dev.skullition.lockium.properties;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discord")
@NullMarked
public record DiscordProperties(String token) {}
