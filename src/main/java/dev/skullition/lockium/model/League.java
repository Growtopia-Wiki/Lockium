package dev.skullition.lockium.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * The 18 Growtopia leaderboard leagues, from Bronze I up to Sapphire III.
 *
 * <p>Each constant pairs a human display name with the key the proxy uses in its {@code leagues}
 * map. The API key is stored explicitly rather than derived from {@link #name()} so that renaming a
 * constant cannot silently break the contract with the proxy.
 *
 * <p>Exposed as predefined slash-command choices by {@code LeagueResolver}. The count stays within
 * Discord's limit of 25 choices per option.
 */
public enum League {
  BRONZE_1("Bronze I", "bronze_1"),
  BRONZE_2("Bronze II", "bronze_2"),
  BRONZE_3("Bronze III", "bronze_3"),
  SILVER_1("Silver I", "silver_1"),
  SILVER_2("Silver II", "silver_2"),
  SILVER_3("Silver III", "silver_3"),
  GOLD_1("Gold I", "gold_1"),
  GOLD_2("Gold II", "gold_2"),
  GOLD_3("Gold III", "gold_3"),
  PLATINUM_1("Platinum I", "platinum_1"),
  PLATINUM_2("Platinum II", "platinum_2"),
  PLATINUM_3("Platinum III", "platinum_3"),
  DIAMOND_1("Diamond I", "diamond_1"),
  DIAMOND_2("Diamond II", "diamond_2"),
  DIAMOND_3("Diamond III", "diamond_3"),
  SAPPHIRE_1("Sapphire I", "sapphire_1"),
  SAPPHIRE_2("Sapphire II", "sapphire_2"),
  SAPPHIRE_3("Sapphire III", "sapphire_3");

  /** Lookup from the proxy's league key to the matching constant. */
  private static final Map<String, League> BY_API_KEY =
      Arrays.stream(values()).collect(Collectors.toMap(League::getApiKey, Function.identity()));

  private final String displayName;
  private final String apiKey;

  League(String displayName, String apiKey) {
    this.displayName = displayName;
    this.apiKey = apiKey;
  }

  /**
   * Returns the name shown to users, for example {@code Sapphire III}.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the key the proxy uses for this league, for example {@code sapphire_3}.
   *
   * @return the API key
   */
  public String getApiKey() {
    return apiKey;
  }

  /**
   * Resolves a league from the key used by the proxy.
   *
   * @param apiKey the key from the proxy's {@code leagues} map or an entry's {@code league} field;
   *     may be {@code null}
   * @return the matching league, or {@code null} when the key is unknown, which happens if the
   *     upstream board set changes
   */
  @Nullable
  public static League fromApiKey(@Nullable String apiKey) {
    return apiKey == null ? null : BY_API_KEY.get(apiKey);
  }
}
