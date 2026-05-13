package dev.skullition.lockium.service.command;

import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;

@Command
public class SlashPing {
    @JDASlashCommand(name = "ping", description = "Gets the REST ping.")
    public void onSlashPing(GuildSlashEvent event) {
        event.deferReply(true).queue();

        event.getJDA()
                .getRestPing()
                .queue(ping -> {
                    String output = String.format("Pong! Time taken: %s ms.", ping);
                    event.getHook().editOriginal(output).queue();
                });
    }
}
