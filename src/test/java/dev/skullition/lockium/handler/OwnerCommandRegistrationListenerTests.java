package dev.skullition.lockium.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.service.OwnerCommandRegistrationService;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext;
import io.github.freya022.botcommands.api.commands.application.slash.SlashCommandInfo;
import io.github.freya022.botcommands.api.core.BotOwners;
import io.github.freya022.botcommands.api.core.annotations.BEventListener;
import io.github.freya022.botcommands.api.core.config.BApplicationConfig;
import io.github.freya022.botcommands.api.core.config.application.cache.ApplicationCommandsCacheConfig;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Event-routing tests for owner-command registration recovery. */
class OwnerCommandRegistrationListenerTests {
  @Test
  void guildReadyReconcilesTheGuildThatBecameReady() {
    OwnerCommandRegistrationService registration = mock(OwnerCommandRegistrationService.class);
    Guild guild = mock(Guild.class);
    GuildReadyEvent event = mock(GuildReadyEvent.class);
    when(event.getGuild()).thenReturn(guild);
    OwnerCommandRegistrationListener listener =
        listener(
            registration,
            mock(ApplicationCommandsContext.class),
            mock(BotOwners.class),
            mock(BApplicationConfig.class));

    listener.onGuildReady(event);

    verify(registration).reconcile(guild);
    verifyNoMoreInteractions(registration);
  }

  @Test
  void readyReconcilesConfiguredGuildsAlreadyConnectedToTheBot() {
    OwnerCommandRegistrationService registration = mock(OwnerCommandRegistrationService.class);
    BApplicationConfig config = mock(BApplicationConfig.class);
    ApplicationCommandsCacheConfig cache = mock(ApplicationCommandsCacheConfig.class);
    when(config.getEnable()).thenReturn(true);
    when(config.getTestGuildIds()).thenReturn(List.of(101L, 202L));
    when(config.getGuildsToUpdate()).thenReturn(List.of(303L));
    when(config.getCache()).thenReturn(cache);
    when(cache.getCheckOnline()).thenReturn(true);

    Guild configuredGuild = mock(Guild.class);
    ReadyEvent event = readyEvent(Map.of(101L, configuredGuild));
    OwnerCommandRegistrationListener listener =
        listener(
            registration, mock(ApplicationCommandsContext.class), mock(BotOwners.class), config);

    listener.onReady(event);

    verify(event.getJDA()).getGuildById(101L);
    verify(event.getJDA()).getGuildById(202L);
    verify(cache).getCheckOnline();
    verify(registration).reconcile(configuredGuild);
    verifyNoMoreInteractions(registration);
  }

  @Test
  void readySkipsConfiguredGuildLookupWhenApplicationCommandsAreDisabled() {
    OwnerCommandRegistrationService registration = mock(OwnerCommandRegistrationService.class);
    BApplicationConfig config = mock(BApplicationConfig.class);
    when(config.getEnable()).thenReturn(false);
    when(config.getTestGuildIds()).thenReturn(List.of(101L));
    ReadyEvent event = readyEvent(Map.of());
    OwnerCommandRegistrationListener listener =
        listener(
            registration, mock(ApplicationCommandsContext.class), mock(BotOwners.class), config);

    listener.onReady(event);

    verify(event.getJDA(), never()).getGuildById(101L);
    verifyNoMoreInteractions(registration);
  }

  @Test
  void ignoresNonOwnerNamesDirectMessagesNonOwnersAndIneligibleGuilds() {
    OwnerCommandRegistrationService registration = mock(OwnerCommandRegistrationService.class);
    ApplicationCommandsContext commands = mock(ApplicationCommandsContext.class);
    BotOwners owners = mock(BotOwners.class);
    OwnerCommandRegistrationListener listener =
        listener(registration, commands, owners, mock(BApplicationConfig.class));
    Guild guild = mock(Guild.class);
    User user = mock(User.class);

    SlashCommandInteractionEvent unrelated = slashEvent("gt", guild, user, 1L, null, "item");
    listener.onSlashCommand(unrelated);
    assertNoAcknowledgement(unrelated);

    SlashCommandInteractionEvent directMessage =
        slashEvent("owner", null, user, 2L, null, "reload");
    listener.onSlashCommand(directMessage);
    assertNoAcknowledgement(directMessage);

    SlashCommandInteractionEvent nonOwner = slashEvent("owner", guild, user, 3L, null, "reload");
    when(owners.isOwner(user)).thenReturn(false);
    listener.onSlashCommand(nonOwner);
    assertNoAcknowledgement(nonOwner);

    SlashCommandInteractionEvent ineligibleGuild =
        slashEvent("owner", guild, user, 4L, null, "reload");
    when(owners.isOwner(user)).thenReturn(true);
    when(registration.isEligible(guild)).thenReturn(false);
    listener.onSlashCommand(ineligibleGuild);
    assertNoAcknowledgement(ineligibleGuild);

    verify(owners, times(2)).isOwner(user);
    verify(registration).isEligible(guild);
    verify(registration, never()).reconcile(guild);
    verify(commands).getApplicationCommandById(SlashCommandInfo.class, 4L, null, "reload");
    verifyNoMoreInteractions(commands);
  }

