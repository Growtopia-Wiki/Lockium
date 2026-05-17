package dev.skullition.lockium.service.command;

import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Command
public class SlashPing {
    private static final Logger logger = LoggerFactory.getLogger(SlashPing.class);
    private final WikiService wiki;

    public SlashPing(WikiService wiki) {
        this.wiki = wiki;
    }

    @TopLevelSlashCommandData(
            contexts = {
                    InteractionContextType.BOT_DM, InteractionContextType.GUILD, InteractionContextType.PRIVATE_CHANNEL
            },
            integrationTypes = {
                    IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL
            }
    )
    @JDASlashCommand(name = "ping", description = "Check Discord and Wiki latency.")
    public void onSlashPing(GlobalSlashEvent event) {
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
            wiki.health();
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        } catch (Exception e) {
            logger.error("Ping failed!", e);
            return -1;
        }
    }
}
