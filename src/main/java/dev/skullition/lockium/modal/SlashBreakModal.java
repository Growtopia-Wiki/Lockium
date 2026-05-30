package dev.skullition.lockium.modal;

import dev.skullition.lockium.model.*;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import io.github.freya022.botcommands.api.modals.ModalEvent;
import io.github.freya022.botcommands.api.modals.annotations.ModalData;
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler;
import io.github.freya022.botcommands.api.modals.annotations.ModalInput;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Handler
public class SlashBreakModal {
    public static final String MODAL_NAME = "SlashBreak: break";
    public static final String INPUT_LUCKY = "SlashBreak: lucky";
    public static final String INPUT_BUDDY = "SlashBreak: buddy";
    public static final String INPUT_ANCES = "SlashBreak: ances";
    private final TreeFruitService fruitService;

    public SlashBreakModal(TreeFruitService fruitService) {
        this.fruitService = fruitService;
    }

    @ModalHandler(MODAL_NAME)
    public void onBreakModal(
            ModalEvent event,
            @ModalData ItemDetailResponse itemDetail,
            @ModalData ItemCatalogue itemCatalogue,
            @ModalData int blockCount,
            @ModalInput(INPUT_LUCKY) String luckyString,
            @ModalInput(INPUT_BUDDY) String buddyString,
            @ModalInput(INPUT_ANCES) String ancesString
    ) {
        boolean lucky = parseBoolean(luckyString);
        boolean buddy = parseBoolean(buddyString);
        int ances = tryParseInt(ancesString, event);
        if (ances == -1) {
            return;
        } else if (ances > 6) {
            event.reply("The maximum level of Ancestral Tesseract of Dimensions is 6.").setEphemeral(true).queue();
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
        boolean dropsSeeds = !props.contains(ItemProperty.NO_SEED);
        boolean dropsItself = !props.contains(ItemProperty.NO_DROP);
        boolean dropsGems = !props2.contains(ItemProperty2.GEMLESS);

        // Will always be 25% (unless NoSeed of course)
        float baseSeedDrop = 0;
        if (dropsSeeds) {
            baseSeedDrop = blockCount * 0.25f;
        }
        float baseBlockDrop = 0;
        double gemDrops;
        if (lucky || buddy || ances != 0) {
            components.add(TextDisplay.of("### Optional Items Used:"));
        }

        if (lucky) {
            components.add(
                    TextDisplay.of("%s **Lucky! effect** (10%% chance of triggering) - Increased chance of block drops and 5x gems."
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
            if (dropsItself)
                baseBlockDrop = blockCount * 0.083333333f;

            // 66% of gems
            double gemAvg = ItemUtils.getAverageGemCountToDropOnTreeSmash(item);
            gemDrops = (blockCount * 0.66f) * gemAvg;
        }

        if (ances != 0) {
            float increase;
            if (ances == 1) {
                increase = 0.05f;
            } else {
                increase = 0.05f + (((float) ances - 1f) / 100);
            }
            components.add(TextDisplay.of("%s Ancestral Tesseract of Dimensions Lv.%s (%s%% extra blocks)."
                    .formatted(AppEmojis.ANCES_TESSERACT, ances, (int) (increase * 100))));

            baseBlockDrop += blockCount * increase;
        }

        if (buddy) {
            components.add(TextDisplay.of("%s Using Buddy's Block Head (2%% extra blocks)."
                    .formatted(AppEmojis.BBH)));
            baseBlockDrop += (blockCount * 1.02f) - blockCount;
        }
        
        if (!dropsSeeds) {
            components.add(TextDisplay.of("%s Block does not drop seeds.".formatted(AppEmojis.EXCLAMATION)));
        }
        if (!dropsItself) {
            components.add(TextDisplay.of("%s Block does not drop itself.".formatted(AppEmojis.EXCLAMATION)));
        }
        if (!dropsGems) {
            components.add(TextDisplay.of("%s Block does not drop gems.".formatted(AppEmojis.EXCLAMATION)));
        }

        String countFormatted = String.format(Locale.US, "%,d", blockCount);
        components.add(TextDisplay.of("### Results for %s blocks:".formatted(countFormatted)));
        
        int seedsFromDroppedBlocks = (int)(baseBlockDrop * 0.25f);
        
        String baseBlockDropFormatted = String.format(Locale.US, "%,.0f", baseBlockDrop);
        String baseBlockSeedDropFormatted = String.format(Locale.US, "%,.0f", baseSeedDrop);
        String totalBlocksAndSeedsFormatted = String.format(Locale.US, "%,.0f", baseSeedDrop + seedsFromDroppedBlocks);
        String gemDropsFormatted = String.format(Locale.US, "%,.0f", gemDrops);
        components.add(TextDisplay.of("""
                **Average Base Block Drops:** `%s` (+`~%s` seeds if broken)
                **Average Seed Drops:** `%s` (Total `%s` with seeds from broken blocks)
                    (%s Will not include all seeds dropped from extra blocks from the base broken blocks!)
                **Average Gem Drops:** `%s`
                """
                .formatted(baseBlockDropFormatted, seedsFromDroppedBlocks,
                        baseBlockSeedDropFormatted, totalBlocksAndSeedsFormatted,
                        AppEmojis.EXCLAMATION, gemDropsFormatted)));

        Container container = ItemUtils.createItemContainer(itemDetail, itemCatalogue, components);
        event.replyComponents(container)
                .useComponentsV2()
                .queue();
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
