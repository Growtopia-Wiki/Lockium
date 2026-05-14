package dev.skullition.lockium.service.command;

import dev.skullition.lockium.service.client.WikiClient;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Command
public class SlashPing {
    private static final Logger logger = LoggerFactory.getLogger(SlashPing.class);
    private final WikiClient wikiClient;

    public SlashPing(WikiClient wikiClient) {
        this.wikiClient = wikiClient;
    }

    @JDASlashCommand(name = "ping", description = "Check Discord and Wiki latency.")
    public void onSlashPing(GuildSlashEvent event) {
        event.deferReply(true).queue();

        event.getJDA()
                .getRestPing()
                .queue(ping -> {
                    long wikiPing = pingMillis();
                    
                    String wikiStatus = wikiPing >= 0 ? wikiPing + "ms" : "DOWN";
                    String output = String.format("Pong! Discord: %s ms. | Wiki API: %s", ping, wikiStatus);
                    event.getHook().editOriginal(output).queue();
                });
    }
    
    private long pingMillis() {
        long start = System.nanoTime();
        try {
            wikiClient.health();
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        } catch (Exception e) {
            logger.error("Ping failed!", e);
            return -1;
        }
    }
}
