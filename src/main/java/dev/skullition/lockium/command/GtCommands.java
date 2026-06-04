package dev.skullition.lockium.command;

import static dev.skullition.lockium.handler.ItemNameAutocompleteHandler.ITEM_AUTOCOMPLETE_NAME;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.modal.SlashBreakModal;
import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemCategory;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemProperty;
import dev.skullition.lockium.model.ItemProperty2;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.service.TreeFruitService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slash commands under {@code /gt} for Growtopia lookups.
 *
 * <p>All commands work in guilds, bot DMs, and private channels, and support both guild-install and
 * user-install contexts. Data comes from {@link WikiService} (cached wiki API) and {@link
 * GrowtopiaDetailClient} for live details. Responses use JDA Components V2.
 */
@Command
public class GtCommands {
  private static final Logger logger = LoggerFactory.getLogger(GtCommands.class);
  private final Modals modals;
  private final WikiService wikiService;
  private final GrowtopiaDetailClient detailClient;
  private final LockiumProperties lockiumProperties;
  private final TreeFruitService fruitService;

  /**
   * Creates the command handler.
   *
   * @param modals modal manager for interactive flows
   * @param wikiService cached access to the wiki item API
   * @param detailClient client for live Growtopia data (WOTD)
   * @param lockiumProperties application configuration, including render URLs
   * @param fruitService service used to determine an item is farmable
   */
  public GtCommands(
      Modals modals,
      WikiService wikiService,
      GrowtopiaDetailClient detailClient,
      LockiumProperties lockiumProperties,
      TreeFruitService fruitService) {
    this.modals = modals;
    this.wikiService = wikiService;
    this.detailClient = detailClient;
    this.lockiumProperties = lockiumProperties;
    this.fruitService = fruitService;
  }

  /**
   * Handles {@code /gt item}.
   *
   * <p>Resolves the autocomplete selection to a full {@link ItemDetailResponse}, then builds a
   * Components V2 container showing properties, category, rarity, hardness, colors, grow time, and
   * estimated gem drops. Uses {@link ItemProperty} and {@link ItemProperty2} for flag text.
   *
   * @param event the slash interaction
   * @param itemQuery the item chosen via autocomplete; never null
   */
  @TopLevelSlashCommandData(
      contexts = {
        InteractionContextType.BOT_DM,
        InteractionContextType.GUILD,
        InteractionContextType.PRIVATE_CHANNEL
      },
      integrationTypes = {IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL},
      description = "Commands related to Growtopia.")
  @JDASlashCommand(name = "gt", subcommand = "item", description = "Lookup a Growtopia item.")
  public void onSlashItem(
      GlobalSlashEvent event,
      @SlashOption(
              description = "The item name you are looking for.",
              autocomplete = ITEM_AUTOCOMPLETE_NAME)
          ItemCatalogue itemQuery) {
    logger.debug("onSlashItem: itemQuery={}", itemQuery);
    final ItemDetailResponse itemResponse = wikiService.getItemDetail(itemQuery);
    final GrowtopiaObject item = itemResponse.item();
    final GrowtopiaObject seed = itemResponse.seed();

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
      components.add(
          TextDisplay.of("**%s - %s**".formatted(item.categoryInfo().name(), categoryType)));
    } else {
      components.add(TextDisplay.of("**%s**".formatted(item.categoryInfo().name())));
    }

    String rarity = item.rarity() == 999 ? "None (999)" : String.valueOf(item.rarity());
    components.add(
        TextDisplay.of(
            "**%s Rarity:** `%s`  **%s %s**"
                .formatted(
                    AppEmojis.RARITY, rarity, AppEmojis.COLLISION, item.collisionType().name())));

    int hardness = item.hardness();
    components.add(
        TextDisplay.of(
            "**Hardness:** %s `%s hits` **%s** `%s hits (%s seconds to restore.)`"
                .formatted(
                    AppEmojis.FIST,
                    (int) Math.ceil(hardness / 6.0),
                    AppEmojis.PICKAXE,
                    (int) Math.ceil(hardness / 8.0),
                    item.restoreTime())));

    String baseColor = seed.baseColor().hex();
    String overColor = seed.overColor().hex();
    components.add(
        TextDisplay.of(
            "**Base color:** `%s` **Overlay color:** `%s`".formatted(baseColor, overColor)));

