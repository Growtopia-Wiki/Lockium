package dev.skullition.lockium.util;

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

    public static Container createItemContainer(ItemDetailResponse item, ContainerChildComponent... components) {
        List<ContainerChildComponent> container = new ArrayList<>();

        // 1. Add Header
        String itemUrl = String.format(GROWTOPIA_WIKI_URL, getWikiItemName(item.item().name()));
        Section header = Section.of(Thumbnail.fromUrl(
                        getItemSpriteUrl(item.item().id())),
                TextDisplay.of(String.format("## [%s](%s)", item.item().name(), itemUrl)),
                TextDisplay.of(item.item().description())
        );
        container.add(header);
        container.add(Separator.create(true, Separator.Spacing.LARGE));

        // 2. Add Middle Components
        container.addAll(List.of(components));

        // 3. Add Footer
        container.add(Separator.create(true, Separator.Spacing.SMALL));
        container.add(TextDisplay.of("-# With love, by the [Growtopia Wiki](https://growtopiawiki.com)."));

        return Container.of(container).withAccentColor(item.seed().overColor().intOrTransparent());
    }

    public static String getWikiItemName(String itemName) {
        return itemName.replace(" ", "_");
    }
}
