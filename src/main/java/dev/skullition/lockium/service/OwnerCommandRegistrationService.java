package dev.skullition.lockium.service;

import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext;
import io.github.freya022.botcommands.api.commands.application.CommandUpdateException;
import io.github.freya022.botcommands.api.commands.application.CommandUpdateResult;
import io.github.freya022.botcommands.api.commands.application.TopLevelApplicationCommandInfo;
import io.github.freya022.botcommands.api.commands.application.slash.SlashCommandInfo;
import io.github.freya022.botcommands.api.core.config.BApplicationConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Repairs stale guild-scoped owner command registrations through BotCommands' public API.
 *
 * <p>BotCommands 3.4 readiness checks can suppress its usual missing-command recovery, so this
 * service audits the actual guild routes and reconciles them independently of those flags.
 */
@Service
public class OwnerCommandRegistrationService {
  private static final Logger logger =
      LoggerFactory.getLogger(OwnerCommandRegistrationService.class);
  private static final Duration COOLDOWN = Duration.ofMinutes(1);
  private static final List<String> OWNER_SUBCOMMANDS = List.of("activity", "guilds", "reload");

  private final ApplicationCommandsContext applicationCommands;
  private final BApplicationConfig config;
  private final Clock clock;
  private final ConcurrentMap<Long, AttemptGuard> attempts = new ConcurrentHashMap<>();
  private final Set<Long> blockedGuildWarnings = ConcurrentHashMap.newKeySet();

  /** Creates the registration recovery service with the system UTC clock. */
  @Autowired
  public OwnerCommandRegistrationService(
      ApplicationCommandsContext applicationCommands, BApplicationConfig config) {
    this(applicationCommands, config, Clock.systemUTC());
  }

  /** Creates the service with an explicit clock for deterministic cooldown tests. */
  OwnerCommandRegistrationService(
      ApplicationCommandsContext applicationCommands, BApplicationConfig config, Clock clock) {
    this.applicationCommands = applicationCommands;
    this.config = config;
    this.clock = clock;
  }

  /**
   * Whether owner command registration may be reconciled for this guild without widening scope.
   *
   * <p>Owner routes are annotated with {@code @Test} and have no per-annotation guild IDs, so the
   * configured test guild list is their allowlist. A non-empty {@code guildsToUpdate} adds a second
   * allowlist.
   *
   * @param guild guild whose commands may be reconciled
   * @return whether application commands are enabled and the guild is in every configured scope
   */
  public boolean isEligible(Guild guild) {
    if (!config.getEnable()) {
      return false;
    }

    long guildId = guild.getIdLong();
    List<Long> testGuildIds = config.getTestGuildIds();
    if (!testGuildIds.contains(guildId)) {
      return false;
    }

    List<Long> guildsToUpdate = config.getGuildsToUpdate();
    if (!guildsToUpdate.isEmpty() && !guildsToUpdate.contains(guildId)) {
      if (blockedGuildWarnings.add(guildId)) {
        logger.warn(
            "Skipped owner command registration recovery for configured test guild {}: "
                + "guild is not in botcommands.application.guildsToUpdate",
            guildId);
      }
      return false;
    }

    blockedGuildWarnings.remove(guildId);
    return true;
  }

