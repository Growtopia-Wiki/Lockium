package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Resolver
@NullMarked
public class ItemCatalogueResolver
        extends ClassParameterResolver<ItemCatalogueResolver, ItemCatalogue>
        implements SlashParameterResolver<ItemCatalogueResolver, ItemCatalogue> {
    private final WikiService wikiService;

    public ItemCatalogueResolver(WikiService wikiService) {
        super(ItemCatalogue.class);
        this.wikiService = wikiService;
    }

    @Override
    public OptionType getOptionType() {
        return OptionType.STRING;
    }

    @Override
    @Nullable
    public ItemCatalogue resolve(SlashCommandOption option, CommandInteractionPayload event, OptionMapping optionMapping) {
        return resolveItemByName(optionMapping);
    }
    
    
    @Nullable
    private ItemCatalogue resolveItemByName(OptionMapping optionMapping) {
        String itemName = optionMapping.getAsString().trim();
        Map<Integer, ItemCatalogue> items = wikiService.getItems().items();
        
        return items.values()
                .stream()
                .filter(item -> item.itemName().equalsIgnoreCase(itemName))
                .findAny()
                .orElse(null);
    }

    @Override
    public Collection<Command.Choice> getPredefinedChoices(@Nullable Guild guild) {
        Map<Integer, ItemCatalogue> items = wikiService.getItems().items();
        var predefinedItems = List.of(items.get(1), items.get(2), items.get(3));
        return predefinedItems.stream()
                .map(item -> new Command.Choice(item.itemName(), item.itemId()))
                .toList();
    }
}
