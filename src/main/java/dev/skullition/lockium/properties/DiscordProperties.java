package dev.skullition.lockium.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discord")
public record DiscordProperties(String token) {}
