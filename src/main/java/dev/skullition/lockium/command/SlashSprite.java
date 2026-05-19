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
    public static final String ITEM_SPRITE_URL = "https://cdn.growtopiawiki.com/sprites/%s.png";
    public static final String TREE_SPRITE_URL = "https://cdn.growtopiawiki.com/sprites/%s-tree.png";

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
                              ItemCatalogue itemName) {
        // Might remove or cache this in the future if rate-limited
        ItemDetailResponse item = wikiService.getItemDetail(itemName);
        String itemUrl = String.format(ITEM_SPRITE_URL, itemName.itemId());
        String seedUrl = String.format(ITEM_SPRITE_URL, itemName.seedId());
        String treeUrl = String.format(TREE_SPRITE_URL, itemName.seedId());
        
        Container container = ItemUtils.createItemContainer(
                item,
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
