package dev.skullition.lockium.util;

import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemUtils {
    private static final String ITEM_SPRITE_URL = "https://cdn.growtopiawiki.com/sprites/%s.png";
    private static final String TREE_SPRITE_URL = "https://cdn.growtopiawiki.com/sprites/%s-tree.png";
    private static final String GROWTOPIA_WIKI_URL = "https://growtopiawiki.com/w/%s";

    public static String getItemSpriteUrl(int id) {
        return String.format(ITEM_SPRITE_URL, id);
    }

    public static String getTreeSpriteUrl(int id) {
        return String.format(TREE_SPRITE_URL, id);
    }

    public static String norm(String itemName) {
        return itemName.trim().toLowerCase(Locale.ROOT);
    }

    public static double getAverageGemCountToDropOnTreeSmash(GrowtopiaObject item) {
        if (item.rarity() == 999) {
            return 0d;
        }

        int gemDrop = item.rarity() / 4;
        if (item.rarity() <= 30) {
            gemDrop = (3 * gemDrop) / 4;
        }

        int end = Math.max(gemDrop, 2);
        return Math.round((end - 1) / 2d);
    }

    public static Container createItemContainer(ItemDetailResponse item,
                                                ItemCatalogue itemCatalogue,
                                                List<ContainerChildComponent> components) {
        List<ContainerChildComponent> container = new ArrayList<>();

        // 1. Add Header
        String itemName = itemCatalogue.seedName() == null ? item.item().name() : itemCatalogue.seedName();
        String itemUrl = String.format(GROWTOPIA_WIKI_URL, getWikiItemName(itemName));
        Section header = Section.of(Thumbnail.fromUrl(
                        getItemSpriteUrl(item.item().id())),
                TextDisplay.of(String.format("## [%s](%s)", itemName, itemUrl)),
                TextDisplay.of(item.item().description())
        );
        container.add(header);
        container.add(Separator.create(true, Separator.Spacing.LARGE));

        // 2. Add Middle Components
        container.addAll(components);

        // 3. Add Footer
        container.add(Separator.create(true, Separator.Spacing.SMALL));
        container.add(TextDisplay.of("-# With love, by the [Growtopia Wiki](https://growtopiawiki.com)."));

        return Container.of(container).withAccentColor(item.seed().overColor().intOrTransparent());
    }

    public static Container createItemContainer(ItemDetailResponse item,
                                                ItemCatalogue itemCatalogue,
                                                ContainerChildComponent... components) {
        return createItemContainer(item, itemCatalogue, List.of(components));
    }

    public static String getWikiItemName(String itemName) {
        return itemName.replace(" ", "_");
    }

    public static String toDHMS(int totalSeconds) {
        if (totalSeconds <= 0) return "0s";

        var d = java.time.Duration.ofSeconds(totalSeconds);

        StringBuilder sb = new StringBuilder();
        append(sb, d.toDays(), 'd');
        append(sb, d.toHoursPart(), 'h');
        append(sb, d.toMinutesPart(), 'm');
        append(sb, d.toSecondsPart(), 's');

        return sb.toString().trim();
    }

    private static void append(StringBuilder sb, long value, char unit) {
        if (value > 0) sb.append(value).append(unit).append(' ');
    }
}
