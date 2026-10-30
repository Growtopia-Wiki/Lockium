package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.SpriteVariant;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Regression tests for item-backed slash commands. */
class GtItemCommandsTests {
  private WikiService wiki;
    private ItemEffectService effects;
  private GtCommands commands;

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @BeforeEach
  void setUp() {
    wiki = mock(WikiService.class);
      TreeFruitService fruits = mock(TreeFruitService.class);
    effects = mock(ItemEffectService.class);
    when(wiki.getItemDetail(CommandFixtures.DIRT_CATALOGUE))
        .thenReturn(CommandFixtures.dirtDetail());
    when(wiki.getItemDetail(CommandFixtures.COMET_DUST_CATALOGUE))
        .thenReturn(CommandFixtures.cometDustDetail());
    when(effects.getEffects(CommandFixtures.dirtDetail().item())).thenReturn(List.of());
    when(effects.getEffects(CommandFixtures.cometDustDetail().item())).thenReturn(List.of());
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
        "gt-sprite", event -> commands.onSlashSprite(event, CommandFixtures.DIRT_CATALOGUE, null));
    assertSnapshot(
        "gt-sprite-seed",
        event -> commands.onSlashSprite(event, CommandFixtures.DIRT_CATALOGUE, SpriteVariant.SEED));
    assertSnapshot(
        "gt-sprite-tree",
        event -> commands.onSlashSprite(event, CommandFixtures.DIRT_CATALOGUE, SpriteVariant.TREE));
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
  void fixedRecycleValueUsesAnExactFormattedTotal() throws IOException {
    ItemCatalogue item = new ItemCatalogue(701, 1400, 1401, "Mini Growtopian", "");
    when(wiki.getItemDetail(item))
        .thenReturn(CommandFixtures.raritylessDetail(1400, item.itemName()));
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashRecycle(harness.slashEvent(), item, 3);

    String reply = harness.snapshot();
    assertTrue(reply.contains("Gems per item: 4,000"));
    assertTrue(reply.contains("Total gems: `12,000`"));
    assertTrue(reply.contains("Rarity: None"));
    assertFalse(reply.contains("~"));
    SnapshotAssertions.assertMatches("gt-recycle-fixed", reply);
  }

  @Test
  void knownZeroRecycleValueIsSupported() throws IOException {
    ItemCatalogue item = new ItemCatalogue(1856, 3710, 3711, "Spectral Goggles", "");
    when(wiki.getItemDetail(item))
        .thenReturn(CommandFixtures.raritylessDetail(3710, item.itemName()));
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashRecycle(harness.slashEvent(), item, 100);

    String reply = harness.snapshot();
    assertTrue(reply.contains("Gems per item: 0"));
    assertTrue(reply.contains("Total gems: `0`"));
    SnapshotAssertions.assertMatches("gt-recycle-zero", reply);
  }

  @ParameterizedTest
  @CsvSource({"5208, Demon Megalodon", "0, Blank"})
  void rejectsRaritylessItemsWithNoReportedValue(int itemId, String name) {
    ItemCatalogue item = new ItemCatalogue(1, itemId, itemId + 1, name, "");
    when(wiki.getItemDetail(item)).thenReturn(CommandFixtures.raritylessDetail(itemId, name));
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashRecycle(harness.slashEvent(), item, 100);

    assertTrue(harness.snapshot().contains("The recycle value for this item is not known yet."));
  }

  @Test
  void itemHeaderUsesTheMainItemNameAndWikiPage() {
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashItem(harness.slashEvent(), CommandFixtures.DIRT_CATALOGUE);

    String snapshot = harness.snapshot();
    assertTrue(snapshot.contains("## [Dirt](https://growtopiawiki.com/w/Dirt)"));
    assertFalse(snapshot.contains("[Dirt Seed](https://growtopiawiki.com/w/Dirt_Seed)"));
  }

  @Test
  void itemHeaderLinksToCatalogueSeedPageWhenPresent() {
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashItem(harness.slashEvent(), CommandFixtures.COMET_DUST_CATALOGUE);

    String snapshot = harness.snapshot();
    assertTrue(snapshot.contains("## [Comet Dust](https://growtopiawiki.com/w/Starseed)"));
    assertFalse(snapshot.contains("https://growtopiawiki.com/w/Comet_Dust"));
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
