package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.service.ChiService;
import dev.skullition.lockium.service.GrowtopiaAuctionService;
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
import java.util.function.Consumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.RestAction;
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
  private GrowtopiaAuctionService auctions;
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
    auctions = mock(GrowtopiaAuctionService.class);
    owners = mock(BotOwners.class);
    commands =
        new OwnerCommands(
            fruits, cache, chi, riddles, effects, details, leaderboards, auctions, owners);
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

    assertTrue(harness.snapshot().contains("not connected to any servers"));
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
    assertAll(
        () -> assertTrue(reply.contains("Servers (`40`)")),
        () -> assertTrue(reply.contains("attachments=[guilds.txt]")),
        () -> assertTrue(reply.contains("complete server list")));
  }

  @Test
  void ownerReloadRefreshesEveryExternalDataset() throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);

    commands.onSlashReload(event);

    SnapshotAssertions.assertMatches("owner-reload", harness.snapshot());
    var interactions =
        inOrder(event, cache, fruits, chi, riddles, effects, details, leaderboards, auctions);
    interactions.verify(event).deferReply(true);
    interactions.verify(cache).refreshCaches();
    interactions.verify(fruits).reload();
    interactions.verify(chi).reload();
    interactions.verify(riddles).reload();
    interactions.verify(effects).reload();
    interactions.verify(details).refresh();
    interactions.verify(leaderboards).refresh();
    interactions.verify(auctions).refresh();
    interactions.verify(event).getHook();
  }

  @Test
  void ownerReloadReportsRefreshFailureAfterDeferring() {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    doThrow(new IllegalStateException("upstream unavailable")).when(cache).refreshCaches();

    commands.onSlashReload(event);

    String reply = harness.snapshot();
    assertAll(
        () -> assertTrue(reply.startsWith("delivery=edit-original")),
        () -> assertTrue(reply.contains("ephemeral=true")),
        () -> assertTrue(reply.contains("Cache reload failed")));
    verify(event).deferReply();
    verify(fruits, never()).reload();
    verify(leaderboards, never()).refresh();
    verify(auctions, never()).refresh();
  }

  @Test
  void nonOwnerIsRejectedBeforeSideEffects() {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = identifiedEvent(harness);
    when(owners.isOwner(event.getUser())).thenReturn(false);

    commands.onSlashReload(event);
    commands.onSlashGuilds(event);
    commands.onSlashLeave(event, "456");

    assertTrue(harness.snapshot().contains("Only bot owners"));
    verify(cache, never()).refreshCaches();
    verify(details, never()).refresh();
    verify(leaderboards, never()).refresh();
    verify(auctions, never()).refresh();
    verify(event, never()).getJDA();
  }

  @Test
  void ownerLeaveRejectsMalformedUnsignedSnowflakesBeforeLookingUpGuilds() {
    for (String serverId :
        List.of(
            "", "0", "+1", "-1", " 123", "12x", "18446744073709551616", "100000000000000000000")) {
      DiscordEventHarness harness = new DiscordEventHarness();
      GuildSlashEvent event = ownerEvent(harness);
      JDA jda = mock(JDA.class);
      doReturn(jda).when(event).getJDA();

      commands.onSlashLeave(event, serverId);

      verify(jda, never()).getGuildById(any(String.class));
      String reply = harness.snapshot();
      assertAll(
          () -> assertTrue(reply.contains("valid server ID")),
          () -> assertTrue(reply.contains("ephemeral=true")),
          () -> assertTrue(reply.contains("componentsV2=true")),
          () -> assertTrue(reply.contains("allowedMentions=[]")));
    }
  }

  @Test
  void ownerLeaveUsesCachedGuildAndRepliesWhenBotIsNotConnected() {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    doReturn(jda).when(event).getJDA();
    when(jda.getGuildById("404")).thenReturn(null);

    commands.onSlashLeave(event, "404");

    verify(jda).getGuildById("404");
    String reply = harness.snapshot();
    assertAll(
        () -> assertTrue(reply.contains("not connected to server `404`")),
        () -> assertTrue(reply.contains("ephemeral=true")),
        () -> assertTrue(reply.contains("componentsV2=true")),
        () -> assertTrue(reply.contains("allowedMentions=[]")));
  }

  @Test
  void ownerLeaveAcceptsTheLargestUnsignedSnowflake() {
    String serverId = "18446744073709551615";
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    Guild target = mock(Guild.class);
    doReturn(jda).when(event).getJDA();
    when(jda.getGuildById(serverId)).thenReturn(target);

    commands.onSlashLeave(event, serverId);

    verify(jda).getGuildById(serverId);
    verify(target, never()).leave();
  }

  @Test
  void ownerLeaveWaitsForAcknowledgementThenReportsLeaveSuccess() {
    LeaveScenario scenario = ownerLeaveScenario("456", false);

    commands.onSlashLeave(scenario.event(), "456");

    verify(scenario.jda()).getGuildById("456");
    verify(scenario.target(), never()).leave();
    verify(scenario.leaveAction(), never()).queue(any(), any());

    scenario.harness().completeReply();

    verify(scenario.target()).leave();
    verify(scenario.leaveAction()).queue(any(), any());
    scenario.callbacks().succeed();

    String reply = scenario.harness().snapshot();
    assertAll(
        () -> assertTrue(reply.contains("The bot has left server `456`")),
        () -> assertTrue(reply.contains("delivery=edit-original")),
        () -> assertTrue(reply.contains("ephemeral=true")),
        () -> assertTrue(reply.contains("componentsV2=true")),
        () -> assertTrue(reply.contains("allowedMentions=[]")));
  }

  @Test
  void ownerLeaveReportsAsynchronousLeaveFailure() {
    LeaveScenario scenario = ownerLeaveScenario("456", false);
    commands.onSlashLeave(scenario.event(), "456");

    scenario.harness().completeReply();
    scenario.callbacks().fail(new IllegalStateException("Discord rejected leave"));

    String reply = scenario.harness().snapshot();
    assertAll(
        () -> assertTrue(reply.contains("Could not leave server `456`")),
        () -> assertTrue(reply.contains("ephemeral=true")),
        () -> assertTrue(reply.contains("componentsV2=true")),
        () -> assertTrue(reply.contains("allowedMentions=[]")));
  }

  @Test
  void synchronousLeaveFailureEditsPrivateComponentsV2Reply() {
    LeaveScenario scenario = ownerLeaveScenario("456", false);
    when(scenario.target().leave())
        .thenThrow(new IllegalStateException("leave failed synchronously"));

    commands.onSlashLeave(scenario.event(), "456");
    scenario.harness().completeReply();

    verify(scenario.target()).leave();
    verify(scenario.leaveAction(), never()).queue(any(), any());
    String reply = scenario.harness().snapshot();
    assertAll(
        () -> assertTrue(reply.contains("Could not leave server `456`")),
        () -> assertTrue(reply.contains("delivery=edit-original")),
        () -> assertTrue(reply.contains("ephemeral=true")),
        () -> assertTrue(reply.contains("componentsV2=true")),
        () -> assertTrue(reply.contains("allowedMentions=[]")));
  }

  @Test
  void failedDeferAcknowledgementNeverLeavesGuild() {
    LeaveScenario scenario = ownerLeaveScenario("456", false);
    commands.onSlashLeave(scenario.event(), "456");

    scenario.harness().failReply(new IllegalStateException("Discord acknowledgement failed"));

    verify(scenario.target(), never()).leave();
    verify(scenario.leaveAction(), never()).queue(any(), any());
  }

  @Test
  void ownerCanLeaveTheGuildWhereTheCommandWasInvoked() {
    LeaveScenario scenario = ownerLeaveScenario("456", true);
    commands.onSlashLeave(scenario.event(), "456");

    scenario.harness().completeReply();
    verify(scenario.target()).leave();
    scenario.callbacks().succeed();

    assertTrue(scenario.harness().snapshot().contains("The bot has left server `456`"));
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

  private LeaveScenario ownerLeaveScenario(String serverId, boolean currentGuild) {
    DiscordEventHarness harness = new DiscordEventHarness();
    GuildSlashEvent event = ownerEvent(harness);
    JDA jda = mock(JDA.class);
    Guild target = currentGuild ? event.getGuild() : mock(Guild.class);
    @SuppressWarnings("unchecked")
    RestAction<Void> leaveAction = mock(RestAction.class);
    LeaveCallbacks callbacks = new LeaveCallbacks();
    doReturn(jda).when(event).getJDA();
    when(jda.getGuildById(serverId)).thenReturn(target);
    when(target.leave()).thenReturn(leaveAction);
    doAnswer(
            invocation -> {
              callbacks.success = invocation.getArgument(0);
              callbacks.failure = invocation.getArgument(1);
              return null;
            })
        .when(leaveAction)
        .queue(any(), any());
    return new LeaveScenario(harness, event, jda, target, leaveAction, callbacks);
  }

  private record LeaveScenario(
      DiscordEventHarness harness,
      GuildSlashEvent event,
      JDA jda,
      Guild target,
      RestAction<Void> leaveAction,
      LeaveCallbacks callbacks) {}

  private static final class LeaveCallbacks {
    private Consumer<? super Void> success;
    private Consumer<? super Throwable> failure;

    void succeed() {
      success.accept(null);
    }

    void fail(Throwable error) {
      failure.accept(error);
    }
  }
}
