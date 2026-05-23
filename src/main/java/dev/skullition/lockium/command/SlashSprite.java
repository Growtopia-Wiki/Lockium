package dev.skullition.lockium.command;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.awt.*;

import static dev.skullition.lockium.handler.ItemNameAutocompleteHandler.ITEM_AUTOCOMPLETE_NAME;

@Command
public class SlashSprite {
    private final WikiService wikiService;

    public SlashSprite(WikiService wikiService) {
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
    @JDASlashCommand(name = "sprite", description = "Lookup a Growtopia item.")
    public void onSlashSprite(GlobalSlashEvent event,
                              @SlashOption(description = "The item name you are looking for.", autocomplete = ITEM_AUTOCOMPLETE_NAME)
                              ItemCatalogue itemQuery) {
        // Might remove or cache this in the future if rate-limited
        ItemDetailResponse item = wikiService.getItemDetail(itemQuery);
        String itemUrl = ItemUtils.getItemSpriteUrl(item.item().id());
        String seedUrl = ItemUtils.getItemSpriteUrl(item.seed().id());
        String treeUrl = ItemUtils.getTreeSpriteUrl(item.seed().id());
        
        Container container = ItemUtils.createItemContainer(
                item,
                itemQuery,
                MediaGallery.of(
                        MediaGalleryItem.fromUrl(itemUrl),
                        MediaGalleryItem.fromUrl(seedUrl),
                        MediaGalleryItem.fromUrl(treeUrl)
                )
        );
        
        event.replyComponents(container)
                .useComponentsV2()
                .queue();
    }
}
