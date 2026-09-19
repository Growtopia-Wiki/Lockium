package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.RoleType;
import dev.skullition.lockium.properties.ProxyProperties;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.PlayerCountService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.RiddleService.Riddle;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.service.WorldRenderService;
import io.github.freya022.botcommands.api.modals.Modals;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Snapshot and validation tests for commands that do not require full item API fixtures. */
class GtCommandsBasicTests {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-04T17:22:00Z"), ZoneOffset.UTC);

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @Test
  void snapshotsDeterministicUtilityCommands() throws IOException {
    GtCommands commands = commands(mock(WikiService.class), mock(RiddleService.class));

    assertSnapshot("gt-role", event -> commands.onSlashRole(event, RoleType.FARMER));
    assertSnapshot("gt-events", commands::onSlashEvents);
    assertSnapshot("gt-telephone", commands::onSlashTelephone);
    assertSnapshot("gt-xp", event -> commands.onSlashXp(event, 10, 20));
    assertSnapshot("gt-startdate", event -> commands.onSlashStartDate(event, 1_000));
    assertSnapshot("gt-time", commands::onSlashTime);
  }

  @Test
  void snapshotsRiddleAndSearchResults() throws IOException {
    WikiService wiki = mock(WikiService.class);
    when(wiki.getNameIndex())
        .thenReturn(
            Map.of(
                "dirt", new ItemCatalogue(1, 2, 3, "Dirt", "Dirt Seed"),
                "display", new ItemCatalogue(2, 4, 5, "Display Block", "Display Block Seed")));
    RiddleService riddles = mock(RiddleService.class);
    when(riddles.search("earth")).thenReturn(List.of(new Riddle("Born from the earth", 2, 200)));
    GtCommands commands = commands(wiki, riddles);

    assertSnapshot("gt-riddle", event -> commands.onSlashRiddle(event, "earth"));
    assertSnapshot("gt-search", event -> commands.onSlashSearch(event, "di" + "r"));
  }

  @Test
  void rejectsInvalidUtilityInputsWithoutCallingServices() {
    GtCommands commands = commands(mock(WikiService.class), mock(RiddleService.class));

    DiscordEventHarness xp = new DiscordEventHarness();
    commands.onSlashXp(xp.slashEvent(), 20, 10);
    DiscordEventHarness search = new DiscordEventHarness();
    commands.onSlashSearch(search.slashEvent(), "ab");
    DiscordEventHarness world = new DiscordEventHarness();
    commands.onSlashWorld(world.slashEvent(), "bad-world!");

    assertTrue(xp.snapshot().contains("Invalid input"));
    assertTrue(search.snapshot().contains("at least 3 characters"));
    assertTrue(world.snapshot().contains("letters/numbers/underscores"));
  }

  private static GtCommands commands(WikiService wiki, RiddleService riddles) {
    return new GtCommands(
        mock(Modals.class),
        wiki,
        mock(GrowtopiaDetailService.class),
        mock(GrowtopiaLeaderboardService.class),
        mock(PlayerCountService.class),
        mock(ProxyProperties.class),
        mock(TreeFruitService.class),
        mock(WorldRenderService.class),
        riddles,
        mock(ItemEffectService.class),
        CLOCK,
        () -> 123_456);
  }

  private static void assertSnapshot(String name, CommandInvocation invocation) throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();
    invocation.invoke(harness.slashEvent());
    SnapshotAssertions.assertMatches(name, harness.snapshot());
  }

  @FunctionalInterface
  private interface CommandInvocation {
    void invoke(
        io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent event);
  }
}
