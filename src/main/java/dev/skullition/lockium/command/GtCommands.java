package dev.skullition.lockium.command;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.modal.SlashBreakModal;
import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemProperty2;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import io.github.freya022.botcommands.api.modals.Modals;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static dev.skullition.lockium.handler.ItemNameAutocompleteHandler.ITEM_AUTOCOMPLETE_NAME;

@Command
public class GtCommands {
    private static final Logger logger = LoggerFactory.getLogger(GtCommands.class);
    private final Modals modals;
    private final WikiService wikiService;
    private final GrowtopiaDetailClient detailClient;
    private final LockiumProperties lockiumProperties;

    public GtCommands(Modals modals,
                      WikiService wikiService,
                      GrowtopiaDetailClient detailClient, 
                      LockiumProperties lockiumProperties) {
        this.modals = modals;
        this.wikiService = wikiService;
        this.detailClient = detailClient;
        this.lockiumProperties = lockiumProperties;
    }

    @TopLevelSlashCommandData(
            contexts = {
                    InteractionContextType.BOT_DM, InteractionContextType.GUILD, InteractionContextType.PRIVATE_CHANNEL
            },
            integrationTypes = {
                    IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL
            },
            description = "Commands related to Growtopia."
    )
    @JDASlashCommand(name = "gt", subcommand = "item", description = "Lookup a Growtopia item.")
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

        if (item.getClothingType() != null) {
            String clothingType = item.getClothingType().getItemName();
            var icon = item.getClothingType().getIcon();
            components.add(TextDisplay.of("**%s Clothes - %s**".formatted(icon, clothingType)));
        } else if (item.categoryInfo().type() != null) {
            String categoryType = item.categoryInfo().name();
            components.add(TextDisplay.of("**%s - %s**".formatted(item.categoryInfo().name(), categoryType)));
        } else {
            components.add(TextDisplay.of("**%s**".formatted(item.categoryInfo().name())));
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

    @JDASlashCommand(name = "gt", subcommand = "sprite", description = "Lookup a Growtopia item's sprite.")
    public void onSlashSprite(GlobalSlashEvent event,
                              @SlashOption(description = "The item name you are looking for.", autocomplete = ITEM_AUTOCOMPLETE_NAME)
                              ItemCatalogue itemQuery) {
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

    @JDASlashCommand(name = "gt", subcommand = "break", description = "Get the amount of items you get from breaking blocks.")
    public void onSlashBreak(GlobalSlashEvent event,
                             @SlashOption(description = "The item name you'd like to break.", autocomplete = ITEM_AUTOCOMPLETE_NAME)
                             ItemCatalogue itemQuery,
                             @SlashOption(description = "How many blocks?") int blockCount
                             ) {
        var modal = modals.create("Break %s".formatted(itemQuery.itemName()))
                .addComponents(
                        TextDisplay.of("Are you using the Lucky! mod? (e.g. Lucky Clover, Songpyeon)"),
                        Label.of("Yes or no?", 
                                        StringSelectMenu.create(SlashBreakModal.INPUT_LUCKY)
                                                .addOption("Yes", "Yes")
                                                .addOption("No", "No")
                                        .build()
                ))
                .bindTo(SlashBreakModal.MODAL_NAME, itemQuery, blockCount)
                .build();
        event.replyModal(modal).queue();
    }

    @JDASlashCommand(name = "gt", subcommand = "wotd", description = "Render today's World of the Day.")
    public void onSlashWotd(GlobalSlashEvent event) {
        var detail = detailClient.getGrowtopiaDetail();
        String wotd = detail.wotd().fullSize().substring(7);
        int dotIndex = wotd.indexOf(".");

        String renderUrl = lockiumProperties.renderUrl();
        var container = ContainerUtil.createGenericContainer(
                TextDisplay.of("## %s WOTD: %s".formatted(AppEmojis.WOTD, wotd.substring(0, dotIndex).toUpperCase())),
                MediaGallery.of(MediaGalleryItem.fromUrl(renderUrl + wotd.toLowerCase()))
        );

        event.replyComponents(container)
                .useComponentsV2()
                .queue();
    }
}
