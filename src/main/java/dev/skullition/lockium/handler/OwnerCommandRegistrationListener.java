package dev.skullition.lockium.handler;

import dev.skullition.lockium.service.OwnerCommandRegistrationService;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommandsContext;
import io.github.freya022.botcommands.api.commands.application.slash.SlashCommandInfo;
import io.github.freya022.botcommands.api.core.BotOwners;
import io.github.freya022.botcommands.api.core.annotations.BEventListener;
import io.github.freya022.botcommands.api.core.annotations.BEventListener.RunMode;
import io.github.freya022.botcommands.api.core.config.BApplicationConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Checks owner-command registration at startup and after an owner's unresolved interaction.
 *
 * <p>This observer never acknowledges or executes interactions. BotCommands remains responsible for
 * routing and authorization; a failed invocation may need to be retried once reconciliation
 * finishes. Registration work is asynchronous and limited by {@link
 * OwnerCommandRegistrationService}.
 */
@Service
public class OwnerCommandRegistrationListener {
  private static final Logger logger =
      LoggerFactory.getLogger(OwnerCommandRegistrationListener.class);

  private final OwnerCommandRegistrationService registration;
  private final ApplicationCommandsContext commands;
  private final BotOwners owners;
  private final BApplicationConfig config;

  /** Creates the observer using the existing framework configuration and owner registry. */
  public OwnerCommandRegistrationListener(
      OwnerCommandRegistrationService registration,
      ApplicationCommandsContext commands,
      BotOwners owners,
      BApplicationConfig config) {
    this.registration = registration;
    this.commands = commands;
    this.owners = owners;
    this.config = config;
  }

  /** Checks newly available guilds, including guilds that become ready after initial login. */
  @BEventListener(mode = RunMode.ASYNC)
  public void onGuildReady(GuildReadyEvent event) {
    registration.reconcile(event.getGuild());
  }

  /** Logs effective registration settings and checks configured guilds after login completes. */
  @BEventListener(mode = RunMode.ASYNC)
  public void onReady(ReadyEvent event) {
    var cache = config.getCache();
    logger.info(
        "Owner command registration: application={}, enabled={}, adminGuilds={},"
            + " guildsToUpdate={}, checkOnline={}",
        event.getJDA().getSelfUser().getApplicationIdLong(),
        config.getEnable(),
        config.getTestGuildIds(),
        config.getGuildsToUpdate(),
        cache == null ? "cache disabled" : cache.getCheckOnline());
    if (!config.getEnable()) {
      return;
    }
    for (long guildId : config.getTestGuildIds()) {
      @Nullable Guild guild = event.getJDA().getGuildById(guildId);
      if (guild == null) {
        logger.warn("Configured owner-command guild {} is not available to this bot", guildId);
      } else {
        registration.reconcile(guild);
      }
    }
  }

  /** Requests recovery when an owner invokes a command absent from the framework's routing map. */
  @BEventListener(mode = RunMode.ASYNC)
  public void onSlashCommand(SlashCommandInteractionEvent event) {
    @Nullable Guild guild = event.getGuild();
    if (!event.getName().equals("owner") || guild == null || !owners.isOwner(event.getUser())) {
      return;
    }
    var command =
        commands.getApplicationCommandById(
            SlashCommandInfo.class,
            event.getCommandIdLong(),
            event.getSubcommandGroup(),
            event.getSubcommandName());
    if (command != null) {
      return;
    }
    if (!registration.isEligible(guild)) {
      logger.warn(
          "Unresolved owner interaction in guild {} is outside the effective registration scope;"
              + " check application enablement, testGuildIds and guildsToUpdate",
          guild.getIdLong());
      return;
    }
    logger.warn(
        "Unresolved owner interaction: application={}, guild={}, commandId={}, path={};"
            + " checking registration, caller may need to retry",
        event.getJDA().getSelfUser().getApplicationIdLong(),
        guild.getIdLong(),
        event.getCommandIdLong(),
        event.getFullCommandName());
    registration.reconcile(guild);
  }
}
