package dev.skullition.lockium.command;

import dev.skullition.lockium.service.ChiService;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiCacheService;
import dev.skullition.lockium.util.AppEmojis;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.CommandScope;
import io.github.freya022.botcommands.api.commands.application.annotations.Test;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import io.github.freya022.botcommands.api.core.BotOwners;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owner-only slash commands for runtime administration.
 *
 * <p>Provides privileged utilities that are not exposed to normal users:
 *
 * <ul>
 *   <li>{@code /owner activity} – update the bot's Discord presence
 *   <li>{@code /owner reload} – force a full refresh of all in-memory caches
 * </ul>
 *
 * <p>The {@code /owner} command is annotated with {@link Test}, so it is only pushed to the test
 * guilds configured by {@code botcommands.application.testGuildIds}, and is {@code defaultLocked}
 * so only administrators see it by default. Subcommands additionally reject users that are not
 * {@linkplain BotOwners bot owners}.
 */
@Command
public class OwnerCommands {
  private static final Logger logger = LoggerFactory.getLogger(OwnerCommands.class);

  private final TreeFruitService fruitService;
  private final WikiCacheService cacheService;
  private final ChiService chiService;
  private final RiddleService riddleService;
  private final ItemEffectService itemEffectService;
  private final BotOwners botOwners;

  /**
   * Creates the owner command handler.
   *
   * @param fruitService service that holds the tree-fruit max-drop map
   * @param cacheService service that manages wiki API caches
   * @param chiService service that holds the item chi map
   * @param riddleService service that holds the ancestral riddle dataset
   * @param itemEffectService service that holds seed and scraped item effects
   * @param botOwners registry of bot owners used to gate slash commands
   */
  public OwnerCommands(
      TreeFruitService fruitService,
      WikiCacheService cacheService,
      ChiService chiService,
      RiddleService riddleService,
      ItemEffectService itemEffectService,
      BotOwners botOwners) {
    this.fruitService = fruitService;
    this.cacheService = cacheService;
    this.chiService = chiService;
    this.riddleService = riddleService;
    this.itemEffectService = itemEffectService;
    this.botOwners = botOwners;
  }

  /**
   * Handles {@code /owner activity}.
   *
   * <p>Sets a custom status via JDA and replies ephemerally with a confirmation message.
   *
   * @param event the slash interaction
   * @param activity the text to display as the bot's status
   */
  @Test({})
  @TopLevelSlashCommandData(
      scope = CommandScope.GUILD,
      defaultLocked = true,
      description = "Owner-only administration commands.")
  @JDASlashCommand(name = "owner", subcommand = "activity", description = "Update bot activity.")
  public void onSlashActivity(
      GuildSlashEvent event,
      @SlashOption(description = "The text to display as the bot's status.") String activity) {
    if (rejectNonOwner(event)) {
      return;
    }
    logger.info(
        "Owner {} updating activity in guild {} to '{}'",
        event.getUser().getId(),
        event.getGuild().getId(),
        activity);
    event.getJDA().getPresence().setActivity(Activity.customStatus(activity));
    event.reply("Activity updated to %s".formatted(activity)).setEphemeral(true).queue();
  }

  /**
   * Handles {@code /owner reload}.
   *
   * <p>Calls {@link WikiCacheService#refreshCaches()} to evict and re-fetch wiki data, then reloads
   * the {@link TreeFruitService}, {@link ChiService}, {@link RiddleService}, and {@link
   * ItemEffectService} data files from disk. Useful after deploying new data files without
   * restarting.
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(name = "owner", subcommand = "reload", description = "Reloads all bot cache.")
  public void onSlashReload(GuildSlashEvent event) {
    if (rejectNonOwner(event)) {
      return;
    }
    logger.info(
        "Owner {} requested a full cache reload in guild {}",
        event.getUser().getId(),
        event.getGuild().getId());
    long start = System.nanoTime();
    cacheService.refreshCaches();
    fruitService.reload();
    chiService.reload();
    riddleService.reload();
    itemEffectService.reload();
    logger.info(
        "Full cache reload completed in {} ms: treeFruits={}, chi={}, riddles={}, effectItems={}",
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
        fruitService.size(),
        chiService.size(),
        riddleService.size(),
        itemEffectService.size());
    event
        .reply("%s Reloaded all bot cache.".formatted(AppEmojis.LOADING))
        .setEphemeral(true)
        .queue();
  }

  /**
   * Rejects the interaction if the invoking user is not a bot owner.
   *
   * @param event the slash interaction to check and, if rejected, reply to
   * @return {@code true} if the user is not a bot owner and an ephemeral rejection was sent
   */
  private boolean rejectNonOwner(GuildSlashEvent event) {
    if (botOwners.isOwner(event.getUser())) {
      return false;
    }
    logger.warn(
        "Rejected owner command from user {} in guild {}",
        event.getUser().getId(),
        event.getGuild().getId());
    event
        .reply("%s Only bot owners can use this command.".formatted(AppEmojis.NO))
        .setEphemeral(true)
        .queue();
    return true;
  }
}
