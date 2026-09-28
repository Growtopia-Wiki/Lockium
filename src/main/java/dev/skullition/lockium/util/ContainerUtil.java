package dev.skullition.lockium.util;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

/**
 * Factory helpers for creating consistent JDA {@link Container} layouts.
 *
 * <p>All generic Lockium containers share the same footer and accent color. Use these methods or
 * {@link ItemUtils#createItemContainer(ItemDetailResponse, ItemCatalogue,
 * ContainerChildComponent...)} instead of building containers manually to ensure visual
 * consistency.
 *
 * @see ItemUtils#createItemContainer
 */
public class ContainerUtil {
  private ContainerUtil() {}

  /**
   * Creates a generic container with a standard footer.
   *
   * @param components list of child components; the list is copied, the original is not modified
   * @return a {@link Container} with the provided children, a small separator, and a "Crafted by
   *     the Growtopia Wiki" footer, accented in pink
   */
  public static Container createGenericContainer(ContainerChildComponent... components) {
    return createGenericContainer(Arrays.asList(components));
  }

  /**
   * Creates a generic container with a standard footer.
   *
   * @param components list of child components; the list is copied, the original is not modified
   * @return a {@link Container} with the provided children, a small separator, and a "Crafted by
   *     the Growtopia Wiki" footer, accented in pink
   */
  public static Container createGenericContainer(List<ContainerChildComponent> components) {
    List<ContainerChildComponent> container = new ArrayList<>(components);

    // Add Footer
    container.add(Separator.create(true, Separator.Spacing.SMALL));
    container.add(TextDisplay.of("-# Crafted by the [Growtopia Wiki](https://growtopiawiki.com)!"));

    return Container.of(container).withAccentColor(Color.PINK);
  }
}