    String growTime = ItemUtils.toDayHourMinutesSeconds(seed.growTime());
    components.add(TextDisplay.of("%s `%s` to grow.".formatted(AppEmojis.GROW_SPRAY, growTime)));

    String gemDrops;
    if (item.rarity() == 999
        || ItemProperty2.fromInt(item.propFlag2().raw()).contains(ItemProperty2.GEMLESS)) {
      gemDrops = "N/A";
    } else if (item.rarity() > 30) {
      gemDrops = "0 - %s".formatted((int) Math.floor(item.rarity() / 4.0 + 1.0));
    } else if (item.rarity() >= 8) {
      gemDrops = "0 - %s".formatted((int) Math.floor(Math.floor(item.rarity() / 4.0) * 0.75 + 1.0));
    } else {
      gemDrops = "0 - 1";
    }
    components.add(TextDisplay.of("%s `%s` gems dropped.".formatted(AppEmojis.GEM, gemDrops)));

    Container container = ItemUtils.createItemContainer(itemResponse, itemQuery, components);

    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt sprite}.
   *
   * <p>Displays three renders for the chosen item: the placed block sprite, the seed sprite, and
   * the fully grown tree sprite, using URLs from {@link ItemUtils}.
   *
   * @param event the slash interaction
   * @param itemQuery the item to preview
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "sprite",
      description = "Lookup a Growtopia item's sprite.")
  public void onSlashSprite(
      GlobalSlashEvent event,
      @SlashOption(
              description = "The item name you are looking for.",
              autocomplete = ITEM_AUTOCOMPLETE_NAME)
          ItemCatalogue itemQuery) {
    ItemDetailResponse item = wikiService.getItemDetail(itemQuery);
    String itemUrl = ItemUtils.getItemSpriteUrl(item.item().id());
    String seedUrl = ItemUtils.getItemSpriteUrl(item.seed().id());
    String treeUrl = ItemUtils.getTreeSpriteUrl(item.seed().id());

    Container container =
        ItemUtils.createItemContainer(
            item,
            itemQuery,
            MediaGallery.of(
                MediaGalleryItem.fromUrl(itemUrl),
                MediaGalleryItem.fromUrl(seedUrl),
                MediaGalleryItem.fromUrl(treeUrl)));

    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt break}.
   *
   * <p>Validates the item category and block count, then opens a modal to collect modifiers (Lucky
   * mod, Buddy's Block, Ancestral Tesseract level). The calculation itself is performed in {@link
   * SlashBreakModal}.
   *
   * @param event the slash interaction
   * @param itemQuery the block to break
   * @param blockCount number of blocks; must be between 10 and 1,000,000 (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "break",
      description = "Get the amount of items you get from breaking blocks.")
  public void onSlashBreak(
      GlobalSlashEvent event,
      @SlashOption(
              description = "The item name you'd like to break.",
              autocomplete = ITEM_AUTOCOMPLETE_NAME)
          ItemCatalogue itemQuery,
      @SlashOption(description = "How many blocks?") int blockCount) {
    var itemDetail = wikiService.getItemDetail(itemQuery);
    var item = itemDetail.item();
    var category = ItemCategory.fromId(item.categoryInfo().id());

    if (!isValidItemCategory(category)) {
      event.reply("This is not a valid item to break.").queue();
      return;
    } else if (blockCount < 10 || blockCount > 1_000_000) {
      event.reply("Must be at least 10 and no more than 1,000,000 blocks!").queue();
      return;
    }

    var modal =
        modals
            .create("Break %s".formatted(itemQuery.itemName()))
            .addComponents(
                Label.of(
                    "Using Lucky! mod?",
                    RadioGroup.create(SlashBreakModal.INPUT_LUCKY)
                        .addOption("Yes", "True")
                        .addOption("No", "False")
                        .build()),
                Label.of(
                    "Buddy's Block Head?",
                    RadioGroup.create(SlashBreakModal.INPUT_BUDDY)
                        .addOption("Yes", "True")
                        .addOption("No", "False")
                        .build()),
                Label.of(
                    "Ancestral Tesseract level?",
                    TextInput.create(SlashBreakModal.INPUT_ANCES, TextInputStyle.SHORT)
                        .setValue("0")
                        .setRequiredRange(1, 1)
                        .build()))
            .bindTo(SlashBreakModal.MODAL_NAME, itemDetail, itemQuery, blockCount)
            .build();
    event.replyModal(modal).queue();
  }

  private boolean isValidItemCategory(ItemCategory category) {
    return category != ItemCategory.CLOTHES
        && category != ItemCategory.COMPONENTS
        && category != ItemCategory.COMPONENTS_2
        && category != ItemCategory.CONSUMABLES
        && category != ItemCategory.ARTIFACTS
        && category != ItemCategory.BEDROCK
        && category != ItemCategory.FIST
        && category != ItemCategory.WRENCH;
  }

  /**
   * Handles {@code /gt harvest}.
   *
   * <p>Validates the item category and tree count, performs the calculation for harvesting trees
   * using all modifiers.
   *
   * @param event the slash interaction
   * @param itemQuery the block to break
   * @param treeCount number of blocks; must be between 10 and 1,000,000 (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "harvest",
      description = "Get the amount of items you get from harvesting trees.")
  public void onSlashHarvest(
      GlobalSlashEvent event,
      @SlashOption(
              description = "The item name you'd like to break.",
              autocomplete = ITEM_AUTOCOMPLETE_NAME)
          ItemCatalogue itemQuery,
      @SlashOption(description = "How many trees?") int treeCount) {
    if (treeCount < 10 || treeCount > 1_000_000) {
      event.reply("Must be at least 10 and no more than 1,000,000 trees!").queue();
      return;
    }
    var itemDetail = wikiService.getItemDetail(itemQuery);
    var item = itemDetail.item();
    if (!item.canHaveTrees()) {
      event.reply("This item cannot have trees").queue();
      return;
    }

    List<ContainerChildComponent> components = new ArrayList<>();
    String treeCountFormatted = String.format(Locale.US, "%,d", treeCount);
    components.add(
        TextDisplay.of("### Harvesting %s %s Trees.".formatted(treeCountFormatted, item.name())));

    boolean isFarmable = fruitService.getMaxDrop(item.id()) > 4;
    if (isFarmable) {
      components.add(TextDisplay.of("%s Item is farmable.".formatted(AppEmojis.TRACTOR)));
    } else {
      components.add(TextDisplay.of("%s Item is **not** farmable.".formatted(AppEmojis.TRACTOR)));
    }

    double treeDropCount = isFarmable ? treeCount * 3.75 : treeCount * 2.5;
    String treeDropCountFormatted = String.format(Locale.US, "%,.0f", treeDropCount);
    components.add(
        TextDisplay.of(
            "%s Block drops: ~**%s**".formatted(AppEmojis.DIRT_BLOCK, treeDropCountFormatted)));

    double fuelTotal = treeDropCount * 1.1;
    String fuelTotalFormatted = String.format(Locale.US, "%,.0f", fuelTotal);
    var extraBlocksHarvesterAvg = Math.floor(treeDropCount * 1.10) - treeDropCount;
    String extraBlocksHarvesterAvgFormatted =
        String.format(Locale.US, "%,.0f", extraBlocksHarvesterAvg);
    String consumedFuelPacksFormatted = String.format(Locale.US, "%,d", treeCount / 10);
    components.add(
        TextDisplay.of(
            "%s Extra block drops with harvester: `~%s` (Total `~%s`) (**%s fuel packs consumed**)"
                .formatted(
                    AppEmojis.FUEL,
                    extraBlocksHarvesterAvgFormatted,
                    fuelTotalFormatted,
                    consumedFuelPacksFormatted)));

    double gemDropAvg = ItemUtils.getAverageGemCountToDropOnTreeSmash(item);
    if (gemDropAvg == 0) {
      components.add(TextDisplay.of("%s No gem drops.".formatted(AppEmojis.GEM)));
    } else {
      double totalGemDrop = treeCount * gemDropAvg;
      String totalGemFormatted = String.format(Locale.US, "%,.0f", totalGemDrop);
      String gemDropAvgFormatted = String.format(Locale.US, "%,.0f", gemDropAvg);
      components.add(
          TextDisplay.of(
              "%s Gem drops: `~%s` (`~%s` per tree)"
                  .formatted(AppEmojis.GEM, totalGemFormatted, gemDropAvgFormatted)));
    }

    var properties = ItemProperty.fromInt(item.propFlag().raw());
    if (properties.contains(ItemProperty.NO_SEED)) {
      components.add(
          TextDisplay.of("%s No seed - Tree does not drop seeds.".formatted(AppEmojis.NO_SEED)));
    } else {
      double chanceSeedDrop = ItemUtils.getChanceToDropSeedOnTreeSmash(item.rarity());
      int totalSeedDrop = (int) (treeCount / (100 / chanceSeedDrop));

      String chanceSeedDropFormatted = String.format(Locale.US, "%,.0f", chanceSeedDrop);
      String totalSeedDropFormatted = String.format(Locale.US, "%,d", totalSeedDrop);
      components.add(
          TextDisplay.of(
              "%s Seed drops: `%s` seeds. (`~%s`%% chance.)"
                  .formatted(
                      AppEmojis.DIRT_SEED, totalSeedDropFormatted, chanceSeedDropFormatted)));

      var totalSeedsEarned = Math.round(treeDropCount / 4) + totalSeedDrop;
      double blockBlockDrop = treeDropCount / 12;
      double blockBlockDropHarvester = blockBlockDrop * 1.10;

      String totalSeedsEarnedFormatted = String.format(Locale.US, "%,d", totalSeedsEarned);
      String blockBlockDropFormatted = String.format(Locale.US, "%,.0f", blockBlockDrop);
      components.add(
          TextDisplay.of(
              ("`~%s` seeds after harvesting and breaking blocks from trees, "
                      + "`%s` block drop from blocks.")
                  .formatted(totalSeedsEarnedFormatted, blockBlockDropFormatted)));
      String harvesterFuelTotalFormatted = String.format(Locale.US, "%,.0f", fuelTotal / 4);
      String blockBlockDropHarvesterFormatted =
          String.format(Locale.US, "%,.0f", blockBlockDropHarvester);
      components.add(
          TextDisplay.of(
              "%s Harvester: `~%s` seeds + `%s` blocks after breaking, using harvester."
                  .formatted(
                      AppEmojis.FUEL,
                      harvesterFuelTotalFormatted,
                      blockBlockDropHarvesterFormatted)));

      int secondsToHarvest = Math.toIntExact(Math.round(treeCount / 3.61111));
      String timeToHarvest = ItemUtils.toDayHourMinutesSeconds(secondsToHarvest);
      components.add(
          TextDisplay.of(
              "%s Approximately `%s` to harvest all the trees."
                  .formatted(AppEmojis.TICKING_CLOCK, timeToHarvest)));

      String finalNoHarvesterFormatted =
          String.format(Locale.US, "%,.0f", totalSeedsEarned + (blockBlockDrop / 4));
      String finalHarvesterFormatted =
          String.format(
              Locale.US, "%,.0f", (totalSeedsEarned * 1.10) + (blockBlockDropHarvester / 4));
      components.add(
          TextDisplay.of(
              "### %s TOTAL: %s seeds after one cycle, ~%s with harvester."
                  .formatted(
                      AppEmojis.CHECKBOX_ENABLED,
                      finalNoHarvesterFormatted,
                      finalHarvesterFormatted)));
    }

    Container container = ItemUtils.createItemContainer(itemDetail, itemQuery, components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt wotd}.
   *
   * <p>Fetches today's World of the Day from {@link GrowtopiaDetailClient}, then replies with a
   * title and a full-size render from the URL configured in {@link LockiumProperties#renderUrl()}.
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "wotd",
      description = "Render today's World of the Day.")
  public void onSlashWotd(GlobalSlashEvent event) {
    var detail = detailClient.getGrowtopiaDetail();
    String wotd = detail.wotd().fullSize().substring(7);
    int dotIndex = wotd.indexOf(".");

    String renderUrl = lockiumProperties.renderUrl();
    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of(
                "## %s WOTD: %s"
                    .formatted(AppEmojis.WOTD, wotd.substring(0, dotIndex).toUpperCase())),
            MediaGallery.of(MediaGalleryItem.fromUrl(renderUrl + wotd.toLowerCase())));

    event.replyComponents(container).useComponentsV2().queue();
  }
}