  @Test
  void resolvedOwnerCommandIsIgnoredEvenWhenGuildIsIneligible() {
    OwnerCommandRegistrationService registration = mock(OwnerCommandRegistrationService.class);
    ApplicationCommandsContext commands = mock(ApplicationCommandsContext.class);
    BotOwners owners = mock(BotOwners.class);
    Guild guild = mock(Guild.class);
    User user = mock(User.class);
    SlashCommandInteractionEvent event =
        slashEvent("owner", guild, user, 5_010L, "admin", "reload");
    when(owners.isOwner(user)).thenReturn(true);
    when(commands.getApplicationCommandById(SlashCommandInfo.class, 5_010L, "admin", "reload"))
        .thenReturn(mock(SlashCommandInfo.class));
    OwnerCommandRegistrationListener listener =
        listener(registration, commands, owners, mock(BApplicationConfig.class));

    listener.onSlashCommand(event);

    verify(commands).getApplicationCommandById(SlashCommandInfo.class, 5_010L, "admin", "reload");
    verify(registration, never()).isEligible(guild);
    verify(registration, never()).reconcile(guild);
    assertNoAcknowledgement(event);
  }

  @Test
  void missingOwnerCommandRouteReconcilesByExactDiscordIdAndPathWithoutAcknowledging() {
    OwnerCommandRegistrationService registration = mock(OwnerCommandRegistrationService.class);
    ApplicationCommandsContext commands = mock(ApplicationCommandsContext.class);
    BotOwners owners = mock(BotOwners.class);
    Guild guild = mock(Guild.class);
    User user = mock(User.class);
    SlashCommandInteractionEvent event =
        slashEvent("owner", guild, user, 5_011L, "admin", "reload");
    when(owners.isOwner(user)).thenReturn(true);
    when(registration.isEligible(guild)).thenReturn(true);
    OwnerCommandRegistrationListener listener =
        listener(registration, commands, owners, mock(BApplicationConfig.class));

    listener.onSlashCommand(event);

    verify(commands).getApplicationCommandById(SlashCommandInfo.class, 5_011L, "admin", "reload");
    verify(registration).reconcile(guild);
    assertNoAcknowledgement(event);
  }

  @Test
  void allObserversRunAsynchronously() throws ReflectiveOperationException {
    assertAsync("onGuildReady", GuildReadyEvent.class);
    assertAsync("onReady", ReadyEvent.class);
    assertAsync("onSlashCommand", SlashCommandInteractionEvent.class);
  }

  private static OwnerCommandRegistrationListener listener(
      OwnerCommandRegistrationService registration,
      ApplicationCommandsContext commands,
      BotOwners owners,
      BApplicationConfig config) {
    return new OwnerCommandRegistrationListener(registration, commands, owners, config);
  }

  private static ReadyEvent readyEvent(Map<Long, Guild> guilds) {
    JDA jda = mock(JDA.class);
    SelfUser selfUser = mock(SelfUser.class);
    when(jda.getSelfUser()).thenReturn(selfUser);
    when(selfUser.getApplicationIdLong()).thenReturn(9001L);
    when(jda.getGuildById(101L)).thenReturn(guilds.get(101L));
    when(jda.getGuildById(202L)).thenReturn(guilds.get(202L));
    ReadyEvent event = mock(ReadyEvent.class);
    when(event.getJDA()).thenReturn(jda);
    return event;
  }

  private static SlashCommandInteractionEvent slashEvent(
      String name,
      @Nullable Guild guild,
      User user,
      long commandId,
      @Nullable String group,
      @Nullable String subcommand) {
    SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
    JDA jda = mock(JDA.class);
    SelfUser selfUser = mock(SelfUser.class);
    when(jda.getSelfUser()).thenReturn(selfUser);
    when(selfUser.getApplicationIdLong()).thenReturn(9001L);
    when(event.getJDA()).thenReturn(jda);
    when(event.getName()).thenReturn(name);
    when(event.getGuild()).thenReturn(guild);
    when(event.getUser()).thenReturn(user);
    when(event.getCommandIdLong()).thenReturn(commandId);
    when(event.getSubcommandGroup()).thenReturn(group);
    when(event.getSubcommandName()).thenReturn(subcommand);
    when(event.getFullCommandName()).thenReturn("owner %s %s".formatted(group, subcommand));
    return event;
  }

  private static void assertNoAcknowledgement(SlashCommandInteractionEvent event) {
    Set<String> acknowledgementMethods =
        Set.of("reply", "replyComponents", "deferReply", "deferEdit", "replyModal");
    Set<String> invokedAcknowledgements =
        Mockito.mockingDetails(event).getInvocations().stream()
            .map(invocation -> invocation.getMethod().getName())
            .filter(acknowledgementMethods::contains)
            .collect(Collectors.toSet());
    assertTrue(invokedAcknowledgements.isEmpty(), invokedAcknowledgements.toString());
  }

  private static void assertAsync(String methodName, Class<?> eventType)
      throws ReflectiveOperationException {
    Method method = OwnerCommandRegistrationListener.class.getMethod(methodName, eventType);
    BEventListener listener = method.getAnnotation(BEventListener.class);
    assertEquals(BEventListener.RunMode.ASYNC, listener.mode());
    assertFalse(listener.ignoreIntents());
  }
}