  /**
   * Reconciles this eligible guild's owner routes once, then forces one update only if its audit
   * finds a Discord/local registration mismatch.
   *
   * <p>Concurrent and recently attempted reconciliations for the same guild are skipped. Expected
   * Discord and declaration failures are logged and converted to a normally completed future.
   *
   * @param guild guild whose owner routes are being checked
   * @return a future that always completes normally after the reconciliation attempt
   */
  public CompletableFuture<Void> reconcile(Guild guild) {
    if (!isEligible(guild)) {
      return CompletableFuture.completedFuture(null);
    }

    AttemptGuard guard = attempts.computeIfAbsent(guild.getIdLong(), ignored -> new AttemptGuard());
    Instant attemptStartedAt = clock.instant();
    if (!guard.tryStart(attemptStartedAt)) {
      logger.debug(
          "Skipped owner command registration recovery for guild {}: in flight or cooling down",
          guild.getId());
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> attempt;
    try {
      attempt = Objects.requireNonNull(updateAndAudit(guild));
    } catch (RuntimeException failure) {
      logFailure("guild command update", guild, failure);
      guard.finish();
      return CompletableFuture.completedFuture(null);
    }

    return attempt
        .whenComplete((ignored, failure) -> guard.finish())
        .exceptionally(
            failure -> {
              logFailure("guild registration audit", guild, failure);
              return null;
            });
  }

  private CompletableFuture<Void> updateAndAudit(Guild guild) {
    return updateGuildCommands(guild, false)
        .thenCompose(
            succeeded ->
                succeeded ? auditAndRepair(guild, false) : CompletableFuture.completedFuture(null));
  }

  private CompletableFuture<Boolean> updateGuildCommands(Guild guild, boolean force) {
    return applicationCommands
        .updateGuildApplicationCommands(guild, force)
        .thenApply(
            result -> {
              CommandUpdateResult updateResult = Objects.requireNonNull(result);
              logger.info(
                  "Owner command registration update for guild {} (force={}): {}",
                  guild.getId(),
                  force,
                  updateResult);
              if (!updateResult.getUpdateExceptions().isEmpty()) {
                for (CommandUpdateException failure : updateResult.getUpdateExceptions()) {
                  Throwable cause = failure.getThrowable();
                  logger.warn(
                      "Owner command declaration failed for guild {}: declaration={}, failure={}",
                      guild.getId(),
                      failure.getFunction().getName(),
                      cause.toString(),
                      cause);
                }
                return false;
              }
              return true;
            });
  }

  private CompletableFuture<Void> auditAndRepair(Guild guild, boolean forcedAlready) {
    return guild
        .retrieveCommands(true)
        .submit()
        .thenCompose(
            discordCommands -> {
              Audit audit = auditGuild(guild, discordCommands);
              if (audit.healthy()) {
                logger.info(
                    "Verified owner command routes for guild {}: discordOwnerId={}, routes={}",
                    guild.getId(),
                    audit.discordOwnerId(),
                    OWNER_SUBCOMMANDS);
                return CompletableFuture.completedFuture(null);
              }

              logger.warn(
                  "Owner command registration mismatch for guild {}: "
                      + "discordOwnerId={}, problems={}",
                  guild.getId(),
                  audit.discordOwnerId() == 0 ? "missing" : audit.discordOwnerId(),
                  audit.problems());
              if (forcedAlready) {
                logger.error(
                    "Owner command registration remains unhealthy after forced update "
                        + "for guild {}: discordOwnerId={}, problems={}",
                    guild.getId(),
                    audit.discordOwnerId() == 0 ? "missing" : audit.discordOwnerId(),
                    audit.problems());
                return CompletableFuture.completedFuture(null);
              }

              return updateGuildCommands(guild, true)
                  .thenCompose(
                      succeeded ->
                          succeeded
                              ? auditAndRepair(guild, true)
                              : CompletableFuture.completedFuture(null));
            });
  }

  private Audit auditGuild(Guild guild, List<Command> discordCommands) {
    Command ownerCommand =
        discordCommands.stream()
            .filter(command -> command.getType() == Command.Type.SLASH)
            .filter(command -> command.getName().equals("owner"))
            .findFirst()
            .orElse(null);
    if (ownerCommand == null) {
      TopLevelApplicationCommandInfo localOwner =
          applicationCommands.findTopLevelSlashCommand(guild, "owner");
      String localRoute = describeLocalOwner(localOwner);
      return new Audit(0, List.of("Discord slash /owner is missing; localOwner=" + localRoute));
    }

    long discordOwnerId = ownerCommand.getIdLong();
    TopLevelApplicationCommandInfo localOwner =
        applicationCommands.findTopLevelSlashCommand(guild, "owner");
    String localOwnerDescription = describeLocalOwner(localOwner);
    List<String> discordSubcommands =
        ownerCommand.getSubcommands().stream().map(Command.Subcommand::getName).toList();
    List<String> problems = new ArrayList<>();
    for (String leaf : OWNER_SUBCOMMANDS) {
      String path = "owner " + leaf;
      if (!discordSubcommands.contains(leaf)) {
        problems.add("Discord is missing /" + path);
      }

      SlashCommandInfo localRoute =
          applicationCommands.getApplicationCommandById(
              SlashCommandInfo.class, discordOwnerId, null, leaf);
      if (localRoute == null) {
        problems.add(
            "local route /"
                + path
                + " missing: discordOwnerId="
                + discordOwnerId
                + ", localOwner="
                + localOwnerDescription);
        continue;
      }

      TopLevelApplicationCommandInfo routeOwner = localRoute.getTopLevelInstance();
      if (!path.equals(localRoute.getFullCommandName())
          || routeOwner.getIdLong() != discordOwnerId
          || !Objects.equals(routeOwner.getGuildId(), guild.getIdLong())) {
        problems.add(
            "local route /"
                + path
                + " mismatch: path=/"
                + localRoute.getFullCommandName()
                + ", localOwnerId="
                + routeOwner.getIdLong()
                + ", localGuildId="
                + routeOwner.getGuildId());
      }
    }
    return new Audit(discordOwnerId, List.copyOf(problems));
  }

  private static void logFailure(String phase, Guild guild, Throwable failure) {
    Throwable cause = failure;
    while (cause instanceof CompletionException && cause.getCause() != null) {
      cause = cause.getCause();
    }
    logger.warn(
        "Owner command registration recovery failed for guild {} during {}: failure={}",
        guild.getId(),
        phase,
        cause.toString(),
        cause);
  }

  private static String describeLocalOwner(@Nullable TopLevelApplicationCommandInfo localOwner) {
    if (localOwner == null) {
      return "missing";
    }
    return "id=" + localOwner.getIdLong() + ", guildId=" + localOwner.getGuildId();
  }

  private record Audit(long discordOwnerId, List<String> problems) {
    private Audit {
      problems = List.copyOf(problems);
    }

    private boolean healthy() {
      return problems.isEmpty();
    }
  }

  private static final class AttemptGuard {
    private @Nullable Instant lastAttemptStartedAt;
    private boolean inFlight;

    private synchronized boolean tryStart(Instant now) {
      if (inFlight
          || (lastAttemptStartedAt != null && now.isBefore(lastAttemptStartedAt.plus(COOLDOWN)))) {
        return false;
      }
      lastAttemptStartedAt = now;
      inFlight = true;
      return true;
    }

    private synchronized void finish() {
      inFlight = false;
    }
  }
}
