package dev.skullition.lockium.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the internal Growtopia proxy.
 *
 * <p>Bound from properties prefixed with {@code lockium.proxy}.
 *
 * <p>The proxy allowlists a single IP and answers everyone else with HTTP 403, so a developer
 * machine normally cannot reach it. Everything downstream degrades to "data unavailable" rather
 * than failing, and no test touches the network.
 *
 * @param url base URL of the proxy, used by {@link
 *     dev.skullition.lockium.client.GrowtopiaProxyClient}
 * @param detailPollInterval how often {@code /detail} is polled; the proxy refreshes it once a
 *     minute, so polling faster only re-reads its cache
 * @param leaderboardPollInterval how often {@code /leaderboard} is polled
 * @param graphWindow how far back the {@code /gt stats} player-count graph reaches
 * @param sampleRetention how long player-count samples are kept; must exceed {@code graphWindow} so
 *     pruning cannot race the graph query
 */
@ConfigurationProperties("lockium.proxy")
public record ProxyProperties(
    String url,
    Duration detailPollInterval,
    Duration leaderboardPollInterval,
    Duration graphWindow,
    Duration sampleRetention) {}
