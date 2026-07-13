package dev.skullition.lockium.command;

import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slash command that measures latency to Discord and the Growtopia Wiki API.
 *
 * <p>Registered as {@code /ping} globally. The command is available in guilds, DMs, and private
 * channels, and supports both guild and user installs.
 *
 * <p>It performs two checks in sequence:
 *
 * <ol>
 *   <li>Discord REST ping via {@code JDA#getRestPing()}
 *   <li>Wiki API health via {@link WikiService#health()}, measured once the Discord ping resolves
 * </ol>
 *
 * <p>The reply is ephemeral and shows both timings, or {@code DOWN} if the Wiki check fails.
 */
@Command
public class SlashPing {
  /** Logger for health check failures. */
  private static final Logger logger = LoggerFactory.getLogger(SlashPing.class);

  /** Service used to probe the Wiki API. */
  private final WikiService wiki;

  /**
   * Creates the ping command.
   *
   * @param wiki the Wiki service, injected by Spring
   */
  public SlashPing(WikiService wiki) {
    this.wiki = wiki;
  }

  /**
   * Handles the {@code /ping} interaction.
   *
   * <p>Defers the reply ephemerally, then measures Discord latency and calls {@link #pingMillis()}
   * for the Wiki. The final message is edited in place.
   *
   * @param event the slash event, provided by BotCommands
   */
  @TopLevelSlashCommandData(
      contexts = {
        InteractionContextType.BOT_DM,
        InteractionContextType.GUILD,
        InteractionContextType.PRIVATE_CHANNEL
      },
      integrationTypes = {IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL})
  @JDASlashCommand(name = "ping", description = "Check Discord and Wiki latency.")
  public void onSlashPing(GlobalSlashEvent event) {
    logger.debug("onSlashPing: starting Discord and Wiki API health checks");
    event.deferReply(true).queue();

    event
        .getJDA()
        .getRestPing()
        .queue(
            ping -> {
              long wikiPing = pingMillis();

              String wikiStatus = wikiPing >= 0 ? wikiPing + "ms" : "DOWN";
              String output =
                  String.format("Pong! Discord: %s ms. | Wiki API: %s", ping, wikiStatus);
              logger.debug("onSlashPing: Discord={} ms, Wiki API={}", ping, wikiStatus);
              event.getHook().editOriginal(output).queue();
            },
            failure -> logger.warn("Discord REST ping failed: {}", failure.getMessage()));
  }

  /**
   * Measures a round-trip to the Wiki {@code /health} endpoint.
   *
   * @return elapsed time in milliseconds, or {@code -1} if the call throws
   */
  private long pingMillis() {
    long start = System.nanoTime();
    try {
      wiki.health();
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    } catch (Exception e) {
      logger.warn("Wiki API ping failed: {}", e.getMessage());
      return -1;
    }
  }
}
