package dev.skullition.lockium.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.GrowtopiaDetail;
import dev.skullition.lockium.model.GrowtopiaLeaderboard;
import dev.skullition.lockium.model.LeaderboardEntry;
import dev.skullition.lockium.model.ProxyPayload;
import dev.skullition.lockium.properties.ProxyProperties;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.GrowtopiaDetailService.DetailSnapshot;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService.LeaderboardSnapshot;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.PlayerCountService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.service.WorldRenderService;
import dev.skullition.lockium.service.WorldRenderService.WorldRender;
import io.github.freya022.botcommands.api.modals.Modals;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests commands backed by the proxy snapshots and the world render service. */
class GtLiveDataCommandsTests {
  private static final Instant NOW = Instant.parse("2026-07-04T17:22:00Z");

  private GrowtopiaDetailService details;
  private GrowtopiaLeaderboardService leaderboards;
  private PlayerCountService playerCounts;
  private WorldRenderService worldRenders;
  private GtCommands commands;

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @BeforeEach
  void setUp() {
    details = mock(GrowtopiaDetailService.class);
    leaderboards = mock(GrowtopiaLeaderboardService.class);
    playerCounts = mock(PlayerCountService.class);
    worldRenders = mock(WorldRenderService.class);
    ProxyProperties proxyProperties =
        new ProxyProperties(
            "https://proxy.example/",
            Duration.ofMinutes(1),
            Duration.ofMinutes(4),
            Duration.ofHours(6),
            Duration.ofHours(12));
    commands =
        new GtCommands(
            mock(Modals.class),
            mock(WikiService.class),
            details,
            leaderboards,
            playerCounts,
            proxyProperties,
            mock(TreeFruitService.class),
            worldRenders,
            mock(RiddleService.class),
            mock(ItemEffectService.class),
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> 123_456);
  }

  @Test
  void snapshotsWorldAndDetailReplies() throws IOException {
    when(worldRenders.fetchWorldRender("start"))
        .thenReturn(
            Optional.of(
                new WorldRender(
                    "https://world.example/start.png", Instant.parse("2026-07-03T12:00:00Z"))));
    when(details.getSnapshot()).thenReturn(detailSnapshot());

    assertSnapshot("gt-world", event -> commands.onSlashWorld(event, "start"));
    assertSnapshot("gt-wotd", commands::onSlashWotd);
  }

  @Test
  void snapshotsStatsAndLeaderboardReplies() throws IOException {
    when(details.getSnapshot()).thenReturn(detailSnapshot());
    when(playerCounts.since(NOW.minus(Duration.ofHours(6)))).thenReturn(List.of());

    GrowtopiaLeaderboard leaderboard =
        new GrowtopiaLeaderboard(
            List.of(
                new LeaderboardEntry(1, "Player@One", 1_234_567, "gold_1"),
                new LeaderboardEntry(2, "RunnerUp", 900_000, "silver_2")),
            Map.of());
    ProxyPayload<GrowtopiaLeaderboard> payload =
        new ProxyPayload<>(NOW.minusSeconds(60), List.of(), leaderboard, null);
    when(leaderboards.getSnapshot())
        .thenReturn(new LeaderboardSnapshot(leaderboard, payload, NOW.minusSeconds(30)));

    assertSnapshot("gt-stats", commands::onSlashStats);
    assertSnapshot("gt-leaderboard", event -> commands.onSlashLeaderboard(event, null));
  }

  private static DetailSnapshot detailSnapshot() {
    GrowtopiaDetail detail = new GrowtopiaDetail(45_678, "https://world.example/growtopiawiki.png");
    ProxyPayload<GrowtopiaDetail> payload =
        new ProxyPayload<>(NOW.minusSeconds(60), List.of(), detail, null);
    return new DetailSnapshot(detail, payload, NOW.minusSeconds(30));
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
