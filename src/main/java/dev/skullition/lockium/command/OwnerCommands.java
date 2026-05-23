package dev.skullition.lockium.command;

import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.text.CommandEvent;
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommandVariation;
import io.github.freya022.botcommands.api.commands.text.annotations.RequireOwner;
import io.github.freya022.botcommands.api.commands.text.annotations.TextOption;
import net.dv8tion.jda.api.entities.Activity;

@Command
@RequireOwner
public class OwnerCommands {
    @JDATextCommandVariation(path = {"activity"}, description = "Update bot activity.")
    public void onSlashUpdateStatus(CommandEvent event, @TextOption String activity) {
        event.getJDA().getPresence().setActivity(Activity.customStatus(activity));
        event.reply("Activity updated to %s".formatted(activity)).queue();
    }
}
