package dev.skullition.lockium.model;

/**
 * One readable listing from the proxy's {@code GET /auction} snapshot.
 *
 * @param name displayed item name
 * @param owner GrowID that owns the auction
 * @param world world hosting the auction
 * @param bidWl current bid in World Locks
 * @param remainingSeconds countdown observed on the source page when it was fetched
 */
public record AuctionListing(
    String name, String owner, String world, long bidWl, long remainingSeconds) {}
