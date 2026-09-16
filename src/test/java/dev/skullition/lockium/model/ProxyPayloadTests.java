package dev.skullition.lockium.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests deserialization of the proxy envelope.
 *
 * <p>Pins the arrangement the whole proxy integration rests on: Jackson 2 annotations read by a
 * Jackson 3 mapper, {@link Instant} binding without an explicitly registered module, and generic
 * payload resolution through a {@link TypeReference}.
 */
class ProxyPayloadTests {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void deserializesLiveDetailEnvelope() {
    String json =
        """
        {
          "fetched_at": "2026-08-31T18:39:23.621Z",
          "warnings": [],
          "data": {
            "online_count": 69221,
            "wotd": "https://s3.amazonaws.com/world.growtopiagame.com/veilora.png"
          }
        }
        """;

    ProxyPayload<GrowtopiaDetail> payload = MAPPER.readValue(json, new TypeReference<>() {});

    assertEquals(Instant.parse("2026-08-31T18:39:23.621Z"), payload.fetchedAt());
    assertTrue(payload.warnings().isEmpty());
    assertNull(payload.stale());
    assertFalse(payload.isStale());
    assertEquals(69221, payload.data().onlineCount());
    assertEquals("VEILORA", payload.data().wotdName());
  }

  @Test
  void deserializesStaleEnvelopeWithWarnings() {
    String json =
        """
        {
          "fetched_at": "2026-08-31T18:00:00Z",
          "warnings": ["online_count looked odd", "wotd missing"],
          "data": { "online_count": 0, "wotd": null },
          "stale": {
            "reason": "origin",
            "message": "origin /detail returned 403",
            "served_at": "2026-08-31T17:55:00Z"
          }
        }
        """;

    ProxyPayload<GrowtopiaDetail> payload = MAPPER.readValue(json, new TypeReference<>() {});

    assertTrue(payload.isStale());
    assertEquals("origin", payload.stale().reason());
    assertEquals("origin /detail returned 403", payload.stale().message());
    assertEquals(Instant.parse("2026-08-31T17:55:00Z"), payload.stale().servedAt());
    assertEquals(2, payload.warnings().size());
    assertNull(payload.data().wotdUrl());
  }

  @Test
  void deserializesLeaderboardEnvelope() {
    String json =
        """
        {
          "fetched_at": "2026-08-31T18:39:26.899Z",
          "warnings": [],
          "data": {
            "overall": [
              { "rank": 1, "name": "Castor", "score": 257500, "league": "sapphire_3" },
              { "rank": 2, "name": "Onuzu", "score": 198000, "league": "sapphire_3" }
            ],
            "leagues": {
              "bronze_1": [{ "rank": 1, "name": "pituharaLdd", "score": 32000 }],
              "sapphire_3": [{ "rank": 1, "name": "Castor", "score": 257500 }]
            }
          }
        }
        """;

    ProxyPayload<GrowtopiaLeaderboard> payload = MAPPER.readValue(json, new TypeReference<>() {});

    GrowtopiaLeaderboard leaderboard = payload.data();
    assertEquals(2, leaderboard.overall().size());
    assertEquals("sapphire_3", leaderboard.overall().getFirst().league());
    assertEquals(257500L, leaderboard.overall().getFirst().score());

    // Entries inside a per-league board carry no league of their own.
    LeaderboardEntry bronze = leaderboard.leagues().get("bronze_1").getFirst();
    assertEquals("pituharaLdd", bronze.name());
    assertNull(bronze.league());

    // An overall entry maps back to the board it came from.
    assertEquals(
        League.SAPPHIRE_3,
        League.fromApiKey(leaderboard.overall().getFirst().league()));
  }
}
