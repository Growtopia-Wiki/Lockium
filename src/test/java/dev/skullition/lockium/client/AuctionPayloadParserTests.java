package dev.skullition.lockium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Tests strict validation of auction proxy envelopes. */
class AuctionPayloadParserTests {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void parsesWarningsUnknownFieldsZeroesDuplicatesAndStaleMetadata() throws Exception {
    JsonNode json =
        MAPPER.readTree(
            """
            {
              "fetched_at": "2026-10-21T14:45:02.000Z",
              "warnings": ["one row omitted"],
              "data": {
                "auctions": [
                  {
                    "name": "Megaphone",
                    "owner": "ThePlane",
                    "world": "PINATALAND",
                    "bid_wl": 0,
                    "remaining_seconds": 9007199254740991,
                    "future_field": true
                  },
                  {
                    "name": "Megaphone",
                    "owner": "ThePlane",
                    "world": "PINATALAND",
                    "bid_wl": 0,
                    "remaining_seconds": 0
                  }
                ],
                "future_data_field": "ignored"
              },
              "stale": {
                "reason": "parse",
                "message": "source markup changed",
                "served_at": "2026-10-21T14:46:00Z"
              },
              "future_envelope_field": 1
            }
            """);

    ProxyPayload<GrowtopiaAuction> payload = AuctionPayloadParser.parse(json);

    assertEquals("2026-10-21T14:45:02Z", payload.fetchedAt().toString());
    assertEquals(1, payload.warnings().size());
    assertEquals("parse", payload.stale().reason());
    assertEquals(2, payload.data().auctions().size());
    assertEquals(0L, payload.data().auctions().getFirst().bidWl());
    assertEquals(9_007_199_254_740_991L, payload.data().auctions().getFirst().remainingSeconds());
    assertEquals("Megaphone", payload.data().auctions().getLast().name());
  }

  @Test
  void acceptsAnEmptyAuctionList() throws Exception {
    JsonNode json = MAPPER.readTree(envelope(TIME, "[]", "{\"auctions\":[]}", null));

    assertTrue(AuctionPayloadParser.parse(json).data().auctions().isEmpty());
  }

