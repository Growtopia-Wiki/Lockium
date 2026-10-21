package dev.skullition.lockium.model;

import java.util.List;

/**
 * The {@code data} object of the proxy's {@code GET /auction} route.
 *
 * @param auctions listings in source order; duplicate-looking rows are retained
 */
public record GrowtopiaAuction(List<AuctionListing> auctions) {

  /** Copies listings so a published snapshot cannot change after construction. */
  public GrowtopiaAuction {
    auctions = List.copyOf(auctions);
  }
}
