package dev.skullition.lockium.command;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemProperty2;
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

        String propFlag = item.propFlagText();
        String propFlag2Text = item.propFlag2Text();
        // Maybe add non-flag like fish in the future
        if (propFlag2Text != null) {
            propFlag = propFlag + propFlag2Text;
        }
        components.add(TextDisplay.of(propFlag));

        if (item.clothingType() != null) {
            String clothingType = item.clothingType().name();
            components.add(TextDisplay.of("**%s - %s**".formatted(item.category().name(), clothingType)));
        } else if (item.category().type() != null) {
            String categoryType = item.category().name();
            components.add(TextDisplay.of("**%s - %s**".formatted(item.category().name(), categoryType)));
        } else {
            components.add(TextDisplay.of("**%s**".formatted(item.category().name())));
        }

        String rarity = item.rarity() == 999 ? "None (999)" : String.valueOf(item.rarity());
        components.add(TextDisplay.of("**%s Rarity:** `%s`  **%s %s**".formatted(
                AppEmojis.RARITY, rarity, AppEmojis.COLLISION, item.collisionType().name())));

        int hardness = item.hardness();
        components.add(TextDisplay.of("**Hardness:** %s `%s hits` **%s** `%s hits (%s seconds to restore.)`".formatted(
                AppEmojis.FIST, (int) Math.ceil(hardness / 6.0), AppEmojis.PICKAXE, (int) Math.ceil(hardness / 8.0), item.restoreTime())));

        String baseColor = seed.baseColor().hex();
        String overColor = seed.overColor().hex();
        components.add(TextDisplay.of("**Base color:** `%s` **Overlay color:** `%s`".formatted(baseColor, overColor)));

        String growTime = ItemUtils.toDHMS(seed.growTime());
        components.add(TextDisplay.of("%s `%s` to grow.".formatted(AppEmojis.GROW_SPRAY, growTime)));

        String gemDrops;
        if (item.rarity() == 999 || ItemProperty2.fromInt(item.propFlag2().raw()).contains(ItemProperty2.GEMLESS)) {
            gemDrops = "N/A";
        } else if (item.rarity() > 30) {
            gemDrops = "0 - %s".formatted((int) Math.floor(item.rarity() / 4.0 + 1.0));
        } else if (item.rarity() >= 8) {
            gemDrops = "0 - %s".formatted((int) Math.floor(Math.floor(item.rarity() / 4.0) * 0.75 + 1.0));
        } else {
            gemDrops = "0 - 1";
        }
        components.add(TextDisplay.of("%s `%s` gems dropped.".formatted(AppEmojis.GEM, gemDrops)));

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
