package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext;
import io.github.freya022.botcommands.api.commands.application.CommandUpdateException;
import io.github.freya022.botcommands.api.commands.application.CommandUpdateResult;
import io.github.freya022.botcommands.api.commands.application.slash.SlashCommandInfo;
import io.github.freya022.botcommands.api.commands.application.slash.TopLevelSlashCommandInfo;
import io.github.freya022.botcommands.api.core.config.BApplicationConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.reflect.KFunction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.RestAction;
import org.junit.jupiter.api.Test;

/** Offline tests for safe, guild-scoped owner command registration recovery. */
class OwnerCommandRegistrationServiceTests {
  private static final Instant START = Instant.parse("2026-10-21T14:00:00Z");
  private static final long GUILD_ID = 123_456L;
  private static final long OWNER_COMMAND_ID = 987_654L;
  private static final List<String> OWNER_SUBCOMMANDS = List.of("activity", "guilds", "reload");

  @Test
  void normalFalseUpdateBindsCurrentRoutesWithoutForcing() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
    when(fixture.context().updateGuildApplicationCommands(fixture.guild(), false))
        .thenAnswer(
            invocation -> {
              stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID, GUILD_ID);
              return CompletableFuture.completedFuture(updateResult());
            });
    stubCommands(fixture.guild(), List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS)));

    fixture.service().reconcile(fixture.guild()).join();

    var interactions = inOrder(fixture.context(), fixture.guild());
    interactions.verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), false);
    interactions.verify(fixture.guild()).retrieveCommands(true);
    for (String leaf : OWNER_SUBCOMMANDS) {
      verify(fixture.context())
          .getApplicationCommandById(SlashCommandInfo.class, OWNER_COMMAND_ID, null, leaf);
    }
    verify(fixture.context(), never()).updateGuildApplicationCommands(fixture.guild(), true);
    verify(fixture.context(), never()).updateGlobalApplicationCommands(anyBoolean());
  }

  @Test
  void repairsMissingDiscordOwnerOrRequiredSubcommandOnce() {
    for (List<Command> initialCommands :
        List.of(
            List.<Command>of(),
            List.of(ownerCommand(OWNER_COMMAND_ID, List.of("activity", "guilds"))))) {
      RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
      stubUpdate(fixture.context(), fixture.guild(), false, updateResult());
      stubUpdate(fixture.context(), fixture.guild(), true, updateResult());
      stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID, GUILD_ID);
      stubResponses(
          fixture.guild(),
          List.of(
              CompletableFuture.completedFuture(initialCommands),
              CompletableFuture.completedFuture(
                  List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS)))));

      assertDoesNotThrow(() -> fixture.service().reconcile(fixture.guild()).join());

      verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), false);
      verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), true);
      verify(fixture.guild(), times(2)).retrieveCommands(true);
      verify(fixture.context(), never()).updateGlobalApplicationCommands(anyBoolean());
    }
  }

  @Test
  void repairsLocalPathCommandIdAndGuildScopeMismatches() {
    for (RouteMismatch mismatch : RouteMismatch.values()) {
      RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
      stubUpdate(fixture.context(), fixture.guild(), false, updateResult());
      stubUpdate(fixture.context(), fixture.guild(), true, updateResult());
      stubRoutesWithInitialMismatch(fixture.context(), mismatch);
      stubResponses(
          fixture.guild(),
          List.of(
              CompletableFuture.completedFuture(
                  List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS))),
              CompletableFuture.completedFuture(
                  List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS)))));

      fixture.service().reconcile(fixture.guild()).join();

      verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), false);
      verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), true);
      verify(fixture.guild(), times(2)).retrieveCommands(true);
    }
  }

  @Test
  void remainsNormallyCompletedWhenForcedAuditIsStillUnhealthy() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(fixture.context(), fixture.guild(), false, updateResult());
    stubUpdate(fixture.context(), fixture.guild(), true, updateResult());
    stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID, GUILD_ID);
    stubResponses(
        fixture.guild(),
        List.of(
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, List.of("activity")))),
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, List.of("activity"))))));

    assertDoesNotThrow(() -> fixture.service().reconcile(fixture.guild()).join());

    verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), false);
    verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), true);
    verify(fixture.guild(), times(2)).retrieveCommands(true);
  }

  @Test
  void declarationErrorsStopBeforeDiscordAuditAndCompleteNormally() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(
        fixture.context(), fixture.guild(), false, updateResult(List.of(declarationFailure())));

    assertDoesNotThrow(() -> fixture.service().reconcile(fixture.guild()).join());

    verify(fixture.guild(), never()).retrieveCommands(true);
    verify(fixture.context(), never()).updateGuildApplicationCommands(fixture.guild(), true);
    verify(fixture.context(), never()).updateGlobalApplicationCommands(anyBoolean());
  }

  @Test
  void forcedUpdateDeclarationErrorsCompleteNormallyWithoutSecondAudit() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(fixture.context(), fixture.guild(), false, updateResult());
    stubUpdate(
        fixture.context(), fixture.guild(), true, updateResult(List.of(declarationFailure())));
    stubCommands(fixture.guild(), List.of());

    assertDoesNotThrow(() -> fixture.service().reconcile(fixture.guild()).join());

    verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), false);
    verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), true);
    verify(fixture.guild()).retrieveCommands(true);
    verify(fixture.context(), never()).updateGlobalApplicationCommands(anyBoolean());
  }

  @Test
  void synchronousAndAsynchronousUpdateFailuresCompleteNormally() {
    RegistrationFixture synchronous = fixture(true, List.of(GUILD_ID), List.of());
    when(synchronous.context().updateGuildApplicationCommands(synchronous.guild(), false))
        .thenThrow(new IllegalStateException("declaration failed"));

    assertDoesNotThrow(() -> synchronous.service().reconcile(synchronous.guild()).join());

    RegistrationFixture asynchronous = fixture(true, List.of(GUILD_ID), List.of());
    when(asynchronous.context().updateGuildApplicationCommands(asynchronous.guild(), false))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("REST failed")));

    assertDoesNotThrow(() -> asynchronous.service().reconcile(asynchronous.guild()).join());
    verify(asynchronous.guild(), never()).retrieveCommands(true);

    RegistrationFixture synchronousRead = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(synchronousRead.context(), synchronousRead.guild(), false, updateResult());
    when(synchronousRead.guild().retrieveCommands(true))
        .thenThrow(new IllegalStateException("synchronous REST failure"));

    assertDoesNotThrow(() -> synchronousRead.service().reconcile(synchronousRead.guild()).join());
  }

  @Test
  void discordReadAndForcedReadFailuresCompleteNormally() {
    RegistrationFixture firstRead = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(firstRead.context(), firstRead.guild(), false, updateResult());
    stubResponses(
        firstRead.guild(),
        List.of(CompletableFuture.failedFuture(new IllegalStateException("Discord read failed"))));

    assertDoesNotThrow(() -> firstRead.service().reconcile(firstRead.guild()).join());

    RegistrationFixture forcedRead = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(forcedRead.context(), forcedRead.guild(), false, updateResult());
    stubUpdate(forcedRead.context(), forcedRead.guild(), true, updateResult());
    stubHealthyRoutes(forcedRead.context(), OWNER_COMMAND_ID, GUILD_ID);
    stubResponses(
        forcedRead.guild(),
        List.of(
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, List.of("activity")))),
            CompletableFuture.failedFuture(new IllegalStateException("forced read failed"))));

    assertDoesNotThrow(() -> forcedRead.service().reconcile(forcedRead.guild()).join());

    verify(forcedRead.context()).updateGuildApplicationCommands(forcedRead.guild(), true);
    verify(forcedRead.guild(), times(2)).retrieveCommands(true);
  }

  @Test
  void cooldownSkipsBeforeBoundaryAndAllowsAtExactBoundary() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
    stubUpdate(fixture.context(), fixture.guild(), false, updateResult());
    stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID, GUILD_ID);
    stubResponses(
        fixture.guild(),
        List.of(
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS))),
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS)))));

    fixture.service().reconcile(fixture.guild()).join();
    fixture.clock().advance(Duration.ofSeconds(59));
    fixture.service().reconcile(fixture.guild()).join();
    verify(fixture.context(), times(1)).updateGuildApplicationCommands(fixture.guild(), false);

    fixture.clock().advance(Duration.ofSeconds(1));
    fixture.service().reconcile(fixture.guild()).join();

    verify(fixture.context(), times(2)).updateGuildApplicationCommands(fixture.guild(), false);
    verify(fixture.context(), never()).updateGlobalApplicationCommands(anyBoolean());
  }

  @Test
  void sameGuildConcurrentAttemptIsSkippedAndOtherGuildCanProceed() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID, GUILD_ID + 1), List.of());
    Guild otherGuild = guild(GUILD_ID + 1);
    CompletableFuture<CommandUpdateResult> firstUpdate = new CompletableFuture<>();
    CompletableFuture<CommandUpdateResult> otherUpdate = new CompletableFuture<>();
    when(fixture.context().updateGuildApplicationCommands(fixture.guild(), false))
        .thenReturn(firstUpdate);
    when(fixture.context().updateGuildApplicationCommands(otherGuild, false))
        .thenReturn(otherUpdate);
    stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID, GUILD_ID);
    stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID + 1, GUILD_ID + 1);
    stubCommands(fixture.guild(), List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS)));
    stubCommands(otherGuild, List.of(ownerCommand(OWNER_COMMAND_ID + 1, OWNER_SUBCOMMANDS)));

    CompletableFuture<Void> first = fixture.service().reconcile(fixture.guild());
    CompletableFuture<Void> duplicate = fixture.service().reconcile(fixture.guild());
    CompletableFuture<Void> independent = fixture.service().reconcile(otherGuild);

    assertTrue(duplicate.isDone());
    assertFalse(first.isDone());
    assertFalse(independent.isDone());
    verify(fixture.context()).updateGuildApplicationCommands(fixture.guild(), false);
    verify(fixture.context()).updateGuildApplicationCommands(otherGuild, false);

    firstUpdate.complete(updateResult());
    otherUpdate.complete(updateResult());
    first.join();
    independent.join();
  }

  @Test
  void cancellingCallerDoesNotLockGuildAfterAttemptFinishes() {
    RegistrationFixture fixture = fixture(true, List.of(GUILD_ID), List.of());
    CompletableFuture<CommandUpdateResult> pendingUpdate = new CompletableFuture<>();
    CompletableFuture<CommandUpdateResult> nextUpdate =
        CompletableFuture.completedFuture(updateResult());
    when(fixture.context().updateGuildApplicationCommands(fixture.guild(), false))
        .thenReturn(pendingUpdate)
        .thenReturn(nextUpdate);
    stubHealthyRoutes(fixture.context(), OWNER_COMMAND_ID, GUILD_ID);
    stubResponses(
        fixture.guild(),
        List.of(
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS))),
            CompletableFuture.completedFuture(
                List.of(ownerCommand(OWNER_COMMAND_ID, OWNER_SUBCOMMANDS)))));

    CompletableFuture<Void> cancelled = fixture.service().reconcile(fixture.guild());
    assertTrue(cancelled.cancel(true));
    fixture.service().reconcile(fixture.guild()).join();
    verify(fixture.context(), times(1)).updateGuildApplicationCommands(fixture.guild(), false);

    pendingUpdate.complete(updateResult());
    fixture.clock().advance(Duration.ofMinutes(1));
    fixture.service().reconcile(fixture.guild()).join();

    verify(fixture.context(), times(2)).updateGuildApplicationCommands(fixture.guild(), false);
    verify(fixture.context(), never()).updateGuildApplicationCommands(fixture.guild(), true);
  }

  @Test
  void disabledOrOutOfScopeGuildsNeverUpdateCommands() {
    RegistrationFixture disabled = fixture(false, List.of(GUILD_ID), List.of());
    RegistrationFixture excluded = fixture(true, List.of(GUILD_ID + 1), List.of());
    RegistrationFixture excludedFromUpdateList =
        fixture(true, List.of(GUILD_ID), List.of(GUILD_ID + 1));

    assertFalse(disabled.service().isEligible(disabled.guild()));
    assertFalse(excluded.service().isEligible(excluded.guild()));
    assertFalse(excludedFromUpdateList.service().isEligible(excludedFromUpdateList.guild()));
    disabled.service().reconcile(disabled.guild()).join();
    excluded.service().reconcile(excluded.guild()).join();
    excludedFromUpdateList.service().reconcile(excludedFromUpdateList.guild()).join();

    verify(disabled.context(), never()).updateGuildApplicationCommands(disabled.guild(), false);
    verify(excluded.context(), never()).updateGuildApplicationCommands(excluded.guild(), false);
    verify(excludedFromUpdateList.context(), never())
        .updateGuildApplicationCommands(excludedFromUpdateList.guild(), false);
  }

  private static RegistrationFixture fixture(
      boolean enabled, List<Long> testGuildIds, List<Long> guildsToUpdate) {
    BApplicationConfig config = mock(BApplicationConfig.class);
    when(config.getEnable()).thenReturn(enabled);
    when(config.getTestGuildIds()).thenReturn(testGuildIds);
    when(config.getGuildsToUpdate()).thenReturn(guildsToUpdate);
    ApplicationCommandsContext context = mock(ApplicationCommandsContext.class);
    MutableClock clock = new MutableClock(START);
    return new RegistrationFixture(
        context,
        guild(GUILD_ID),
        clock,
        new OwnerCommandRegistrationService(context, config, clock));
  }

  private static Guild guild(long id) {
    Guild guild = mock(Guild.class);
    when(guild.getIdLong()).thenReturn(id);
    when(guild.getId()).thenReturn(Long.toString(id));
    return guild;
  }

  private static Command ownerCommand(long id, List<String> subcommands) {
    Command command = mock(Command.class);
    when(command.getType()).thenReturn(Command.Type.SLASH);
    when(command.getName()).thenReturn("owner");
    when(command.getIdLong()).thenReturn(id);
    List<Command.Subcommand> options =
        subcommands.stream().map(OwnerCommandRegistrationServiceTests::subcommand).toList();
    when(command.getSubcommands()).thenReturn(options);
    return command;
  }

  private static Command.Subcommand subcommand(String name) {
    Command.Subcommand subcommand = mock(Command.Subcommand.class);
    when(subcommand.getName()).thenReturn(name);
    return subcommand;
  }

  private static void stubHealthyRoutes(
      ApplicationCommandsContext context, long ownerId, long guildId) {
    for (String leaf : OWNER_SUBCOMMANDS) {
      SlashCommandInfo route = route(ownerId, guildId, "owner " + leaf);
      when(context.getApplicationCommandById(SlashCommandInfo.class, ownerId, null, leaf))
          .thenReturn(route);
    }
  }

  private static void stubRoutesWithInitialMismatch(
      ApplicationCommandsContext context, RouteMismatch mismatch) {
    for (String leaf : OWNER_SUBCOMMANDS) {
      SlashCommandInfo correct = route(OWNER_COMMAND_ID, GUILD_ID, "owner " + leaf);
      if (leaf.equals("activity")) {
        SlashCommandInfo incorrect =
            switch (mismatch) {
              case MISSING_ROUTE -> null;
              case PATH -> route(OWNER_COMMAND_ID, GUILD_ID, "not-owner activity");
              case COMMAND_ID -> route(OWNER_COMMAND_ID + 1, GUILD_ID, "owner activity");
              case GUILD_SCOPE -> route(OWNER_COMMAND_ID, GUILD_ID + 1, "owner activity");
            };
        when(context.getApplicationCommandById(
                SlashCommandInfo.class, OWNER_COMMAND_ID, null, leaf))
            .thenReturn(incorrect, correct);
      } else {
        when(context.getApplicationCommandById(
                SlashCommandInfo.class, OWNER_COMMAND_ID, null, leaf))
            .thenReturn(correct);
      }
    }
  }

  private static SlashCommandInfo route(long ownerId, long guildId, String path) {
    SlashCommandInfo route = mock(SlashCommandInfo.class);
    TopLevelSlashCommandInfo owner = mock(TopLevelSlashCommandInfo.class);
    when(route.getFullCommandName()).thenReturn(path);
    when(route.getTopLevelInstance()).thenReturn(owner);
    when(owner.getIdLong()).thenReturn(ownerId);
    when(owner.getGuildId()).thenReturn(guildId);
    return route;
  }

  private static void stubUpdate(
      ApplicationCommandsContext context, Guild guild, boolean force, CommandUpdateResult result) {
    when(context.updateGuildApplicationCommands(guild, force))
        .thenReturn(CompletableFuture.completedFuture(result));
  }

  private static CommandUpdateResult updateResult() {
    return updateResult(List.of());
  }

  private static CommandUpdateResult updateResult(List<CommandUpdateException> failures) {
    CommandUpdateResult result = mock(CommandUpdateResult.class);
    when(result.getUpdateExceptions()).thenReturn(failures);
    return result;
  }

  @SuppressWarnings("unchecked")
  private static CommandUpdateException declarationFailure() {
    CommandUpdateException failure = mock(CommandUpdateException.class);
    KFunction<?> declaration = mock(KFunction.class);
    when(declaration.getName()).thenReturn("ownerCommands");
    doReturn(declaration).when(failure).getFunction();
    when(failure.getThrowable()).thenReturn(new IllegalStateException("invalid declaration"));
    return failure;
  }

  private static void stubCommands(Guild guild, List<Command> commands) {
    stubResponses(guild, List.of(CompletableFuture.completedFuture(commands)));
  }

  private static void stubResponses(Guild guild, List<CompletableFuture<List<Command>>> responses) {
    @SuppressWarnings("unchecked")
    RestAction<List<Command>> action = mock(RestAction.class);
    AtomicInteger responseIndex = new AtomicInteger();
    when(action.submit()).thenAnswer(invocation -> responses.get(responseIndex.getAndIncrement()));
    when(guild.retrieveCommands(true)).thenReturn(action);
  }

  private record RegistrationFixture(
      ApplicationCommandsContext context,
      Guild guild,
      MutableClock clock,
      OwnerCommandRegistrationService service) {}

  private enum RouteMismatch {
    MISSING_ROUTE,
    PATH,
    COMMAND_ID,
    GUILD_SCOPE
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
      return now;
    }

    private void advance(Duration duration) {
      now = now.plus(duration);
    }
  }
}
