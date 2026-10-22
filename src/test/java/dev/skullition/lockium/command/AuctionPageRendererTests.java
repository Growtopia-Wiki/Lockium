package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.skullition.lockium.model.AuctionListing;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Fixed-clock tests for the auction listing renderer and its Discord text bounds. */
class AuctionPageRendererTests {
  private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final Duration MAX_AGE = Duration.ofMinutes(10);

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @Test
  void keepsSourceOrderAndDuplicatesAcrossPagesAndClampsPageIndex() {
    AuctionPageRenderer renderer = renderer();
    AuctionListing first = listing("Golden Chair", "Owner", "WORLD", 1, 3_600);
    AuctionListing duplicate = listing("Golden Chair", "Owner", "WORLD", 1, 3_600);
    List<AuctionListing> listings =
        List.of(
            first,
            duplicate,
            listing("Dirt", "Owner", "WORLD", 2, 3_600),
            listing("Rock", "Owner", "WORLD", 3, 3_600),
            listing("Lava", "Owner", "WORLD", 4, 3_600),
            listing("Door", "Owner", "WORLD", 5, 3_600));

    AuctionPageRenderer.RenderedPage firstPage =
        renderer.render(payload(listings, NOW), null, "", 0);
    final AuctionPageRenderer.RenderedPage clampedLastPage =
        renderer.render(payload(listings, NOW), null, "", 99);

    assertEquals(0, firstPage.pageIndex());
    assertEquals(2, firstPage.pageCount());
    assertTrue(pageText(firstPage).indexOf("Golden Chair") < pageText(firstPage).indexOf("Dirt"));
    assertEquals(2, occurrences(pageText(firstPage), "Golden Chair"));
    assertEquals(1, clampedLastPage.pageIndex());
    assertEquals(2, clampedLastPage.pageCount());
    assertTrue(pageText(clampedLastPage).contains("**Door**"));
    assertFalse(pageText(clampedLastPage).contains("**Golden Chair**"));
  }

  @Test
  void filtersItemNamesWithoutCaseSensitivityAndClampsNegativePage() {
    AuctionPageRenderer renderer = renderer();
    ProxyPayload<GrowtopiaAuction> payload =
        payload(
            List.of(
                listing("Golden Chair", "Owner", "WORLD", 1, 60),
                listing("Chair Seed", "Owner", "WORLD", 2, 60),
                listing("Dirt", "Owner", "WORLD", 3, 60)),
            NOW);

    AuctionPageRenderer.RenderedPage page = renderer.render(payload, null, "cHaIr", -10);

    assertEquals(0, page.pageIndex());
    assertEquals(1, page.pageCount());
    assertTrue(pageText(page).contains("2 results"));
    assertTrue(pageText(page).contains("**Golden Chair**"));
    assertTrue(pageText(page).contains("**Chair Seed**"));
    assertFalse(pageText(page).contains("**Dirt**"));
  }

  @Test
  void distinguishesEmptySnapshotFromNoFilterMatchesAndUnavailablePayload() {
    AuctionPageRenderer renderer = renderer();
    AuctionPageRenderer.RenderedPage empty = renderer.render(payload(List.of(), NOW), null, "", 0);
    AuctionPageRenderer.RenderedPage noMatches =
        renderer.render(
            payload(List.of(listing("Dirt", "Owner", "WORLD", 1, 60)), NOW), null, "Rock", 0);
    AuctionPageRenderer.RenderedPage unavailable = renderer.render(null, null, "", 0);
    final AuctionPageRenderer.RenderedPage failed = renderer.render(null, NOW, "", 0);

    assertTrue(pageText(empty).contains("This snapshot contains no auctions."));
    assertTrue(pageText(noMatches).contains("No auctions matched"));
    assertTrue(pageText(unavailable).contains("currently unavailable"));
    assertTrue(pageText(failed).contains("latest refresh failed"));
  }

  @Test
  void countsWarningsAndShowsStaleOutOfDateAndRefreshFailureSeparately() {
    AuctionPageRenderer renderer = renderer();
    ProxyPayload<GrowtopiaAuction> payload =
        new ProxyPayload<>(
            NOW.minus(Duration.ofMinutes(3)),
            List.of("warning 1", "warning 2"),
            new GrowtopiaAuction(List.of(listing("Dirt", "Owner", "WORLD", 1, 120))),
            new ProxyPayload.Stale("origin", "cached copy", NOW));

    String text = pageText(renderer.render(payload, NOW.minusSeconds(5), "", 0));

    assertTrue(text.contains("Snapshot: " + relative(NOW.minus(Duration.ofMinutes(3))) + "."));
    assertTrue(text.contains("Timers are estimates."));
    assertTrue(text.contains("stale cached data"));
    assertTrue(text.contains("source unavailable"));
    assertTrue(text.contains("latest refresh failed; showing the last accepted snapshot"));
    assertTrue(text.contains("Parsing warnings: 2."));
    assertTrue(text.contains("Estimated end: " + relative(NOW.minusSeconds(60))));
    assertTrue(text.contains("Timer elapsed; outcome unknown"));
    assertFalse(text.contains("cached copy"));
  }

