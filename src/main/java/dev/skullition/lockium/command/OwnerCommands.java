package dev.skullition.lockium.command;

import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiCacheService;
import dev.skullition.lockium.util.AppEmojis;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.text.CommandEvent;
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommandVariation;
import io.github.freya022.botcommands.api.commands.text.annotations.RequireOwner;
import io.github.freya022.botcommands.api.commands.text.annotations.TextOption;
import net.dv8tion.jda.api.entities.Activity;

@Command
@RequireOwner
public class OwnerCommands {
    private final TreeFruitService fruitService;
    private final WikiCacheService cacheService;

    public OwnerCommands(TreeFruitService fruitService, WikiCacheService cacheService) {
        this.fruitService = fruitService;
        this.cacheService = cacheService;
    }

    @JDATextCommandVariation(path = {"activity"}, description = "Update bot activity.")
    public void onTextUpdateStatus(CommandEvent event, @TextOption String activity) {
        event.getJDA().getPresence().setActivity(Activity.customStatus(activity));
        event.reply("Activity updated to %s".formatted(activity)).queue();
    }

    @JDATextCommandVariation(path = {"reload"}, description = "Reloads all bot cache.")
    public void onTextReload(CommandEvent event) {
        cacheService.refreshCaches();
        fruitService.reload();
        event.reply("%s Reloaded all bot cache.".formatted(AppEmojis.LOADING)).queue();
    }
}
