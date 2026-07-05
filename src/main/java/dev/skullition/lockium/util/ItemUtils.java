package dev.skullition.lockium.util;

import dev.skullition.lockium.model.Chi;
import dev.skullition.lockium.model.GrowtopiaObject;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.ItemDetailResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;

/**
 * Rendering and calculation helpers for Growtopia items.
 *
 * <p>Centralizes CDN URL formats, wiki linking, gem-drop math, and the standard item container
 * layout used by {@code /gt item} and related commands.
 */
public class ItemUtils {
  private ItemUtils() {}

  private static final String ITEM_SPRITE_URL = "https://cdn.growtopiawiki.com/sprites/%s.png";
  private static final String TREE_SPRITE_URL = "https://cdn.growtopiawiki.com/sprites/%s-tree.png";
  private static final String GROWTOPIA_WIKI_URL = "https://growtopiawiki.com/w/%s";

  /** Item ID → chi map, populated by {@link dev.skullition.lockium.service.ChiService}. */
  private static volatile Map<Integer, Chi> chiMap = Map.of();

  /**
   * Publishes the chi dataset used by {@link #createItemContainer}.
   *
   * <p>Called by {@link dev.skullition.lockium.service.ChiService} at startup and on reload; not
   * intended for other callers.
   *
   * @param map immutable map of in-game item ID to chi
   */
  public static void setChiMap(Map<Integer, Chi> map) {
    chiMap = map;
  }

  /**
   * Builds the CDN URL for an item sprite.
   *
   * @param id the item ID
   * @return absolute URL to {@code {id}.png}
   */
  public static String getItemSpriteUrl(int id) {
    return String.format(ITEM_SPRITE_URL, id);
  }

  /**
   * Builds the CDN URL for a tree sprite.
   *
   * @param id the seed/item ID
   * @return absolute URL to {@code {id}-tree.png}
   */
  public static String getTreeSpriteUrl(int id) {
    return String.format(TREE_SPRITE_URL, id);
  }

  private static final Pattern COLOR_CODE = Pattern.compile("`.?");

  /**
   * Removes Growtopia color codes from an item name.
   *
   * <p>The game encodes text colors as a backtick followed by one character (e.g. {@code
   * `6Immortal Dirt}, {@code ``} to reset). A dangling trailing backtick is also removed.
   *
   * @param itemName raw name from the API; may contain color codes
   * @return the name without color codes
   */
  public static String stripColorCodes(String itemName) {
    if (itemName.indexOf('`') < 0) {
      return itemName;
    }
    return COLOR_CODE.matcher(itemName).replaceAll("");
  }

  /**
   * Normalizes an item name for case-insensitive lookups.
   *
   * @param itemName raw name from user input or API
   * @return trimmed, lower-cased string using {@link Locale#US}
   */
  public static String norm(String itemName) {
    return itemName.trim().toLowerCase(Locale.US);
  }

  /**
   * Calculates the average gems dropped when harvesting a tree.
   *
   * <p>Implements the in-game formula: {@code rarity/4}, reduced by 25% for rarity ≤30, with a
   * minimum of 2, then averaged. Returns 0 for rarity 999 (unbreakable items).
   *
   * @param item the {@link GrowtopiaObject} to evaluate
   * @return average gem count, rounded to nearest whole number
   */
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

  /**
   * Calculates the chance of getting seed as a drop when harvesting a tree.
   *
   * @param rarity the rarity of the item
   * @return the drop chance in percent (0–100); {@code 0} for rarity 999
   */
  public static double getChanceToDropSeedOnTreeSmash(int rarity) {
    if (rarity == 999) {
      return 0;
    }

    // chance is 1/max
    var max = (rarity / 4) + 3;

    return 100.0 / ((double) max); // Random(max)
  }

  /**
   * Creates a full item container with header, body, and footer.
   *
   * <p>The header shows the sprite, wiki-linked name, and description. The accent color is taken
   * from the seed's overlay color.
   *
   * @param item the detail response containing item and seed
   * @param itemCatalogue catalogue entry for display name fallback
   * @param components middle section components (properties, stats, etc.)
   * @return a complete {@link Container} ready to send
   */
  public static Container createItemContainer(
      ItemDetailResponse item,
      ItemCatalogue itemCatalogue,
      List<ContainerChildComponent> components) {
    List<ContainerChildComponent> container = new ArrayList<>();

    // 1. Add Header
    String itemName =
        itemCatalogue.seedName() == null ? item.item().name() : itemCatalogue.seedName();
    String itemUrl = String.format(GROWTOPIA_WIKI_URL, getWikiItemName(itemName));
    var chiEmoji = chiMap.getOrDefault(item.item().id(), Chi.NONE).getEmoji();
    String chiPrefix = chiEmoji == null ? "" : chiEmoji.getFormatted() + " ";
    Section header =
        Section.of(
            Thumbnail.fromUrl(getItemSpriteUrl(item.item().id())),
            TextDisplay.of(String.format("## %s[%s](%s)", chiPrefix, itemName, itemUrl)),
            TextDisplay.of(item.item().description()));
    container.add(header);
    container.add(Separator.create(true, Separator.Spacing.LARGE));

    // 2. Add Middle Components
    container.addAll(components);

    // 3. Add Footer
    container.add(Separator.create(true, Separator.Spacing.SMALL));
    container.add(
        TextDisplay.of("-# With love, by the [Growtopia Wiki](https://growtopiawiki.com)."));

    return Container.of(container).withAccentColor(item.seed().overColor().intOrTransparent());
  }

  /**
   * Varargs overload of {@link #createItemContainer(ItemDetailResponse, ItemCatalogue, List)}.
   *
   * @param item the detail response
   * @param itemCatalogue catalogue entry
   * @param components middle components
   * @return a complete container
   */
  public static Container createItemContainer(
      ItemDetailResponse item, ItemCatalogue itemCatalogue, ContainerChildComponent... components) {
    return createItemContainer(item, itemCatalogue, List.of(components));
  }

  /**
   * Converts an item name to a Wiki URL slug.
   *
   * @param itemName display name (e.g. "Bunny Egg")
   * @return wiki page name (e.g. "Bunny_Egg")
   */
  public static String getWikiItemName(String itemName) {
    return itemName.replace(" ", "_");
  }

  /**
   * Formats a duration in seconds as a compact d/h/m/s string.
   *
   * <p>Examples: {@code 90 → "1m 30s"}, {@code 90061 → "1d 1h 1m 1s"}.
   *
   * @param totalSeconds duration in seconds; values ≤0 return "0s"
   * @return human-readable duration without zero units
   */
  public static String toDayHourMinutesSeconds(int totalSeconds) {
    if (totalSeconds <= 0) {
      return "0s";
    }

    var d = Duration.ofSeconds(totalSeconds);

    StringBuilder sb = new StringBuilder();
    append(sb, d.toDays(), 'd');
    append(sb, d.toHoursPart(), 'h');
    append(sb, d.toMinutesPart(), 'm');
    append(sb, d.toSecondsPart(), 's');

    return sb.toString().trim();
  }

  private static void append(StringBuilder sb, long value, char unit) {
    if (value > 0) {
      sb.append(value).append(unit).append(' ');
    }
  }
}
