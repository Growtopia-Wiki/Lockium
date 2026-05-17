package dev.skullition.lockium.service.command;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command
@NullMarked
public class SlashItem {
    private static final Logger logger = LoggerFactory.getLogger(SlashItem.class);
    private final WikiService wiki;

    public SlashItem(WikiService wiki) {
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
    @JDASlashCommand(name = "item", description = "Lookup a Growtopia item.")
    public void onSlashItem(GlobalSlashEvent event,
                            @SlashOption(description = "The item name you are looking for.") String item) {
        //TODO: Fix this after fixing options
        event.reply(item.toString()).queue();
    }
}
