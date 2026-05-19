package dev.skullition.lockium.util;

import dev.skullition.lockium.model.ItemDetailResponse;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemUtils {

    public static String norm(String itemName) {
        return itemName.trim().toLowerCase(Locale.ROOT);
    }

    public static Container createItemContainer(ItemDetailResponse item, ContainerChildComponent... components) {
        List<ContainerChildComponent> container = new ArrayList<>();

        // 1. Add Header
        container.add(TextDisplay.of(String.format("# %s", item.item().name())));
        container.add(Separator.create(true, Separator.Spacing.LARGE));

        // 2. Add Middle Components
        container.addAll(List.of(components));

        // 3. Add Footer
        container.add(Separator.create(true, Separator.Spacing.SMALL));
        container.add(TextDisplay.of("-# With love, by the [Growtopia Wiki](https://growtopiawiki.com)."));

        return Container.of(container).withAccentColor(item.seed().overColor().intOrTransparent());
    }
}
