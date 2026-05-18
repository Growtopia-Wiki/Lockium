package dev.skullition.lockium.command;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.skullition.lockium.handler.ItemNameAutocompleteHandler.ITEM_AUTOCOMPLETE_NAME;

@Command
public class SlashItem {
    private static final Logger logger = LoggerFactory.getLogger(SlashItem.class);
    private final WikiService wikiService;

    public SlashItem(WikiService wikiService) {
        this.wikiService = wikiService;
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
                            @SlashOption(description = "The item name you are looking for.", autocomplete = ITEM_AUTOCOMPLETE_NAME)
                            ItemCatalogue itemName) {
        logger.debug("onSlashItem: itemName={}", itemName);
        event.reply(itemName.toString()).queue();
    }

}