  @Test
  void agesTimersFromFetchedAtAndClampsAtZero() {
    AuctionPageRenderer renderer = renderer();
    ProxyPayload<GrowtopiaAuction> payload =
        new ProxyPayload<>(
            NOW.minus(Duration.ofMinutes(5)),
            List.of(),
            new GrowtopiaAuction(
                List.of(
                    listing("Still listed", "Owner", "WORLD", 1, 600),
                    listing("Elapsed", "Owner", "WORLD", 1, 299))),
            new ProxyPayload.Stale("origin", "ignored", NOW.minusSeconds(1)));

    String text = pageText(renderer.render(payload, null, "", 0));

    String expectedEnd = relative(NOW.plus(Duration.ofMinutes(5)));
    assertTrue(text.contains("Snapshot: " + relative(NOW.minus(Duration.ofMinutes(5))) + "."));
    assertTrue(text.contains("Estimated end: " + expectedEnd));
    assertTrue(text.contains("Estimated end: " + relative(NOW.minusSeconds(1))));
    assertTrue(text.contains("Timer elapsed; outcome unknown"));
    assertFalse(text.contains("Estimated end: " + relative(NOW.plusSeconds(599))));

    AuctionPageRenderer laterRenderer =
        new AuctionPageRenderer(Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC), MAX_AGE);
    String laterText = pageText(laterRenderer.render(payload, null, "", 0));
    assertTrue(laterText.contains("Estimated end: " + expectedEnd));
  }

  @Test
  void clampsFutureFetchedAtForBothSnapshotAgeAndCountdown() {
    AuctionPageRenderer renderer = renderer();
    ProxyPayload<GrowtopiaAuction> payload =
        payload(List.of(listing("Future", "Owner", "WORLD", 1, 90)), NOW.plusSeconds(300));

    String text = pageText(renderer.render(payload, null, "", 0));

    assertTrue(text.contains("Snapshot: " + relative(NOW.plusSeconds(300)) + "."));
    assertTrue(text.contains("Estimated end: " + relative(NOW.plusSeconds(90))));
  }

  @Test
  void hidesListingsOnceSnapshotReachesMaxAge() {
    AuctionPageRenderer renderer = renderer();
    ProxyPayload<GrowtopiaAuction> payload =
        payload(
            List.of(listing("Expired listing", "Owner", "WORLD", 1, 3_600)), NOW.minus(MAX_AGE));

    AuctionPageRenderer.RenderedPage page = renderer.render(payload, null, "", 0);

    assertEquals(1, page.pageCount());
    assertEquals(0, page.pageIndex());
    assertTrue(pageText(page).contains("too old to display"));
    assertFalse(pageText(page).contains("Expired listing"));
    assertTrue(renderer.expired().getComponents().size() >= 3);
  }

  @Test
  void marksLivePayloadOutOfDateAfterTwoMinutes() {
    AuctionPageRenderer renderer = renderer();
    ProxyPayload<GrowtopiaAuction> payload =
        payload(
            List.of(listing("Dirt", "Owner", "WORLD", 1, 600)), NOW.minus(Duration.ofMinutes(2)));

    String text = pageText(renderer.render(payload, null, "", 0));

    assertTrue(text.contains("This snapshot is out of date."));
    assertTrue(text.contains("**Dirt**"));
  }

  @Test
  void formatsLargeSafeIntegerBidAndTimerWithoutOverflow() {
    AuctionPageRenderer renderer = renderer();
    long safeInteger = 9_007_199_254_740_991L;
    ProxyPayload<GrowtopiaAuction> payload =
        payload(List.of(listing("Huge", "Owner", "WORLD", safeInteger, safeInteger)), NOW);

    String text = pageText(renderer.render(payload, null, "", 0));

    assertTrue(text.contains("9,007,199,254,740,991 WL"));
    assertTrue(text.contains("Estimated end is too far away to display"));
    assertFalse(text.contains("<t:9007199254740991:R>"));
  }

  @Test
  void displaysTimestampAtSupportedYearBoundariesAndFallsBackBeyondThem() {
    Instant firstSupportedInstant = Instant.parse("0001-01-01T00:00:00Z");
    AuctionPageRenderer firstYearRenderer =
        new AuctionPageRenderer(Clock.fixed(firstSupportedInstant, ZoneOffset.UTC), MAX_AGE);
    String firstYearText =
        pageText(
            firstYearRenderer.render(
                payload(List.of(listing("First", "Owner", "WORLD", 1, 0)), firstSupportedInstant),
                null,
                "",
                0));

    Instant lastSupportedInstant = Instant.parse("9999-12-31T23:59:00Z");
    AuctionPageRenderer lastYearRenderer =
        new AuctionPageRenderer(Clock.fixed(lastSupportedInstant, ZoneOffset.UTC), MAX_AGE);
    String lastYearText =
        pageText(
            lastYearRenderer.render(
                payload(List.of(listing("Last", "Owner", "WORLD", 1, 59)), lastSupportedInstant),
                null,
                "",
                0));
    final String beyondLastYearText =
        pageText(
            lastYearRenderer.render(
                payload(List.of(listing("Beyond", "Owner", "WORLD", 1, 60)), lastSupportedInstant),
                null,
                "",
                0));

    assertTrue(firstYearText.contains("Snapshot: " + relative(firstSupportedInstant) + "."));
    assertTrue(firstYearText.contains("Estimated end: " + relative(firstSupportedInstant)));
    assertTrue(
        lastYearText.contains("Estimated end: " + relative(Instant.parse("9999-12-31T23:59:59Z"))));
    assertTrue(beyondLastYearText.contains("Estimated end is too far away to display"));
    assertFalse(beyondLastYearText.contains("<t:253402300800:R>"));
  }

  @Test
  void boundsAndEscapesHostileTextAndBuildsValidContainerWithControls() {
    AuctionPageRenderer renderer = renderer();
    String hostile = "@everyone **[click](https://example.invalid)**\n".repeat(60);
    ProxyPayload<GrowtopiaAuction> payload =
        payload(List.of(listing(hostile, hostile, hostile, 1, 60)), NOW);
    AuctionPageRenderer.RenderedPage page = renderer.render(payload, null, hostile, 0);
    String text = pageText(page);
    Container container = page.container(List.of(Button.secondary("auction-next", "Next")));

    assertNotNull(container);
    assertTrue(text.length() + 100 < 4_000);
    assertFalse(text.contains("@everyone"));
    assertTrue(text.contains("@​"));
    assertTrue(text.contains("\\*\\*"));
    assertTrue(container.getComponents().size() >= 4);
    AuctionListing longest =
        listing(hostile, hostile, hostile, 9_007_199_254_740_991L, 9_007_199_254_740_991L);
    AuctionPageRenderer.RenderedPage fullPage =
        renderer.render(
            payload(List.of(longest, longest, longest, longest, longest), NOW), null, "", 0);
    assertTrue(pageText(fullPage).length() + 100 < 4_000);
    assertNotNull(fullPage.container(List.of(Button.secondary("full-page", "Next"))));
  }

  @Test
  void handlesFetchedAtOutsideDisplayableTimestampRangeWithoutOverflow() {
    String text =
        pageText(
            renderer()
                .render(
                    payload(List.of(listing("Future", "Owner", "WORLD", 1, 90)), Instant.MAX),
                    null,
                    "",
                    0));

    assertTrue(text.contains("Snapshot: timestamp unavailable."));
    assertTrue(text.contains("**Future**"));
  }

  @Test
  void renderedPageDefensivelyCopiesItsComponents() {
    List<ContainerChildComponent> source = new ArrayList<>();
    source.add(TextDisplay.of("Stable"));

    AuctionPageRenderer.RenderedPage page = new AuctionPageRenderer.RenderedPage(source, 0, 1);
    source.add(TextDisplay.of("Later mutation"));

    assertEquals(1, page.components().size());
    assertThrows(UnsupportedOperationException.class, () -> page.components().clear());
  }

  private static AuctionPageRenderer renderer() {
    return new AuctionPageRenderer(CLOCK, MAX_AGE);
  }

  private static ProxyPayload<GrowtopiaAuction> payload(
      List<AuctionListing> listings, Instant fetchedAt) {
    return new ProxyPayload<>(fetchedAt, List.of(), new GrowtopiaAuction(listings), null);
  }

  private static AuctionListing listing(
      String name, String owner, String world, long bidWl, long remainingSeconds) {
    return new AuctionListing(name, owner, world, bidWl, remainingSeconds);
  }

  private static String pageText(AuctionPageRenderer.RenderedPage page) {
    return page.components().stream()
        .filter(TextDisplay.class::isInstance)
        .map(TextDisplay.class::cast)
        .map(TextDisplay::getContent)
        .reduce("", (left, right) -> left + "\n" + right);
  }

  private static int occurrences(String text, String substring) {
    int count = 0;
    int offset = 0;
    while ((offset = text.indexOf(substring, offset)) >= 0) {
      count++;
      offset += substring.length();
    }
    return count;
  }

  private static String relative(Instant instant) {
    return "<t:" + instant.getEpochSecond() + ":R>";
  }
}
