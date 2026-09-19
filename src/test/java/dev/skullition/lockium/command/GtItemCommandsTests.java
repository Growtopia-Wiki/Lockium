package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.properties.ProxyProperties;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.PlayerCountService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.service.WorldRenderService;
import io.github.freya022.botcommands.api.modals.Modals;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regression tests for item-backed slash commands. */
class GtItemCommandsTests {
  private WikiService wiki;
  private TreeFruitService fruits;
  private ItemEffectService effects;
  private GtCommands commands;

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @BeforeEach
  void setUp() {
    wiki = mock(WikiService.class);
    fruits = mock(TreeFruitService.class);
    effects = mock(ItemEffectService.class);
    when(wiki.getItemDetail(CommandFixtures.DIRT_CATALOGUE))
        .thenReturn(CommandFixtures.dirtDetail());
    when(effects.getEffects(CommandFixtures.dirtDetail().item())).thenReturn(List.of());
    when(fruits.getMaxDrop(2)).thenReturn(8);
    commands =
        new GtCommands(
            mock(Modals.class),
            wiki,
            mock(GrowtopiaDetailService.class),
            mock(GrowtopiaLeaderboardService.class),
            mock(PlayerCountService.class),
            mock(ProxyProperties.class),
            fruits,
            mock(WorldRenderService.class),
            mock(RiddleService.class),
            effects,
            Clock.fixed(Instant.parse("2026-07-04T17:22:00Z"), ZoneOffset.UTC),
            () -> 123_456);
  }

  @Test
  void snapshotsItemBackedReplies() throws IOException {
    assertSnapshot("gt-item", event -> commands.onSlashItem(event, CommandFixtures.DIRT_CATALOGUE));
    assertSnapshot(
        "gt-sprite", event -> commands.onSlashSprite(event, CommandFixtures.DIRT_CATALOGUE));
    assertSnapshot(
        "gt-harvest",
        event -> commands.onSlashHarvest(event, CommandFixtures.DIRT_CATALOGUE, 1_000));
    assertSnapshot(
        "gt-recycle",
        event -> commands.onSlashRecycle(event, CommandFixtures.DIRT_CATALOGUE, 1_000));
    assertSnapshot(
        "gt-mooncakes",
        event -> commands.onSlashMooncakes(event, CommandFixtures.DIRT_CATALOGUE, 1_000));
  }

  @Test
  void rejectsInvalidCountsBeforeProducingCalculations() {
    DiscordEventHarness harvest = new DiscordEventHarness();
    commands.onSlashHarvest(harvest.slashEvent(), CommandFixtures.DIRT_CATALOGUE, 9);
    DiscordEventHarness recycle = new DiscordEventHarness();
    commands.onSlashRecycle(recycle.slashEvent(), CommandFixtures.DIRT_CATALOGUE, 0);
    DiscordEventHarness mooncakes = new DiscordEventHarness();
    commands.onSlashMooncakes(mooncakes.slashEvent(), CommandFixtures.DIRT_CATALOGUE, 500_001);

    assertTrue(harvest.snapshot().contains("at least 10"));
    assertTrue(recycle.snapshot().contains("between 1 and 100,000"));
    assertTrue(mooncakes.snapshot().contains("between 1 and 500,000"));
  }

  @Test
  void defersItemReplyWhenEffectsNeedScraping() {
    when(effects.requiresScrape(CommandFixtures.dirtDetail().item())).thenReturn(true);
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashItem(harness.slashEvent(), CommandFixtures.DIRT_CATALOGUE);

    assertTrue(harness.snapshot().contains("delivery=followup"));
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
