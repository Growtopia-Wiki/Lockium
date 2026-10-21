package dev.skullition.lockium.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for polling and retaining the proxy's auction snapshot.
 *
 * @param enabled whether scheduled and manual refreshes are allowed
 * @param pollInterval how often the scheduler checks for a new snapshot
 * @param maxAge maximum age from proxy {@code fetched_at} for a snapshot to remain visible
 * @param maxBackoff maximum retry delay after consecutive failures
 */
@ConfigurationProperties("lockium.proxy.auction")
public record AuctionProperties(
    boolean enabled, Duration pollInterval, Duration maxAge, Duration maxBackoff) {}
