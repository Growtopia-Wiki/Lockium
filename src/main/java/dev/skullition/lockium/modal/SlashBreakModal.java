package dev.skullition.lockium.modal;

import static dev.skullition.lockium.util.FormatUtil.formatNumber;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemProperty;
import dev.skullition.lockium.model.ItemProperty2;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import io.github.freya022.botcommands.api.modals.ModalEvent;
import io.github.freya022.botcommands.api.modals.annotations.ModalData;
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler;
import io.github.freya022.botcommands.api.modals.annotations.ModalInput;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

/**
 * Modal handler for the {@code /break} command.
 *
 * <p>Calculates the expected returns from smashing a farmable block, including base block drops,
 * seed drops, and gem drops. The calculation respects item flags ({@link ItemProperty#NO_SEED},
 * {@link ItemProperty#NO_DROP}, {@link ItemProperty2#GEMLESS}) and optional player buffs:
 *
 * <ul>
 *   <li>Lucky! – 10% chance, 5× gems, guaranteed block return; not clothing, never capped
 *   <li>Buddy's Block Head – +2% blocks
 *   <li>Galaxy Skin – +10% blocks
 *   <li>Winter Wishing Star – +2% blocks
 *   <li>Ancestral Tesseract – +5% to +10% blocks depending on level
 * </ul>
 *
 * <p>Clothing block bonuses (all of the above except Lucky!) are capped at {@value
 * #CLOTHING_BONUS_CAP}% in-game. Due to an in-game oversight, the Winter Wishing Star's 2% is
 * added <em>after</em> the cap check, so the actual observed maximum is 17%. This handler
 * replicates that behavior.
 *
 * <p>Data is passed from the slash command via {@code @ModalData} to avoid re-fetching the Wiki
 * API. Results are rendered using Discord Components V2.
 */
@Handler
public class SlashBreakModal {
  public static final String MODAL_NAME = "SlashBreak: break";
  public static final String INPUT_LUCKY = "SlashBreak: lucky";
  public static final String INPUT_CLOTHING = "SlashBreak: clothing";
  public static final String INPUT_ANCES = "SlashBreak: ances";

  /** Select-menu value for Buddy's Block Head (+2% blocks). */
  public static final String CLOTHING_BBH = "BBH";

  /** Select-menu value for Galaxy Skin (+10% blocks). */
  public static final String CLOTHING_GALAXY = "GALAXY";

  /** Select-menu value for Winter Wishing Star (+2% blocks). */
  public static final String CLOTHING_STAR = "STAR";

  /** In-game cap on clothing block bonuses, in percent. The Winter Wishing Star bypasses it. */
  private static final int CLOTHING_BONUS_CAP = 15;

  private final TreeFruitService fruitService;

  /**
   * Creates a new handler.
   *
   * @param fruitService service used to determine if an item is farmable via {@link
   *     TreeFruitService#getMaxDrop(int)}
   */
  public SlashBreakModal(TreeFruitService fruitService) {
    this.fruitService = fruitService;
  }