  @Test
  void acceptsAnEmptyWarningString() throws Exception {
    JsonNode json = MAPPER.readTree(envelope(TIME, "[\"\"]", "{\"auctions\":[]}", null));

    assertEquals(List.of(""), AuctionPayloadParser.parse(json).warnings());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidEnvelopes")
  void rejectsMalformedRequiredValues(String description, String value) throws Exception {
    JsonNode json = MAPPER.readTree(value);
    assertThrows(IllegalArgumentException.class, () -> AuctionPayloadParser.parse(json));
  }

  private static Stream<Arguments> invalidEnvelopes() {
    return Stream.of(
        Arguments.of("root is null", "null"),
        Arguments.of("root is not an object", "[]"),
        Arguments.of("fetched_at is missing", "{\"warnings\":[],\"data\":{\"auctions\":[]}}"),
        Arguments.of("fetched_at is null", envelope("null", "[]", "{\"auctions\":[]}", null)),
        Arguments.of("fetched_at is not text", envelope("1", "[]", "{\"auctions\":[]}", null)),
        Arguments.of(
            "fetched_at is not a timestamp",
            envelope("\"tomorrow\"", "[]", "{\"auctions\":[]}", null)),
        Arguments.of(
            "fetched_at is not UTC",
            envelope("\"2026-10-21T14:45:02+00:00\"", "[]", "{\"auctions\":[]}", null)),
        Arguments.of(
            "warnings is missing",
            "{\"fetched_at\":\"2026-10-21T14:45:02Z\",\"data\":{\"auctions\":[]}}"),
        Arguments.of("warnings is null", envelope(TIME, "null", "{\"auctions\":[]}", null)),
        Arguments.of("warnings is not an array", envelope(TIME, "{}", "{\"auctions\":[]}", null)),
        Arguments.of("warning entry is null", envelope(TIME, "[null]", "{\"auctions\":[]}", null)),
        Arguments.of("warning entry is not text", envelope(TIME, "[1]", "{\"auctions\":[]}", null)),
        Arguments.of(
            "data is missing", "{\"fetched_at\":\"2026-10-21T14:45:02Z\",\"warnings\":[]}"),
        Arguments.of("data is null", envelope(TIME, "[]", "null", null)),
        Arguments.of("data is not an object", envelope(TIME, "[]", "[]", null)),
        Arguments.of("auctions is missing", envelope(TIME, "[]", "{}", null)),
        Arguments.of("auctions is null", envelope(TIME, "[]", "{\"auctions\":null}", null)),
        Arguments.of("auctions is not an array", envelope(TIME, "[]", "{\"auctions\":{}}", null)),
        Arguments.of("auction row is null", envelope(TIME, "[]", data("null"), null)),
        Arguments.of("auction row is not an object", envelope(TIME, "[]", data("1"), null)),
        Arguments.of(
            "name is missing",
            envelope(
                TIME,
                "[]",
                data(
                    """
                    {"owner":"Owner","world":"WORLD","bid_wl":1,
                     "remaining_seconds":1}
                    """),
                null)),
        Arguments.of(
            "name is null",
            envelope(TIME, "[]", data(listing("null", "\"Owner\"", "\"WORLD\"", "1", "1")), null)),
        Arguments.of(
            "name is not text",
            envelope(TIME, "[]", data(listing("1", "\"Owner\"", "\"WORLD\"", "1", "1")), null)),
        Arguments.of(
            "name is blank",
            envelope(TIME, "[]", data(listing("\" \"", "\"Owner\"", "\"WORLD\"", "1", "1")), null)),
        Arguments.of(
            "owner is null",
            envelope(TIME, "[]", data(listing("\"Item\"", "null", "\"WORLD\"", "1", "1")), null)),
        Arguments.of(
            "owner is not text",
            envelope(TIME, "[]", data(listing("\"Item\"", "1", "\"WORLD\"", "1", "1")), null)),
        Arguments.of(
            "world is null",
            envelope(TIME, "[]", data(listing("\"Item\"", "\"Owner\"", "null", "1", "1")), null)),
        Arguments.of(
            "world is blank",
            envelope(TIME, "[]", data(listing("\"Item\"", "\"Owner\"", "\" \"", "1", "1")), null)),
        Arguments.of(
            "bid_wl is null",
            envelope(
                TIME,
                "[]",
                data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "null", "1")),
                null)),
        Arguments.of(
            "bid_wl is missing",
            envelope(
                TIME,
                "[]",
                data(
                    """
                    {"name":"Item","owner":"Owner","world":"WORLD",
                     "remaining_seconds":1}
                    """),
                null)),
        Arguments.of(
            "bid_wl is not integral",
            envelope(
                TIME, "[]", data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "1.0", "1")), null)),
        Arguments.of(
            "bid_wl is negative",
            envelope(
                TIME, "[]", data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "-1", "1")), null)),
        Arguments.of(
            "bid_wl exceeds the safe integer range",
            envelope(
                TIME,
                "[]",
                data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "9007199254740992", "1")),
                null)),
        Arguments.of(
            "remaining_seconds is a string",
            envelope(
                TIME,
                "[]",
                data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "1", "\"1\"")),
                null)),
        Arguments.of(
            "remaining_seconds is missing",
            envelope(
                TIME,
                "[]",
                data("{\"name\":\"Item\",\"owner\":\"Owner\",\"world\":\"WORLD\",\"bid_wl\":1}"),
                null)),
        Arguments.of(
            "remaining_seconds is not integral",
            envelope(
                TIME, "[]", data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "1", "1.0")), null)),
        Arguments.of(
            "remaining_seconds exceeds the safe integer range",
            envelope(
                TIME,
                "[]",
                data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "1", "9007199254740992")),
                null)),
        Arguments.of(
            "remaining_seconds is negative",
            envelope(
                TIME, "[]", data(listing("\"Item\"", "\"Owner\"", "\"WORLD\"", "1", "-1")), null)),
        Arguments.of(
            "explicit null stale metadata is invalid",
            envelope(TIME, "[]", "{\"auctions\":[]}", "null")),
        Arguments.of(
            "stale metadata is not an object", envelope(TIME, "[]", "{\"auctions\":[]}", "[]")),
        Arguments.of(
            "stale reason is unknown",
            envelope(TIME, "[]", "{\"auctions\":[]}", stale("\"cache\"", "\"message\"", TIME))),
        Arguments.of(
            "stale reason is missing",
            envelope(
                TIME,
                "[]",
                "{\"auctions\":[]}",
                "{\"message\":\"message\",\"served_at\":" + TIME + "}")),
        Arguments.of(
            "stale message is missing",
            envelope(
                TIME,
                "[]",
                "{\"auctions\":[]}",
                "{\"reason\":\"origin\",\"served_at\":" + TIME + "}")),
        Arguments.of(
            "stale message is null",
            envelope(TIME, "[]", "{\"auctions\":[]}", stale("\"origin\"", "null", TIME))),
        Arguments.of(
            "stale message is blank",
            envelope(TIME, "[]", "{\"auctions\":[]}", stale("\"origin\"", "\" \"", TIME))),
        Arguments.of(
            "stale served_at is invalid",
            envelope(
                TIME, "[]", "{\"auctions\":[]}", stale("\"origin\"", "\"message\"", "\"bad\""))),
        Arguments.of(
            "stale served_at is null",
            envelope(TIME, "[]", "{\"auctions\":[]}", stale("\"origin\"", "\"message\"", "null"))),
        Arguments.of(
            "stale served_at is missing",
            envelope(
                TIME,
                "[]",
                "{\"auctions\":[]}",
                "{\"reason\":\"origin\",\"message\":\"message\"}")));
  }

  private static String envelope(String fetchedAt, String warnings, String data, String stale) {
    return "{\"fetched_at\":"
        + fetchedAt
        + ",\"warnings\":"
        + warnings
        + ",\"data\":"
        + data
        + (stale == null ? "" : ",\"stale\":" + stale)
        + "}";
  }

  private static String data(String row) {
    return "{\"auctions\":[" + row + "]}";
  }

  private static String listing(
      String name, String owner, String world, String bidWl, String remainingSeconds) {
    return "{\"name\":"
        + name
        + ",\"owner\":"
        + owner
        + ",\"world\":"
        + world
        + ",\"bid_wl\":"
        + bidWl
        + ",\"remaining_seconds\":"
        + remainingSeconds
        + "}";
  }

  private static String stale(String reason, String message, String servedAt) {
    return "{\"reason\":" + reason + ",\"message\":" + message + ",\"served_at\":" + servedAt + "}";
  }

  private static final String TIME = "\"2026-10-21T14:45:02Z\"";
}
