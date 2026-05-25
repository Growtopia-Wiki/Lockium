package dev.skullition.lockium.command;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
                            ItemCatalogue itemQuery) {
        logger.debug("onSlashItem: itemQuery={}", itemQuery);
        ItemDetailResponse itemResponse = wikiService.getItemDetail(itemQuery);
        GrowtopiaObject item = itemResponse.item();
        GrowtopiaObject seed = itemResponse.seed();

        List<ContainerChildComponent> components = new ArrayList<>();
        
        String rarity = item.rarity() == 999 ? "None (999)" : String.valueOf(item.rarity());
        components.add(TextDisplay.of("**%s Rarity:** %s  **%s %s**".formatted(
                AppEmojis.RARITY, rarity, AppEmojis.COLLISION, item.collisionType().name())));
        
        if (item.category().type() == null) {
            components.add(TextDisplay.of("**%s**".formatted(item.category().name())));
        } else {
            components.add(TextDisplay.of("**%s - %s**".formatted(item.category().name(), item.category().type())));
        }
        
        int hardness = item.hardness();
        components.add(TextDisplay.of("**Hardness:** %s %s hits **%s** %s hits (%s seconds to restore.)".formatted(
                AppEmojis.FIST, Math.ceil(hardness / 6.0), AppEmojis.PICKAXE, Math.ceil(hardness / 8.0), item.restoreTime())));
        
        String baseColor = seed.baseColor().hex();
        String overColor = seed.overColor().hex();
        components.add(TextDisplay.of("**Base color:** %s **Overlay color:** %s".formatted(baseColor, overColor)));
        
        String growTime = ItemUtils.toDHMS(seed.growTime());
        components.add(TextDisplay.of("%s %s to grow.".formatted(AppEmojis.GROW_SPRAY, growTime)));
        
        String propFlag = item.propFlagText();
        String propFlag2Text = item.propFlag2Text();
        // Maybe add non-flag like fish in the future
        if (propFlag2Text != null) {
            propFlag = propFlag + propFlag2Text;
        }
        components.add(TextDisplay.of(propFlag));

        Container container = ItemUtils.createItemContainer(
                itemResponse,
                itemQuery,
                components
        );

        event.replyComponents(container)
                .useComponentsV2()
                .queue();
    }

}
