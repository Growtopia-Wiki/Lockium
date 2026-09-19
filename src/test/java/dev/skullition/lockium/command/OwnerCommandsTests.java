package dev.skullition.lockium.command;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.service.ChiService;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiCacheService;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.core.BotOwners;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.managers.Presence;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Side-effect and response tests for owner-only commands. */
class OwnerCommandsTests {
  private TreeFruitService fruits;
  private WikiCacheService cache;
  private ChiService chi;
  private RiddleService riddles;
  private ItemEffectService effects;
  private GrowtopiaDetailService details;
  private GrowtopiaLeaderboardService leaderboards;
  private BotOwners owners;
  private OwnerCommands commands;

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @BeforeEach
  void setUp() {
    fruits = mock(TreeFruitService.class);
    cache = mock(WikiCacheService.class);
    chi = mock(ChiService.class);
    riddles = mock(RiddleService.class);
    effects = mock(ItemEffectService.class);
    details = mock(GrowtopiaDetailService.class);
    leaderboards = mock(GrowtopiaLeaderboardService.class);
    owners = mock(BotOwners.class);
    commands =
        new OwnerCommands(fruits, cache, chi, riddles, effects, details, leaderboards, owners);
  }

  @Test
  void ownerCanUpdateActivity() throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    Presence presence = mock(Presence.class);
    doReturn(jda).when(event).getJDA();
    when(jda.getPresence()).thenReturn(presence);

    commands.onSlashActivity(event, "Watching tests");

    SnapshotAssertions.assertMatches("owner-activity", harness.snapshot());
    verify(presence).setActivity(Activity.customStatus("Watching tests"));
  }

  @Test
  void ownerCanListGuildsInNameOrder() throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    Guild zeta = guild("Zeta Server", 12);
    Guild alpha = guild("Alpha Server", 34);
    doReturn(jda).when(event).getJDA();
    when(jda.getGuilds()).thenReturn(List.of(zeta, alpha));

    commands.onSlashGuilds(event);

    SnapshotAssertions.assertMatches("owner-guilds", harness.snapshot());
  }

  @Test
  void guildListHandlesNoConnectedGuilds() {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    doReturn(jda).when(event).getJDA();
    when(jda.getGuilds()).thenReturn(List.of());

    commands.onSlashGuilds(event);

    org.junit.jupiter.api.Assertions.assertTrue(
        harness.snapshot().contains("not connected to any servers"));
  }

  @Test
  void guildListUsesAttachmentWhenTextExceedsMessageLimit() {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    List<Guild> guilds = new ArrayList<>();
    for (int index = 0; index < 40; index++) {
      guilds.add(guild("Server %02d %s".formatted(index, "x".repeat(80)), index));
    }
    doReturn(jda).when(event).getJDA();
    when(jda.getGuilds()).thenReturn(guilds);

    commands.onSlashGuilds(event);

    String reply = harness.snapshot();
    org.junit.jupiter.api.Assertions.assertAll(
        () -> org.junit.jupiter.api.Assertions.assertTrue(reply.contains("Servers (`40`)")),
        () ->
            org.junit.jupiter.api.Assertions.assertTrue(reply.contains("attachments=[guilds.txt]")),
        () -> org.junit.jupiter.api.Assertions.assertTrue(reply.contains("complete server list")));
  }

  @Test
  void ownerReloadRefreshesEveryExternalDataset() throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashReload(ownerEvent(harness));

    SnapshotAssertions.assertMatches("owner-reload", harness.snapshot());
    verify(cache).refreshCaches();
    verify(fruits).reload();
    verify(chi).reload();
    verify(riddles).reload();
    verify(effects).reload();
    verify(details).refresh();
    verify(leaderboards).refresh();
  }

  @Test
  void nonOwnerIsRejectedBeforeSideEffects() {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = identifiedEvent(harness);
    when(owners.isOwner(event.getUser())).thenReturn(false);

    commands.onSlashReload(event);
    commands.onSlashGuilds(event);

    org.junit.jupiter.api.Assertions.assertTrue(harness.snapshot().contains("Only bot owners"));
    verify(cache, never()).refreshCaches();
    verify(details, never()).refresh();
    verify(leaderboards, never()).refresh();
    verify(event, never()).getJDA();
  }

  private GuildSlashEvent ownerEvent(DiscordEventHarness harness) {
    GuildSlashEvent event = identifiedEvent(harness);
    when(owners.isOwner(event.getUser())).thenReturn(true);
    return event;
  }

  private static GuildSlashEvent identifiedEvent(DiscordEventHarness harness) {
    GuildSlashEvent event = harness.guildEvent();
    User user = mock(User.class);
    Guild guild = mock(Guild.class);
    when(user.getId()).thenReturn("123");
    when(guild.getId()).thenReturn("456");
    doReturn(user).when(event).getUser();
    doReturn(guild).when(event).getGuild();
    return event;
  }

  private static Guild guild(String name, int memberCount) {
    Guild guild = mock(Guild.class);
    when(guild.getName()).thenReturn(name);
    when(guild.getMemberCount()).thenReturn(memberCount);
    return guild;
  }
}
