package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The {@code data} object of the proxy's {@code GET /leaderboard} route.
 *
 * <p>{@link #leagues()} holds all 18 boards, keyed {@code bronze_1} through {@code sapphire_3}.
 * {@link #overall()} is computed by the proxy across every league, and each of its entries carries
 * the league it came from, so {@code leagues().get(entry.league())} yields the originating board.
 *
 * @param overall the computed top entries across every league
 * @param leagues each league's own board, keyed by the league's API key
 */
public record GrowtopiaLeaderboard(
    @JsonProperty("overall") List<LeaderboardEntry> overall,
    @JsonProperty("leagues") Map<String, List<LeaderboardEntry>> leagues) {}
