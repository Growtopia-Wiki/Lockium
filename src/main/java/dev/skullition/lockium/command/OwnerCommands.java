package dev.skullition.lockium.command;

import dev.skullition.lockium.service.ChiService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiCacheService;
import dev.skullition.lockium.util.AppEmojis;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.text.CommandEvent;
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommandVariation;
import io.github.freya022.botcommands.api.commands.text.annotations.RequireOwner;
import io.github.freya022.botcommands.api.commands.text.annotations.TextOption;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Owner-only text commands for runtime administration.
 *
 * <p>Provides privileged utilities that are not exposed to normal users:
 *
 * <ul>
 *   <li>{@code activity} – update the bot's Discord presence
 *   <li>{@code reload} – force a full refresh of all in-memory caches
 * </ul>
 *
 * <p>All commands are guarded by {@link RequireOwner}.
 */
@Command
public class OwnerCommands {
  private final TreeFruitService fruitService;
  private final WikiCacheService cacheService;
  private final ChiService chiService;

  /**
   * Creates the owner command handler.
   *
   * @param fruitService service that holds the tree-fruit max-drop map
   * @param cacheService service that manages wiki API caches
   * @param chiService service that holds the item chi map
   */
  public OwnerCommands(
      TreeFruitService fruitService, WikiCacheService cacheService, ChiService chiService) {
    this.fruitService = fruitService;
    this.cacheService = cacheService;
    this.chiService = chiService;
  }

  /**
   * Updates the bot's custom Discord activity.
   *
   * <p>Executes {@code GET /activity <text>}. Sets a custom status via JDA and replies with a
   * confirmation message.
   *
   * @param event the command event containing the JDA instance and reply channel
   * @param activity the text to display as the bot's status; leading/trailing whitespace is trimmed
   *     by the command framework
   */
  @RequireOwner
  @JDATextCommandVariation(
      path = {"activity"},
      description = "Update bot activity.")
  public void onTextUpdateStatus(CommandEvent event, @TextOption String activity) {
    event.getJDA().getPresence().setActivity(Activity.customStatus(activity));
    event.reply("Activity updated to %s".formatted(activity)).queue();
  }

  /**
   * Reloads all bot caches from their sources.
   *
   * <p>Executes {@code GET /reload}. Calls {@link WikiCacheService#refreshCaches()} to evict and
   * re-fetch wiki data, then {@link TreeFruitService#reload()} to re-read {@code
   * tree-fruit-max.txt} from disk. Useful after deploying new data files without restarting.
   *
   * @param event the command event used to acknowledge completion
   */
  @RequireOwner
  @JDATextCommandVariation(
      path = {"reload"},
      description = "Reloads all bot cache.")
  public void onTextReload(CommandEvent event) {
    cacheService.refreshCaches();
    fruitService.reload();
    chiService.reload();
    event.reply("%s Reloaded all bot cache.".formatted(AppEmojis.LOADING)).queue();
  }
}
