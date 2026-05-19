package dev.skullition.lockium.handler;

import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteMode;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.util.Collection;

@Handler
public class ItemNameAutocompleteHandler {
    public static final String ITEM_AUTOCOMPLETE_NAME = "SlashItem: itemName";
    private final WikiService wikiService;

    public ItemNameAutocompleteHandler(WikiService wikiService) {
        this.wikiService = wikiService;
    }

    @AutocompleteHandler(value = ITEM_AUTOCOMPLETE_NAME, mode = AutocompleteMode.CONTINUITY)
    public Collection<String> onItemNameAutocomplete(CommandAutoCompleteInteractionEvent event) {
        return wikiService.getNameIndex().keySet();
    }
}
