package dev.skullition.lockium.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * A single row of a Growtopia leaderboard.
 *
 * <p>The proxy returns two shapes that differ only by the presence of a league: entries inside
 * {@code leagues[key]} omit it, because the board they belong to is already known from the map key,
 * while entries in the computed {@code overall} board carry it so the row can be traced back to its
 * board. Records cannot extend records, so both shapes bind to this one type and {@link #league()}
 * is simply {@code null} for per-league rows.
 *
 * @param rank position within the board this entry came from, starting at 1
 * @param name the player's name, which is user-controlled text and must be escaped or wrapped in
 *     backticks before being rendered in Discord
 * @param score the player's score
 * @param league key of the board this entry came from, as accepted by {@link
 *     League#fromApiKey(String)}; {@code null} for entries read out of a per-league board
 */
public record LeaderboardEntry(
    @JsonProperty("rank") int rank,
    @JsonProperty("name") String name,
    @JsonProperty("score") long score,
    @JsonProperty("league") @Nullable String league) {}