  /**
   * Handles the break-calculation modal submission.
   *
   * @param event the modal interaction to reply to
   * @param itemDetail the full item data from the Wiki API, supplied via modal data
   * @param itemCatalogue the catalogue entry for display purposes, supplied via modal data
   * @param blockCount the number of blocks the user intends to break, supplied via modal data
   * @param luckyString "True" if Lucky! is active, otherwise "False"
   * @param clothing selected clothing bonus values ({@link #CLOTHING_BBH}, {@link
   *     #CLOTHING_GALAXY}, {@link #CLOTHING_STAR}); empty if none are worn
   * @param ancesString the Ancestral Tesseract level as a string (0–6)
   */
  @ModalHandler(MODAL_NAME)
  public void onBreakModal(
      ModalEvent event,
      @ModalData ItemDetailResponse itemDetail,
      @ModalData ItemCatalogue itemCatalogue,
      @ModalData int blockCount,
      @ModalInput(INPUT_LUCKY) String luckyString,
      @ModalInput(INPUT_CLOTHING) List<String> clothing,
      @ModalInput(INPUT_ANCES) String ancesString) {
    final boolean lucky = parseBoolean(luckyString);
    final boolean buddy = clothing.contains(CLOTHING_BBH);
    final boolean galaxy = clothing.contains(CLOTHING_GALAXY);
    final boolean star = clothing.contains(CLOTHING_STAR);
    int ances = tryParseInt(ancesString, event);
    if (ances == -1) {
      return;
    } else if (ances > 6) {
      event
          .reply("The maximum level of Ancestral Tesseract of Dimensions is 6.")
          .setEphemeral(true)
          .queue();
      return;
    }

    GrowtopiaObject item = itemDetail.item();
    List<ContainerChildComponent> components = new ArrayList<>();

    boolean isFarmable = fruitService.getMaxDrop(item.id()) > 4;
    if (isFarmable) {
      components.add(TextDisplay.of("%s Item is farmable.".formatted(AppEmojis.TRACTOR)));
    } else {
      components.add(TextDisplay.of("%s Item is **not** farmable.".formatted(AppEmojis.TRACTOR)));
    }

    var props = ItemProperty.fromInt(item.propFlag().raw());
    var props2 = ItemProperty2.fromInt(item.propFlag2().raw());
    final boolean dropsSeeds = !props.contains(ItemProperty.NO_SEED);
    final boolean dropsItself = !props.contains(ItemProperty.NO_DROP);
    final boolean dropsGems = !props2.contains(ItemProperty2.GEMLESS);

    // Will always be 25% (unless NoSeed of course)
    float baseSeedDrop = 0;
    if (dropsSeeds) {
      baseSeedDrop = blockCount * 0.25f;
    }
    float baseBlockDrop = 0;
    double gemDrops;
    if (lucky || buddy || galaxy || star || ances != 0) {
      components.add(TextDisplay.of("### Optional Items Used:"));
    }

    if (lucky) {
      components.add(
          TextDisplay.of(
              """
                      %s **Lucky! effect** (10%% chance of triggering) - Increased chance of block drops and 5x gems.
              """
                  .formatted(AppEmojis.LUCKY_CLOVER)));
      int luckyBlocks = blockCount / 10; // 10%
      int normalBlocks = blockCount - luckyBlocks;
      if (dropsItself) {
        // 33 % chance of a seed, and 1/4 of a seed if it's a block, overrides, so 8.3%
        float normalBlocksDrops = normalBlocks * 0.083333333333f;

        // Each lucky block is a guaranteed drop back
        baseBlockDrop = normalBlocksDrops + luckyBlocks;
      }
      double gemAvg = ItemUtils.getAverageGemCountToDropOnTreeSmash(item);

      // Apply for all regular blocks (66%), for lucky blocks, they count as 5x gems
      gemDrops = ((normalBlocks * 0.66f) * gemAvg) + ((luckyBlocks * gemAvg) * 5);

      // Lucky won't affect seeds (still 25%)
    } else {
      if (dropsItself) {
        baseBlockDrop = blockCount * 0.083333333f;
      }

      // 66% of gems
      double gemAvg = ItemUtils.getAverageGemCountToDropOnTreeSmash(item);
      gemDrops = (blockCount * 0.66f) * gemAvg;
    }

    // Clothing block bonuses share an in-game cap; the Winter Wishing Star bypasses it.
    int cappableBonus = 0;
    if (ances != 0) {
      int tesseractBonus = ances == 1 ? 5 : 5 + (ances - 1);
      components.add(
          TextDisplay.of(
              "%s Ancestral Tesseract of Dimensions Lv.%s (%s%% extra blocks)."
                  .formatted(AppEmojis.ANCES_TESSERACT, ances, tesseractBonus)));
      cappableBonus += tesseractBonus;
    }
    if (buddy) {
      components.add(
          TextDisplay.of(
              "%s Using Buddy's Block Head (2%% extra blocks).".formatted(AppEmojis.BBH)));
      cappableBonus += 2;
    }
    if (galaxy) {
      components.add(
          TextDisplay.of(
              "%s Using Galaxy Skin (10%% extra blocks).".formatted(AppEmojis.GALAXY_SKIN)));
      cappableBonus += 10;
    }

    int clothingBonus = Math.min(cappableBonus, CLOTHING_BONUS_CAP);
    if (star) {
      components.add(
          TextDisplay.of(
              "%s Using Winter Wishing Star (2%% extra blocks, ignores the cap)."
                  .formatted(AppEmojis.WWS)));
      clothingBonus += 2;
    }
    if (cappableBonus > CLOTHING_BONUS_CAP) {
      components.add(
          TextDisplay.of(
              "%s Clothing block bonuses are capped at %d%% in-game — effective bonus is +%d%%."
                  .formatted(AppEmojis.EXCLAMATION, CLOTHING_BONUS_CAP, clothingBonus)));
    }
    baseBlockDrop += blockCount * (clothingBonus / 100f);

    if (!dropsSeeds) {
      components.add(
          TextDisplay.of("%s Block does not drop seeds.".formatted(AppEmojis.EXCLAMATION)));
    }
    if (!dropsItself) {
      components.add(
          TextDisplay.of("%s Block does not drop itself.".formatted(AppEmojis.EXCLAMATION)));
    }
    if (!dropsGems) {
      components.add(
          TextDisplay.of("%s Block does not drop gems.".formatted(AppEmojis.EXCLAMATION)));
    }

    String countFormatted = formatNumber(blockCount);
    components.add(TextDisplay.of("### Results for %s blocks:".formatted(countFormatted)));

    int seedsFromDroppedBlocks = (int) (baseBlockDrop * 0.25f);

    String baseBlockDropFormatted = formatNumber(baseBlockDrop);
    String baseBlockSeedDropFormatted = formatNumber(baseSeedDrop);
    String totalBlocksAndSeedsFormatted =
        formatNumber(baseSeedDrop + seedsFromDroppedBlocks);
    String gemDropsFormatted = formatNumber(gemDrops);
    components.add(
        TextDisplay.of(
            """
            **Average Base Block Drops:** `%s` (+`~%s` seeds if broken)
            **Average Seed Drops:** `%s` (Total `%s` with seeds from broken blocks)
              (%s Will not include all seeds dropped from extra blocks from the base broken blocks!)
            **Average Gem Drops:** `%s`
            """
                .formatted(
                    baseBlockDropFormatted,
                    seedsFromDroppedBlocks,
                    baseBlockSeedDropFormatted,
                    totalBlocksAndSeedsFormatted,
                    AppEmojis.EXCLAMATION,
                    gemDropsFormatted)));

    Container container = ItemUtils.createItemContainer(itemDetail, itemCatalogue, components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  private boolean parseBoolean(String input) {
    return input.equals("True");
  }

  private int tryParseInt(String input, ModalEvent event) {
    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException e) {
      event.reply("`%s` is not a valid integer!".formatted(input)).setEphemeral(true).queue();
    }
    return -1;
  }
}
