package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jspecify.annotations.Nullable;

@Resolver
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

        return wikiService.findByName(itemName);
    }
}
