package dev.skullition.lockium.command;

import static dev.skullition.lockium.handler.ItemNameAutocompleteHandler.ITEM_AUTOCOMPLETE_NAME;

import dev.skullition.lockium.client.GrowtopiaDetailClient;
import dev.skullition.lockium.modal.SlashBreakModal;
import dev.skullition.lockium.model.GrowtopiaDetail;
import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemCategory;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemProperty;
import dev.skullition.lockium.model.ItemProperty2;
import dev.skullition.lockium.model.RoleType;
import dev.skullition.lockium.properties.LockiumProperties;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.service.WorldRenderService;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import dev.skullition.lockium.util.GrowtopiaTimeUtil;
import dev.skullition.lockium.util.ItemUtils;
import dev.skullition.lockium.util.RecycleUtil;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import io.github.freya022.botcommands.api.modals.Modals;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.jspecify.annotations.Nullable;
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
  /** The day Growtopia was released. */
  private static final LocalDate GROWTOPIA_RELEASE_DATE = LocalDate.of(2012, 11, 30);
  /** The maximum level reachable in-game. */
  private static final int MAX_GT_LEVEL = 125;
  /** Daily block drop rotation, indexed by day of week (Sunday = 0). */
  private static final String[] DAILY_BLOCKS = {
    "Anemone", "Aurora", "Obsidian", "Lava Lamp", "Fissure", "Waterfall", "Hidden Door"
  };
  /** Maximum amount of matches shown by {@code /gt search}. */
  private static final int MAX_SEARCH_RESULTS = 20;
  /** Valid Growtopia world names: 1-25 letters, digits, or underscores. */
  private static final Pattern WORLD_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,25}");
  /** Worlds rendered before this date have no reliable {@code Last-Modified} header. */
  private static final Instant INITIAL_RENDER_TIME =
      LocalDate.of(2018, 7, 27).atStartOfDay(GrowtopiaTimeUtil.GROWTOPIA_ZONE).toInstant();
  private final Modals modals;
  private final WikiService wikiService;
  private final GrowtopiaDetailService detailService;
  private final LockiumProperties lockiumProperties;
  private final TreeFruitService fruitService;
  private final WorldRenderService worldRenderService;

  /**
   * Creates the command handler.
   *
   * @param modals modal manager for interactive flows
   * @param wikiService cached access to the wiki item API
   * @param detailService client for live Growtopia data (WOTD)
   * @param lockiumProperties application configuration, including render URLs
   * @param fruitService service used to determine an item is farmable
   * @param worldRenderService service used to look up world renders
   */
  public GtCommands(
      Modals modals,
      WikiService wikiService,
      GrowtopiaDetailService detailService,
      LockiumProperties lockiumProperties,
      TreeFruitService fruitService,
      WorldRenderService worldRenderService) {
    this.modals = modals;
    this.wikiService = wikiService;
    this.detailService = detailService;
    this.lockiumProperties = lockiumProperties;
    this.fruitService = fruitService;
    this.worldRenderService = worldRenderService;
  }

  private static String formatNumber(long value) {
    return String.format(Locale.US, "%,d", value);
  }

  /**
   * Builds the Daily Challenge status line.
   *
   * <p>The challenge runs on a 25-hour cycle - it starts one hour later every day - and lasts 2
   * hours.
   *
   * @param now the current Growtopia time
   * @return {@code "Ends <t:..:R>"} while running, otherwise {@code "Starts <t:..:F>"}
   */
  private static String dailyChallengeText(ZonedDateTime now) {
    final long msInHour = 3_600_000L;
    long nowMs = now.toInstant().toEpochMilli();
    long offsetHours = now.getOffset().getTotalSeconds() / 3600;
    long startMs =
        nowMs + 25 * msInHour - ((nowMs + 7 * msInHour) % (25 * msInHour)) + offsetHours * msInHour;
    if (startMs - nowMs > 23 * msInHour) {
      startMs -= 25 * msInHour;
    }
    if (startMs < nowMs) {
      return "Ends <t:%d:R>".formatted((startMs + 2 * msInHour) / 1000);
    }
    return "Starts <t:%d:F>".formatted(startMs / 1000);
  }

  /**
   * Builds the status line of an event that starts on a fixed day of every month.
   *
   * @param now the current Growtopia time
   * @param dayOfMonth day of the month the event starts on
   * @param durationDays how many days the event lasts
   * @param startStyle Discord timestamp style used for the start time (e.g. {@code 'F'}, {@code
   *     'R'})
   * @return {@code "Ends <t:..:R>"} while running, otherwise {@code "Starts <t:..:style>"}
   */
  private static String monthlyEventText(
      ZonedDateTime now, int dayOfMonth, int durationDays, char startStyle) {
    ZonedDateTime start =
        now.toLocalDate().withDayOfMonth(dayOfMonth).atStartOfDay(GrowtopiaTimeUtil.GROWTOPIA_ZONE);
    if (now.isAfter(start.plusDays(durationDays))) {
      start = start.plusMonths(1);
    }
    if (!now.isBefore(start) && now.isBefore(start.plusDays(durationDays))) {
      return "Ends <t:%d:R>".formatted(start.plusDays(durationDays).toEpochSecond());
    }
    return "Starts <t:%d:%c>".formatted(start.toEpochSecond(), startStyle);
  }

  private static void appendXpBlock(
      StringBuilder sb, String hits, long totalXp, int xpPerBreak, String blockName) {
    long breaks = (long) Math.ceil((double) totalXp / xpPerBreak);
    sb.append("[%s] **%s** %s.\n".formatted(hits, formatNumber(breaks), blockName));
  }

  /**
   * Extracts the upper-cased World of the Day name from a detail payload.
   *
   * <p>The API returns a relative image path such as {@code worlds/thedragonattacks.png}.
   *
   * @param detail the detail payload
   * @return the world name, e.g. {@code THEDRAGONATTACKS}
   */
  private static String wotdName(GrowtopiaDetail detail) {
    String wotd = detail.wotd().fullSize().substring(7);
    return wotd.substring(0, wotd.indexOf(".")).toUpperCase(Locale.US);
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
   * Handles {@code /gt recycle}.
   *
   * <p>Calculates how many gems you'd receive for recycling an item. Currently only supports items
   * with rarity (rarity != 999).
   *
   * @param event the slash interaction
   * @param itemQuery the item to recycle
   * @param itemCount number of items; must be between 10 and 100,000 (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "recycle",
      description = "Gives you gem values for recycling an item.")
  public void onSlashRecycle(
      GlobalSlashEvent event,
      @SlashOption(
              description = "The item name you'd like to break.",
              autocomplete = ITEM_AUTOCOMPLETE_NAME)
          ItemCatalogue itemQuery,
      @SlashOption(description = "How many items?") int itemCount) {
    if (itemCount < 1 || itemCount > 100_000) {
      event.reply("Must be between 1 and 100.000 items.").queue();
      return;
    }
    var itemDetail = wikiService.getItemDetail(itemQuery);
    var item = itemDetail.item();
    if (item.rarity() == 999) {
      event.reply("Items with no rarity are currently not supported.").queue();
      return;
    }

    List<ContainerChildComponent> components = new ArrayList<>();

    String itemCountFormatted = String.format(Locale.US, "%,d", itemCount);
    components.add(
        TextDisplay.of("### Recycling %s %s".formatted(itemCountFormatted, item.name())));
    // Could also check for !ItemProperties.NO_SEED
    boolean isFarmable = fruitService.getMaxDrop(item.id()) > 4;
    if (isFarmable) {
      components.add(TextDisplay.of("%s Item is farmable.".formatted(AppEmojis.TRACTOR)));
    } else {
      components.add(TextDisplay.of("%s Item is **not** farmable.".formatted(AppEmojis.TRACTOR)));
    }
    var recycleResult = RecycleUtil.getRecycleValueForItem(itemDetail, itemCount);
    components.add(TextDisplay.of("%s Rarity: %s".formatted(AppEmojis.RARITY, item.rarity())));
    components.add(
        TextDisplay.of(
            "%s Gems per item: %s - %s"
                .formatted(AppEmojis.GEM, recycleResult.rangeMin(), recycleResult.rangeMax())));
    String gemCountFormatted = String.format(Locale.US, "%,d", recycleResult.gemCount());
    components.add(
        TextDisplay.of(
            "### %s Total gems: `~%s`".formatted(AppEmojis.CHECKBOX_ENABLED, gemCountFormatted)));

    Container container = ItemUtils.createItemContainer(itemDetail, itemQuery, components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt mooncakes}.
   *
   * <p>Calculates the expected mooncake drops from harvesting trees during the Lunar New Year
   * event. A tree's chance to drop a mooncake is {@code rarity / 150}, and roughly one in a
   * hundred drops is a balance mooncake.
   *
   * @param event the slash interaction
   * @param itemQuery the tree's item
   * @param treeCount number of trees; must be between 1 and 500,000 (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "mooncakes",
      description = "Calculate the chance to get mooncakes.")
  public void onSlashMooncakes(
      GlobalSlashEvent event,
      @SlashOption(
              description = "The item name of the trees you'd like to harvest.",
              autocomplete = ITEM_AUTOCOMPLETE_NAME)
          ItemCatalogue itemQuery,
      @SlashOption(description = "How many trees?") int treeCount) {
    if (treeCount < 1 || treeCount > 500_000) {
      event.reply("Tree count must be between 1 and 500,000.").queue();
      return;
    }
    var itemDetail = wikiService.getItemDetail(itemQuery);
    var item = itemDetail.item();
    if (item.rarity() == 999) {
      event.reply("Trees with no rarity do not drop any cakes.").queue();
      return;
    }

    double dropChance = item.rarity() / 150.0;
    long totalMooncakes = (long) (treeCount * dropChance);
    long balanceMooncakes = totalMooncakes / 100;
    long normalMooncakes = totalMooncakes - balanceMooncakes;

    List<ContainerChildComponent> components = new ArrayList<>();
    String treeCountFormatted = String.format(Locale.US, "%,d", treeCount);
    components.add(
        TextDisplay.of(
            "### Harvesting %s %s Trees (Rarity %d)"
                .formatted(treeCountFormatted, item.name(), item.rarity())));
    components.add(
        TextDisplay.of(
            """
            Total: `~%s` mooncakes
            ▫ Of which `~%s` should be colored mooncakes (`~%s` of each color)
            ▫ And `~%s` are balance mooncakes\
            """
                .formatted(
                    formatNumber(totalMooncakes),
                    formatNumber(normalMooncakes),
                    formatNumber(normalMooncakes / 4),
                    formatNumber(balanceMooncakes))));
    components.add(Separator.create(true, Separator.Spacing.SMALL));

    String growTime = ItemUtils.toDayHourMinutesSeconds(itemDetail.seed().growTime());
    components.add(
        TextDisplay.of(
            """
            **ℹ Additional Info**
            ▫ Tree takes `%s` to grow.
            ▫ Roughly `%.2f` farms. (2,500 trees/farm)
            ▫ A tree has `%.1f%%` chance to drop a cake.\
            """
                .formatted(growTime, treeCount / 2500.0, dropChance * 100)));

    Container container = ItemUtils.createItemContainer(itemDetail, itemQuery, components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt role}. Shows the XP curve (per level) and gem-cost curve (per daily quest
   * count) for the chosen role.
   *
   * @param event the slash interaction
   * @param role the chosen role; never {@code null} since it comes from predefined enum choices
   */
  @JDASlashCommand(
          name = "gt",
          subcommand = "role",
          description = "Check role XP / gems needed for roles.")
  public void onSlashRole(
          GlobalSlashEvent event,
          @SlashOption(description = "Which role to choose.", usePredefinedChoices = true)
          RoleType role) {
    final String roleName = role.getRoleName();
    final int baseXp = role.getBaseXp();
    final int baseGem = role.getBaseGemCost();

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(TextDisplay.of("## %s %s".formatted(role.getEmoji(), roleName)));
    components.add(Separator.create(true, Separator.Spacing.LARGE));

    StringBuilder xp = new StringBuilder("### Experience to reach Level\n");
    long totalXp = 0;
    for (int level = 1; level <= 10; level++) {
      long levelXp = (long) baseXp * level * level;
      totalXp += levelXp;
      xp.append(
              "▫ Lv.**%d**: `%s` xp required (Total `%s`)\n"
                      .formatted(level, formatNumber(levelXp), formatNumber(totalXp)));
    }
    components.add(TextDisplay.of(xp.toString()));
    components.add(Separator.create(true, Separator.Spacing.SMALL));

    StringBuilder gems = new StringBuilder("### Quest Costs per Day\n");
    long totalGem = 0;
    for (int quests = 1; quests <= 10; quests++) {
      // First quest is free
      long gemCost = (long) baseGem * (quests - 1) * (quests - 1);
      totalGem += gemCost;
      gems.append(
              "▫ **%d** quest(s): `%s` gems (Total `%s`)\n"
                      .formatted(quests, formatNumber(gemCost), formatNumber(totalGem)));
    }
    components.add(TextDisplay.of(gems.toString()));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt events}.
   *
   * <p>Shows when the recurring in-game events start or end, as localized Discord timestamps:
   *
   * <ul>
   *   <li><b>Daily Challenge</b> – runs on a 25-hour cycle (shifts one hour later every day) and
   *       lasts 2 hours
   *   <li><b>Night of the Comet</b> – the 28th of every month, for one day
   *   <li><b>Pet Battle Tournament</b> – the 7th of every month, for five days
   *   <li><b>Daily Block Drop</b> – rotation of the block dropped by breaking any block that day
   * </ul>
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "events",
      description = "Check when the Growtopia events will occur.")
  public void onSlashEvents(GlobalSlashEvent event) {
    ZonedDateTime now = GrowtopiaTimeUtil.now();
    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        TextDisplay.of(
            "### %s %s".formatted(AppEmojis.TICKING_CLOCK, GrowtopiaTimeUtil.nowString())));
    components.add(Separator.create(true, Separator.Spacing.LARGE));

    components.add(
        TextDisplay.of(
            "%s **Daily Challenge**: %s"
                .formatted(AppEmojis.CHALLENGE_BOARD, dailyChallengeText(now))));
    components.add(
        TextDisplay.of(
            "☄️ **Night Of The Comet**: %s".formatted(monthlyEventText(now, 28, 1, 'R'))));
    components.add(
        TextDisplay.of(
            "%s **Pet Battle Tournament**: %s"
                .formatted(AppEmojis.BATTLE_LEASH, monthlyEventText(now, 7, 5, 'F'))));

    // Daily Block Drop rotation: yesterday > today > tomorrow.
    StringBuilder rotation = new StringBuilder();
    for (int i = -1; i <= 1; i++) {
      String block = DAILY_BLOCKS[now.plusDays(i).getDayOfWeek().getValue() % 7];
      rotation.append(i == 0 ? "**" + block + "**" : block);
      if (i < 1) {
        rotation.append(" > ");
      }
    }
    components.add(
        TextDisplay.of("%s **Daily Block Drop**: %s".formatted(AppEmojis.FIST, rotation)));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt telephone}.
   *
   * <p>Lists all telephone numbers that can be dialed on an in-game Telephone.
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "telephone",
      description = "Get all the available telephone numbers.")
  public void onSlashTelephone(GlobalSlashEvent event) {
    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of("### ☎️ All Telephone Numbers"),
            Separator.create(true, Separator.Spacing.LARGE),
            TextDisplay.of(
                """
                ▫ `00000` - Operator
                ▫ `02893` - Growtech Pharma
                ▫ `12345` - Crazy Jim
                ▫ `30912` - Growtopizza
                ▫ `41100`-`41199` - Warehouse
                ▫ `53785` - Sales-Man
                ▫ `77777` - Nobody
                ▫ `90210` - Beverly Hill\
                """));
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt xp}.
   *
   * <p>Sums the XP required between two levels using the in-game formula {@code 50 * (level² + 2)}
   * per level, then lists how many hits of common XP blocks that equals, along with the XP
   * multipliers of consumables and equipment.
   *
   * @param event the slash interaction
   * @param minLevel starting level; defaults to 1
   * @param maxLevel target level; defaults to {@value #MAX_GT_LEVEL}
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "xp",
      description = "Calculate experience needed for levels.")
  public void onSlashXp(
      GlobalSlashEvent event,
      @SlashOption(name = "min_level", description = "Minimum level - i.e 1.") @Nullable
          Integer minLevel,
      @SlashOption(name = "max_level", description = "Maximum level - i.e 125.") @Nullable
          Integer maxLevel) {
    int min = minLevel == null ? 1 : minLevel;
    int max = maxLevel == null ? MAX_GT_LEVEL : maxLevel;
    if (min < 0 || max < 0 || max > MAX_GT_LEVEL + 1 || min > max) {
      event
          .reply("Invalid input. Min level must be above 0, max level below %d!"
                  .formatted(MAX_GT_LEVEL))
          .queue();
      return;
    }

    long totalXp = 0;
    for (int level = min; level < max; level++) {
      totalXp += 50L * ((long) level * level + 2);
    }

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        TextDisplay.of(
            "### XP required from level %d to %d: %s"
                .formatted(min, max, formatNumber(totalXp))));
    components.add(Separator.create(true, Separator.Spacing.LARGE));

    StringBuilder hits = new StringBuilder("**[Hits] Blocks to break:**\n");
    appendXpBlock(hits, "7-6", totalXp, 18, "Chandeliers/Sorcerer Stones");
    appendXpBlock(hits, "4-3", totalXp, 10, "Pepper Trees");
    appendXpBlock(hits, "3-3", totalXp, 9, "Fish Tanks");
    appendXpBlock(hits, "5-4", totalXp, 14, "Laser Grids");
    appendXpBlock(hits, "7-6", totalXp, 11, "Pinball Bumpers");
    appendXpBlock(hits, "6-5", totalXp, 11, "Floor Grills");
    appendXpBlock(hits, "10-8", totalXp, 20, "Magic Bells");
    components.add(TextDisplay.of(hits.toString()));
    components.add(Separator.create(true, Separator.Spacing.SMALL));

    components.add(
        TextDisplay.of(
            """
            **Divide by:**
            ▫ `x1.50` if using Biotronic Brain Enhancer.
            ▫ `x1.25` with Egg Benedict.
            ▫ `x1.10` with Ring Of Wisdom.
            ▫ `x1.05-1.10` with Ancestral Totem of Wisdom. (+1% per level)
            ▫ `x1.01` with Premium Subscription.\
            """));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt startdate}.
   *
   * <p>Converts an account's age in days (visible when wrenching yourself in-game) to the calendar
   * date the account was created, rendered as a localized Discord timestamp.
   *
   * @param event the slash interaction
   * @param days days since the account was created; must not exceed the game's age
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "startdate",
      description = "Tells you when you started playing based on account days.")
  public void onSlashStartDate(
      GlobalSlashEvent event,
      @SlashOption(description = "Days since the account was created (wrench yourself in-game).")
          int days) {
    long daysSinceRelease =
        ChronoUnit.DAYS.between(
            GROWTOPIA_RELEASE_DATE, LocalDate.now(GrowtopiaTimeUtil.GROWTOPIA_ZONE));
    if (days < 0 || days > daysSinceRelease) {
      event
          .reply(
              "Can't be more days than the game has been online for! (%d)"
                  .formatted(daysSinceRelease))
          .queue();
      return;
    }

    long startDateEpochSeconds =
        LocalDate.now(GrowtopiaTimeUtil.GROWTOPIA_ZONE)
            .minusDays(days)
            .atStartOfDay(GrowtopiaTimeUtil.GROWTOPIA_ZONE)
            .toEpochSecond();
    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of(
                "%s Start Date: <t:%d:D>"
                    .formatted(AppEmojis.TICKING_CLOCK, startDateEpochSeconds)));
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt time}.
   *
   * <p>Replies with the current Growtopia (US Eastern) time from {@link GrowtopiaTimeUtil}.
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "time",
      description = "Check the current Growtopia time.")
  public void onSlashTime(GlobalSlashEvent event) {
    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of(
                "%s %s".formatted(AppEmojis.TICKING_CLOCK, GrowtopiaTimeUtil.nowString())));
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt search}.
   *
   * <p>Performs a case-insensitive contains-search over all cached item and seed names from the
   * wiki index, listing up to {@value #MAX_SEARCH_RESULTS} matches alphabetically.
   *
   * @param event the slash interaction
   * @param query partial item name; must be at least 3 characters
   */
  @JDASlashCommand(name = "gt", subcommand = "search", description = "Search items by name.")
  public void onSlashSearch(
      GlobalSlashEvent event,
      @SlashOption(description = "Partial item name to search for.") String query) {
    String needle = ItemUtils.norm(query);
    if (needle.length() < 3) {
      event.reply("Search query must be at least 3 characters.").queue();
      return;
    }

    List<String> matches =
        wikiService.getNameIndex().keySet().stream()
            .filter(name -> ItemUtils.norm(name).contains(needle))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    if (matches.isEmpty()) {
      event.reply("No items found matching `%s`.".formatted(query)).queue();
      return;
    }

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        TextDisplay.of("### 🔍 %d result(s) for \"%s\"".formatted(matches.size(), query)));
    components.add(Separator.create(true, Separator.Spacing.LARGE));

    StringBuilder names = new StringBuilder();
    matches.stream()
        .limit(MAX_SEARCH_RESULTS)
        .forEach(name -> names.append("▫ ").append(name).append('\n'));
    components.add(TextDisplay.of(names.toString()));

    if (matches.size() > MAX_SEARCH_RESULTS) {
      components.add(
          TextDisplay.of(
              "-# Showing the first %d matches, refine your search to see the rest."
                  .formatted(MAX_SEARCH_RESULTS)));
    }

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt world}.
   *
   * <p>Looks up a world render on the official S3 bucket via {@link WorldRenderService} and shows
   * it with the time it was last rendered. Worlds only have renders after their owner runs {@code
   * /renderworld} in-game.
   *
   * @param event the slash interaction
   * @param worldName the world to look up; letters, digits, and underscores only
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "world",
      description = "Search a Growtopia world render from growtopiagame.com.")
  public void onSlashWorld(
      GlobalSlashEvent event,
      @SlashOption(name = "world_name", description = "World name to find a render for.")
          String worldName) {
    if (!WORLD_NAME_PATTERN.matcher(worldName).matches()) {
      event.reply("World names can only contain letters/numbers/underscores.").queue();
      return;
    }

    var render = worldRenderService.fetchWorldRender(worldName);
    if (render.isEmpty()) {
      event
          .reply(
              "That world does not seem to be rendered. "
                  + "If you own that world, do `/renderworld` in-game!")
          .queue();
      return;
    }

    String renderedText;
    Instant lastModified = render.get().lastModified();
    if (lastModified == null || lastModified.isBefore(INITIAL_RENDER_TIME)) {
      renderedText = "-# Unknown render date (before 27/07/2018).";
    } else {
      renderedText = "-# Last rendered: <t:%d:F>.".formatted(lastModified.getEpochSecond());
    }

    // Cache-buster so Discord always fetches the latest render.
    String imageUrl =
        "%s?at=%d".formatted(render.get().url(), ThreadLocalRandom.current().nextInt(1_000_000));
    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of(
                "## %s World | %s"
                    .formatted(AppEmojis.EARTH, worldName.toUpperCase(Locale.US))),
            MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)),
            TextDisplay.of(renderedText));
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
    var detail = detailService.getDetail();
    if (detail == null) {
      event
          .reply("Unexpected error while trying to query WOTD data. Please try again later.")
          .queue();
      return;
    }

    String wotd = detail.wotd().fullSize().substring(7);

    String renderUrl = lockiumProperties.renderUrl();
    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of("## %s WOTD: %s".formatted(AppEmojis.WOTD, wotdName(detail))),
            MediaGallery.of(MediaGalleryItem.fromUrl(renderUrl + wotd.toLowerCase())));

    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt stats}.
   *
   * <p>Shows the current Growtopia time, a server status derived from the online user count, the
   * amount of online users, and today's World of the Day.
   *
   * @param event the slash interaction
   */
  @JDASlashCommand(name = "gt", subcommand = "stats", description = "Game server stats.")
  public void onSlashStats(GlobalSlashEvent event) {
    var detail = detailService.getDetail();
    if (detail == null) {
      event
          .reply("Failed to fetch data from the Growtopia servers. Please try again later.")
          .queue();
      return;
    }

    int onlineUsers;
    try {
      onlineUsers = Integer.parseInt(detail.onlineUsers());
    } catch (NumberFormatException e) {
      logger.warn("stats: invalid online_user value: {}", detail.onlineUsers());
      event.reply("Server API sent invalid data, in maintenance?").queue();
      return;
    }

    String status;
    if (onlineUsers <= 0) {
      status = "❌ (Server is down.)";
    } else if (onlineUsers < 15) {
      status = "🚫 (Maintenance mode - unable to enter.)";
    } else if (onlineUsers < 1000) {
      status = "⚠️ (Server is initializing.)";
    } else {
      status = "🆙";
    }

    var container =
        ContainerUtil.createGenericContainer(
            TextDisplay.of(
                "### %s %s".formatted(AppEmojis.TICKING_CLOCK, GrowtopiaTimeUtil.nowString())),
            Separator.create(true, Separator.Spacing.LARGE),
            TextDisplay.of(
                """
                🖥️ **Server Status:** %s
                👤 **Online Users:** `%s`
                %s **WOTD:** %s\
                """
                    .formatted(
                        status,
                        String.format(Locale.US, "%,d", onlineUsers),
                        AppEmojis.WOTD,
                        wotdName(detail))));
    event.replyComponents(container).useComponentsV2().queue();
  }
}
