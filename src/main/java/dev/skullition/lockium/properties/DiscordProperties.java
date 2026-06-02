package dev.skullition.lockium.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Discord bot credentials.
 *
 * <p>Bound from properties prefixed with {@code discord}.
 *
 * <pre>
 *     discord.token = TOKEN
 * </pre>
 *
 * <p>This record is injected wherever the JDA token is needed and is validated at startup by Spring
 * Boot's configuration binding.
 *
 * @param token the bot token used to log in to Discord; must be a valid bot token, never {@code
 *     null} or blank in production
 */
@ConfigurationProperties("discord")
public record DiscordProperties(String token) {}
