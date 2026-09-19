package dev.skullition.lockium.command;

import static dev.skullition.lockium.util.FormatUtil.formatNumber;

import dev.skullition.lockium.service.ChiService;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiCacheService;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.CommandScope;
import io.github.freya022.botcommands.api.commands.application.annotations.Test;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import io.github.freya022.botcommands.api.core.BotOwners;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owner-only slash commands for runtime administration.
 *
 * <p>Provides privileged utilities that are not exposed to normal users:
 *
 * <ul>
 *   <li>{@code /owner activity} – update the bot's Discord presence
 *   <li>{@code /owner guilds} – list every guild the bot is connected to
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

  /** Leaves room for the heading and component metadata below Discord's message limit. */
  private static final int GUILD_LIST_INLINE_LENGTH = 3_000;

  private final TreeFruitService fruitService;
  private final WikiCacheService cacheService;
  private final ChiService chiService;
  private final RiddleService riddleService;
  private final ItemEffectService itemEffectService;
  private final GrowtopiaDetailService detailService;
  private final GrowtopiaLeaderboardService leaderboardService;
  private final BotOwners botOwners;

  /**
   * Creates the owner command handler.
   *
   * @param fruitService service that holds the tree-fruit max-drop map
   * @param cacheService service that manages wiki API caches
   * @param chiService service that holds the item chi map
   * @param riddleService service that holds the ancestral riddle dataset
   * @param itemEffectService service that holds seed and scraped item effects
   * @param detailService publisher of the polled Growtopia detail snapshot
   * @param leaderboardService publisher of the polled Growtopia leaderboards
   * @param botOwners registry of bot owners used to gate slash commands
   */
  public OwnerCommands(
      TreeFruitService fruitService,
      WikiCacheService cacheService,
      ChiService chiService,
      RiddleService riddleService,
      ItemEffectService itemEffectService,
      GrowtopiaDetailService detailService,
      GrowtopiaLeaderboardService leaderboardService,
      BotOwners botOwners) {
    this.fruitService = fruitService;
    this.cacheService = cacheService;
    this.chiService = chiService;
    this.riddleService = riddleService;
    this.itemEffectService = itemEffectService;
    this.detailService = detailService;
    this.leaderboardService = leaderboardService;
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
   * Handles {@code /owner guilds}.
   *
   * <p>Lists the guilds already held by JDA's cache. Long lists are attached as a UTF-8 text file
   * because Discord limits the combined text in a Components V2 message. {@link
   * Guild#getMemberCount()} does not load or cache members, so this does not require the privileged
   * member intent; without that intent, however, Discord does not keep the count updated between
   * guild payloads.
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(
      name = "owner",
      subcommand = "guilds",
      description = "List every server the bot is in.")
  public void onSlashGuilds(GuildSlashEvent event) {
    if (rejectNonOwner(event)) {
      return;
    }

    List<Guild> guilds = new ArrayList<>(event.getJDA().getGuilds());
    guilds.sort(
        Comparator.comparing(Guild::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Guild::getId));
    logger.info(
        "Owner {} requested the guild list in guild {}: guilds={}",
        event.getUser().getId(),
        event.getGuild().getId(),
        guilds.size());

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(TextDisplay.of("## Servers (`%s`)".formatted(formatNumber(guilds.size()))));
    components.add(Separator.create(true, Separator.Spacing.LARGE));
    FileUpload attachment = null;
    if (guilds.isEmpty()) {
      components.add(TextDisplay.of("The bot is not connected to any servers."));
    } else {
      String guildList = createGuildList(guilds);
      if (guildList.length() <= GUILD_LIST_INLINE_LENGTH) {
        components.add(TextDisplay.of(guildList));
      } else {
        components.add(
            TextDisplay.of(
                "The complete server list is attached because it exceeds Discord's message "
                    + "limit."));
        attachment = FileUpload.fromData(guildList.getBytes(StandardCharsets.UTF_8), "guilds.txt");
      }
    }

    var reply =
        event
            .replyComponents(ContainerUtil.createGenericContainer(components))
            .setAllowedMentions(List.of())
            .useComponentsV2();
    if (attachment != null) {
      reply.addFiles(attachment);
    }
    reply.queue();
  }

  /**
   * Handles {@code /owner reload}.
   *
   * <p>Calls {@link WikiCacheService#refreshCaches()} to evict and re-fetch wiki data, then reloads
   * the {@link TreeFruitService}, {@link ChiService}, {@link RiddleService}, and {@link
   * ItemEffectService} data files from disk, and finally re-polls the Growtopia proxy. Useful after
   * deploying new data files without restarting.
   *
   * <p>Recorded player-count samples are deliberately left untouched: this command means "re-read
   * external data", and discarding the graph history would be a surprising side effect.
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
    final long start = System.nanoTime();
    cacheService.refreshCaches();
    fruitService.reload();
    chiService.reload();
    riddleService.reload();
    itemEffectService.reload();
    detailService.refresh();
    leaderboardService.refresh();
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

  private static String createGuildList(List<Guild> guilds) {
    StringBuilder content = new StringBuilder();
    for (Guild guild : guilds) {
      content.append(
          "▫ %s — `%s` members\n"
              .formatted(
                  MarkdownSanitizer.escape(guild.getName()), formatNumber(guild.getMemberCount())));
    }
    return content.toString();
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
