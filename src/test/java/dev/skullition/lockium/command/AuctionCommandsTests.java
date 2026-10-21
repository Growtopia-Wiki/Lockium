package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.command.AuctionControlFactory.Controls;
import dev.skullition.lockium.model.AuctionListing;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import dev.skullition.lockium.properties.AuctionProperties;
import dev.skullition.lockium.service.GrowtopiaAuctionService;
import dev.skullition.lockium.service.GrowtopiaAuctionService.AuctionState;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.components.event.ButtonEvent;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Interaction tests for the snapshot-backed auction browser. */
class AuctionCommandsTests {
  private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");
  private static final long OWNER_ID = 12_345L;
  private static final long OTHER_USER_ID = 54_321L;

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @Test
  void snapshotsTheInitialPageAndItsNavigationBoundaries() throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    GrowtopiaAuctionService service = service(state(payload(clock.instant(), listings(6)), null));
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);

    SnapshotAssertions.assertMatches("auction-first-page", harness.snapshot());
    String snapshot = harness.snapshot();
    assertAll(
        () -> assertEquals(OWNER_ID, controls.latest().ownerId),
        () -> assertEquals(Duration.ofMinutes(10), controls.latest().lifetime),
        () -> assertTrue(buttonDisabled(snapshot, controls.latest().previousId)),
        () -> assertFalse(buttonDisabled(snapshot, controls.latest().nextId)),
        () -> assertTrue(snapshot.contains("allowedMentions=[]")),
        () -> assertTrue(snapshot.contains("ephemeral=false")));
  }

  @Test
  void nextAndPreviousStayInsideTheAvailablePages() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    GrowtopiaAuctionService service = service(state(payload(clock.instant(), listings(6)), null));
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);
    click(controls.latest().next, harness, OWNER_ID);

    String secondPage = harness.snapshot();
    assertAll(
        () -> assertTrue(secondPage.contains("Item 6")),
        () -> assertTrue(secondPage.contains("Page 2 of 2")),
        () -> assertFalse(buttonDisabled(secondPage, controls.latest().previousId)),
        () -> assertTrue(buttonDisabled(secondPage, controls.latest().nextId)),
        () -> assertTrue(secondPage.contains("allowedMentions=[]")));

    click(controls.latest().previous, harness, OWNER_ID);
    String firstPage = harness.snapshot();
    assertAll(
        () -> assertTrue(firstPage.contains("Item 1")),
        () -> assertFalse(firstPage.contains("Item 6")),
        () -> assertTrue(buttonDisabled(firstPage, controls.latest().previousId)),
        () -> assertFalse(buttonDisabled(firstPage, controls.latest().nextId)));
    verify(service, never()).refresh();
  }

  @Test
  void pagesUseTheFrozenSnapshotUntilRefresh() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    AtomicReference<AuctionState> current =
        new AtomicReference<>(state(payload(clock.instant(), listingsNamed("Old Item", 6)), null));
    GrowtopiaAuctionService service = service(current);
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);
    current.set(state(payload(clock.instant(), listingsNamed("New Item", 1)), null));
    click(controls.latest().next, harness, OWNER_ID);
    String frozenPage = harness.snapshot();
    assertTrue(frozenPage.contains("Old Item 6"));
    assertFalse(frozenPage.contains("New Item"));

    click(controls.latest().refresh, harness, OWNER_ID);
    String refreshed = harness.snapshot();
    assertAll(
        () -> assertTrue(refreshed.contains("New Item 1")),
        () -> assertTrue(refreshed.contains("Page 1 of 1")),
        () -> assertFalse(refreshed.contains("Old Item")));
    verify(service, never()).refresh();
  }

  @Test
  void unauthorizedAndStaleGenerationClicksAreRejectedEphemerally() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    GrowtopiaAuctionService service = service(state(payload(clock.instant(), listings(6)), null));
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);
    Consumer<ButtonEvent> firstPageNext = controls.latest().next;
    click(firstPageNext, harness, OTHER_USER_ID);
    String unauthorized = harness.snapshot();
    assertAll(
        () -> assertTrue(unauthorized.contains("ephemeral=true")),
        () -> assertTrue(unauthorized.contains("Only the person who opened")),
        () -> assertEquals(1, controls.generations.size()));

    click(firstPageNext, harness, OWNER_ID);
    assertTrue(harness.snapshot().contains("Page 2 of 2"));
    click(firstPageNext, harness, OWNER_ID);
    String stale = harness.snapshot();
    assertAll(
        () -> assertTrue(stale.contains("ephemeral=true")),
        () -> assertTrue(stale.contains("This page has changed")),
        () -> assertEquals(2, controls.generations.size()));
  }

  @Test
  void timerExpiryHidesListingsAndDisposesControls() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    GrowtopiaAuctionService service = service(state(payload(clock.instant(), listings(6)), null));
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);
    controls.latest().expired.run();

    String expired = harness.snapshot();
    assertAll(
        () -> assertTrue(expired.contains("Auction listings expired")),
        () -> assertFalse(expired.contains("Item 1")),
        () -> assertTrue(controls.latest().disposed),
        () -> assertTrue(expired.contains("allowedMentions=[]")));
  }

  @Test
  void oldTimerCannotExpireNewGeneration() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    GrowtopiaAuctionService service = service(state(payload(clock.instant(), listings(6)), null));
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);
    FakeControlGeneration old = controls.latest();
    click(old.next, harness, OWNER_ID);
    FakeControlGeneration current = controls.latest();
    String currentPage = harness.snapshot();
    old.expired.run();

    assertAll(
        () -> assertTrue(old.disposed),
        () -> assertFalse(current.disposed),
        () -> assertEquals(currentPage, harness.snapshot()),
        () -> assertTrue(harness.snapshot().contains("Item 6")));
  }

  @Test
  void snapshotAgeShortensTheViewLifetimeAndExpiresFrozenRows() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    ProxyPayload<GrowtopiaAuction> almostExpired =
        payload(NOW.minus(Duration.ofMinutes(9)), listings(6));
    GrowtopiaAuctionService service = service(state(almostExpired, null));
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);

    assertEquals(Duration.ofMinutes(1), controls.latest().lifetime);
    clock.advance(Duration.ofMinutes(1));
    click(controls.latest().next, harness, OWNER_ID);

    String expired = harness.snapshot();
    assertAll(
        () -> assertTrue(expired.contains("Auction listings expired")),
        () -> assertFalse(expired.contains("Item 1")),
        () -> assertTrue(controls.latest().disposed));
  }

  @Test
  void unavailableAndEmptyViewsCanRecoverThroughRefresh() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    AtomicReference<AuctionState> current = new AtomicReference<>(state(null, NOW));
    GrowtopiaAuctionService service = service(current);
    FakeControls controls = new FakeControls();
    AuctionCommands commands = commands(service, controls, clock);

    commands.onSlashAuction(ownerEvent(harness, OWNER_ID), null);
    assertTrue(harness.snapshot().contains("auction data is unavailable"));
    current.set(state(payload(clock.instant(), List.of()), null));
    click(controls.latest().refresh, harness, OWNER_ID);
    assertTrue(harness.snapshot().contains("This snapshot contains no auctions."));

    current.set(state(payload(clock.instant(), listingsNamed("Recovered Item", 1)), null));
    click(controls.latest().refresh, harness, OWNER_ID);
    assertTrue(harness.snapshot().contains("Recovered Item 1"));
    verify(service, never()).refresh();
  }

  @Test
  void rejectsFiltersOverOneHundredCharactersWithoutReadingAuctionState() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    GrowtopiaAuctionService service = service(state(payload(clock.instant(), listings(1)), null));
    FakeControls controls = new FakeControls();

    commands(service, controls, clock)
        .onSlashAuction(ownerEvent(harness, OWNER_ID), "x".repeat(101));

    assertTrue(harness.snapshot().contains("at most 100 characters"));
    verify(service, never()).getState();
    assertTrue(controls.generations.isEmpty());
  }

  @Test
  void escapesMentionsAndBoundsLongListingFields() {
    DiscordEventHarness harness = new DiscordEventHarness();
    MutableClock clock = new MutableClock(NOW);
    AuctionListing unsafe =
        new AuctionListing(
            "@everyone **" + "x".repeat(150),
            "@here " + "owner".repeat(20),
            "WORLD " + "y".repeat(80),
            1,
            30);
    GrowtopiaAuctionService service =
        service(state(payload(clock.instant(), List.of(unsafe)), null));
    FakeControls controls = new FakeControls();

    commands(service, controls, clock).onSlashAuction(ownerEvent(harness, OWNER_ID), null);

    String snapshot = harness.snapshot();
    assertAll(
        () -> assertTrue(snapshot.contains("@\u200B")),
        () -> assertFalse(snapshot.contains("@everyone")),
        () -> assertFalse(snapshot.contains("@here")),
        () -> assertTrue(snapshot.contains("…")),
        () -> assertTrue(snapshot.contains("allowedMentions=[]")));
  }

  private static AuctionCommands commands(
      GrowtopiaAuctionService service, FakeControls controls, MutableClock clock) {
    AuctionProperties properties =
        new AuctionProperties(
            true, Duration.ofSeconds(60), Duration.ofMinutes(10), Duration.ofMinutes(10));
    return new AuctionCommands(service, controls, properties, clock);
  }

  private static GlobalSlashEvent ownerEvent(DiscordEventHarness harness, long id) {
    GlobalSlashEvent event = harness.slashEvent();
    doReturn(user(id)).when(event).getUser();
    return event;
  }

  private static ButtonEvent buttonEvent(DiscordEventHarness harness, long id) {
    ButtonEvent event = harness.buttonEvent();
    doReturn(user(id)).when(event).getUser();
    return event;
  }

  private static User user(long id) {
    User user = mock(User.class);
    when(user.getIdLong()).thenReturn(id);
    return user;
  }

  private static void click(
      Consumer<ButtonEvent> callback, DiscordEventHarness harness, long invokingUserId) {
    callback.accept(buttonEvent(harness, invokingUserId));
  }

  private static boolean buttonDisabled(String snapshot, String id) {
    int start = snapshot.indexOf(id);
    if (start < 0) {
      throw new AssertionError("Button id not present in snapshot: " + id);
    }
    int end = snapshot.indexOf("}", start);
    return snapshot
        .substring(start, end < 0 ? snapshot.length() : end)
        .contains("\"disabled\" : true");
  }

  private static GrowtopiaAuctionService service(AuctionState state) {
    GrowtopiaAuctionService service = mock(GrowtopiaAuctionService.class);
    when(service.getState()).thenReturn(state);
    return service;
  }

  private static GrowtopiaAuctionService service(AtomicReference<AuctionState> state) {
    GrowtopiaAuctionService service = mock(GrowtopiaAuctionService.class);
    when(service.getState()).thenAnswer(invocation -> state.get());
    return service;
  }

  private static AuctionState state(ProxyPayload<GrowtopiaAuction> payload, Instant failedAt) {
    return new AuctionState(payload, failedAt);
  }

  private static ProxyPayload<GrowtopiaAuction> payload(
      Instant fetchedAt, List<AuctionListing> listings) {
    return new ProxyPayload<>(fetchedAt, List.of(), new GrowtopiaAuction(listings), null);
  }

  private static List<AuctionListing> listings(int count) {
    return listingsNamed("Item", count);
  }

  private static List<AuctionListing> listingsNamed(String prefix, int count) {
    List<AuctionListing> listings = new ArrayList<>();
    for (int index = 1; index <= count; index++) {
      listings.add(new AuctionListing(prefix + " " + index, "Owner " + index, "WORLD", index, 300));
    }
    return listings;
  }

  private static final class FakeControls implements AuctionControlFactory {
    private final List<FakeControlGeneration> generations = new ArrayList<>();

    @Override
    public Controls create(
        long ownerId,
        Duration lifetime,
        Consumer<ButtonEvent> previous,
        Consumer<ButtonEvent> next,
        Consumer<ButtonEvent> refresh,
        Runnable expired) {
      int number = generations.size() + 1;
      Button previousButton = Button.secondary("auction-previous-" + number, "Previous");
      Button nextButton = Button.secondary("auction-next-" + number, "Next");
      Button refreshButton = Button.primary("auction-refresh-" + number, "Refresh");
      FakeControlGeneration generation =
          new FakeControlGeneration(
              ownerId,
              lifetime,
              previous,
              next,
              refresh,
              expired,
              previousButton.getCustomId(),
              nextButton.getCustomId(),
              refreshButton.getCustomId());
      generations.add(generation);
      return new Controls(
          previousButton, nextButton, refreshButton, () -> generation.disposed = true);
    }

    private FakeControlGeneration latest() {
      return generations.get(generations.size() - 1);
    }
  }

  private static final class FakeControlGeneration {
    private final long ownerId;
    private final Duration lifetime;
    private final Consumer<ButtonEvent> previous;
    private final Consumer<ButtonEvent> next;
    private final Consumer<ButtonEvent> refresh;
    private final Runnable expired;
    private final String previousId;
    private final String nextId;
    private final String refreshId;
    private boolean disposed;

    private FakeControlGeneration(
        long ownerId,
        Duration lifetime,
        Consumer<ButtonEvent> previous,
        Consumer<ButtonEvent> next,
        Consumer<ButtonEvent> refresh,
        Runnable expired,
        String previousId,
        String nextId,
        String refreshId) {
      this.ownerId = ownerId;
      this.lifetime = lifetime;
      this.previous = previous;
      this.next = next;
      this.refresh = refresh;
      this.expired = expired;
      this.previousId = previousId;
      this.nextId = nextId;
      this.refreshId = refreshId;
    }
  }

  private static final class MutableClock extends Clock {
    private final AtomicReference<Instant> current;

    private MutableClock(Instant instant) {
      current = new AtomicReference<>(instant);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current.get();
    }

    private void advance(Duration duration) {
      current.updateAndGet(instant -> instant.plus(duration));
    }
  }
}
