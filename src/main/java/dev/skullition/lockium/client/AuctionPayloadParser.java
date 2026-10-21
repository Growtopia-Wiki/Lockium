package dev.skullition.lockium.client;

import dev.skullition.lockium.model.AuctionListing;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/** Validates the auction proxy's JSON response before publishing it as typed data. */
public final class AuctionPayloadParser {
  private static final BigInteger MAX_SAFE_INTEGER = new BigInteger("9007199254740991");

  private AuctionPayloadParser() {}

  /**
   * Parses and validates one auction response envelope.
   *
   * <p>Unknown fields are ignored. Missing or malformed required fields reject the whole response,
   * so a broken upstream response can never replace a previously accepted snapshot.
   *
   * @param json response body from {@code GET /auction}
   * @return immutable, validated auction data
   * @throws IllegalArgumentException when the response does not match the proxy contract
   */
  public static ProxyPayload<GrowtopiaAuction> parse(JsonNode json) {
    requiredObject(json, "response");
    Instant fetchedAt = requiredInstant(json, "fetched_at", "response");
    List<String> warnings = requiredStrings(json.get("warnings"), "response.warnings");

    JsonNode data = requiredObject(json.get("data"), "response.data");
    @Nullable JsonNode auctionArray = data.get("auctions");
    if (auctionArray == null || !auctionArray.isArray()) {
      throw invalid("response.data.auctions", "expected an array");
    }

    List<AuctionListing> auctions = new ArrayList<>();
    for (int index = 0; index < auctionArray.size(); index++) {
      String path = "response.data.auctions[" + index + "]";
      JsonNode row = requiredObject(auctionArray.get(index), path);
      auctions.add(
          new AuctionListing(
              requiredText(row, "name", path),
              requiredText(row, "owner", path),
              requiredText(row, "world", path),
              requiredSafeInteger(row, "bid_wl", path),
              requiredSafeInteger(row, "remaining_seconds", path)));
    }

    @Nullable JsonNode staleNode = json.get("stale");
    ProxyPayload.Stale stale = staleNode == null ? null : parseStale(staleNode);
    return new ProxyPayload<>(fetchedAt, warnings, new GrowtopiaAuction(auctions), stale);
  }

  private static ProxyPayload.Stale parseStale(JsonNode staleNode) {
    JsonNode stale = requiredObject(staleNode, "response.stale");
    String reason = requiredText(stale, "reason", "response.stale");
    if (!reason.equals("origin") && !reason.equals("parse")) {
      throw invalid("response.stale.reason", "expected 'origin' or 'parse'");
    }
    String message = requiredText(stale, "message", "response.stale");
    Instant servedAt = requiredInstant(stale, "served_at", "response.stale");
    return new ProxyPayload.Stale(reason, message, servedAt);
  }

  private static List<String> requiredStrings(@Nullable JsonNode node, String path) {
    if (node == null || !node.isArray()) {
      throw invalid(path, "expected an array of strings");
    }
    List<String> strings = new ArrayList<>();
    for (int index = 0; index < node.size(); index++) {
      @Nullable JsonNode value = node.get(index);
      if (value == null || !value.isString()) {
        throw invalid(path + "[" + index + "]", "expected text");
      }
      strings.add(value.stringValue());
    }
    return List.copyOf(strings);
  }

  private static JsonNode requiredObject(@Nullable JsonNode node, String path) {
    if (node == null || !node.isObject()) {
      throw invalid(path, "expected an object");
    }
    return node;
  }

  private static String requiredText(JsonNode parent, String field, String path) {
    @Nullable JsonNode node = parent.get(field);
    if (node == null || !node.isString() || node.stringValue().isBlank()) {
      throw invalid(path + "." + field, "expected nonblank text");
    }
    return node.stringValue();
  }

  private static long requiredSafeInteger(JsonNode parent, String field, String path) {
    @Nullable JsonNode node = parent.get(field);
    if (node == null || !node.isIntegralNumber()) {
      throw invalid(path + "." + field, "expected a nonnegative safe integer");
    }
    BigInteger value = node.bigIntegerValue();
    if (value.signum() < 0 || value.compareTo(MAX_SAFE_INTEGER) > 0) {
      throw invalid(path + "." + field, "expected a nonnegative safe integer");
    }
    return value.longValueExact();
  }

  private static Instant requiredInstant(JsonNode parent, String field, String path) {
    String value = requiredText(parent, field, path);
    if (!value.endsWith("Z")) {
      throw invalid(path + "." + field, "expected an ISO-8601 UTC timestamp");
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      throw invalid(path + "." + field, "expected an ISO-8601 UTC timestamp");
    }
  }

  private static IllegalArgumentException invalid(String path, String reason) {
    return new IllegalArgumentException(path + ": " + reason);
  }
}
