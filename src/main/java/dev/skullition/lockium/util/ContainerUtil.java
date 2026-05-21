package dev.skullition.lockium.util;

import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContainerUtil {
    public static Container createGenericContainer(ContainerChildComponent... components) {
        return createGenericContainer(Arrays.asList(components));
    }

    public static Container createGenericContainer(List<ContainerChildComponent> components) {
        List<ContainerChildComponent> container = new ArrayList<>(components);

        // Add Footer
        container.add(Separator.create(true, Separator.Spacing.SMALL));
        container.add(TextDisplay.of("-# Crafted by the [Growtopia Wiki](https://growtopiawiki.com)!"));
        
        return Container.of(container).withAccentColor(Color.PINK);
    }
}
