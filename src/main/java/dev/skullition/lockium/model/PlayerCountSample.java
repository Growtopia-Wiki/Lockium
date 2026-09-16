package dev.skullition.lockium.model;

import java.time.Instant;

/**
 * One observation of the number of players online.
 *
 * <p>Recorded by the scheduled {@code /detail} poll and stored in {@code
 * lockium.player_count_sample}, then read back to draw the history graph shown by {@code /gt
 * stats}. Samples live in an in-memory database and therefore do not survive a restart.
 *
 * @param sampledAt when the count was observed
 * @param onlineCount number of players online at that moment
 */
public record PlayerCountSample(Instant sampledAt, int onlineCount) {}
