package dev.skullition.lockium.command;

import static dev.skullition.lockium.util.FormatUtil.formatNumber;

import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slash commands under {@code /gt provider} for provider-block earning estimates.
 *
 * <p>Providers are blocks that produce items over time (ATMs, Tackle Boxes, Science
 * Stations). All commands are pure calculations over hardcoded in-game drop rates - no external
 * API calls are made.
 */
@Command
public class ProviderCommands {
  private static final Logger logger = LoggerFactory.getLogger(ProviderCommands.class);

  /** In-game item ID of the ATM, used for the sprite thumbnail. */
  private static final int ATM_MACHINE_ID = 1008;
  /** In-game item ID of the Tackle Box, used for the sprite thumbnail. */
  private static final int TACKLE_BOX_ID = 3044;
  /** Tackle Box drop pool with in-game weights. */
  private static final List<TackleDrop> TACKLE_DROPS =
      List.of(
          new TackleDrop("Wiggly Worms", 500),
          new TackleDrop("Shiny Flashy Worms", 400),
          new TackleDrop("Salmon Eggs", 300),
          new TackleDrop("Fishing Fly", 200),
          new TackleDrop("Shrimp Lure", 100),
          new TackleDrop("Uranium Glowing Lure", 50),
          new TackleDrop("Mega-Pellet Bait", 50));
  /** In-game item ID of the Science Station, used for the sprite thumbnail. */
  private static final int SCIENCE_STATION_ID = 928;
  /** Science Station chemical drop chances, in percent. */
  private static final List<ChemicalDrop> CHEMICAL_DROPS =
      List.of(
          new ChemicalDrop("Chemical G", 40.0),
          new ChemicalDrop("Chemical R", 25.0),
          new ChemicalDrop("Chemical B", 16.0),
          new ChemicalDrop("Chemical Y", 13.0),
          new ChemicalDrop("Chemical P", 7.0));

  /**
   * Handles {@code /gt provider atm}.
   *
   * <p>Estimates gem earnings from harvesting ATMs. Every ATM drops 1-19 gems (average
   * 10), and one in a hundred harvests is a jackpot of 100 gems.
   *
   * @param event the slash interaction
   * @param atmCount number of ATMs harvested per day; must be between 1 and 500,000 (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      group = "provider",
      subcommand = "atm",
      description = "Estimates ATM Machine earnings.")
  public void onSlashAtm(
      GlobalSlashEvent event, @SlashOption(description = "How many ATMs?") long atmCount) {
    logger.debug("onSlashAtm: atmCount={}", atmCount);
    if (atmCount < 1 || atmCount > 500_000) {
      logger.debug("onSlashAtm: rejected atmCount={}", atmCount);
      event.reply("Must be between 1 and 500,000 ATMs.").queue();
      return;
    }
    int atmCountInt = (int) atmCount;

    // Non-jackpot drops are 1-19 gems, so an average of 10.
    final double normalDropAverage = 10.0;
    int countJackpot = atmCountInt / 100;
    int countNormal = atmCountInt - countJackpot;
    long totalGems = (long) (countJackpot * 100 + countNormal * normalDropAverage);

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        Section.of(
            Thumbnail.fromUrl(ItemUtils.getItemSpriteUrl(ATM_MACHINE_ID)),
            TextDisplay.of("### Harvesting %s ATMs".formatted(formatNumber(atmCount))),
            TextDisplay.of("-# ATMs cost 8,000 Gems from the City Pack.")));
    components.add(Separator.create(true, Separator.Spacing.LARGE));
    components.add(
        TextDisplay.of(
            """
            %1$s Total Gems: `~%2$s`
            %1$s Total Gems (1 week): `~%3$s`
            %1$s Total Gems (30 days): `~%4$s`\
            """
                .formatted(
                    AppEmojis.GEM,
                    formatNumber(totalGems),
                    formatNumber(totalGems * 7),
                    formatNumber(totalGems * 30))));
    components.add(Separator.create(true, Separator.Spacing.SMALL));
    components.add(
        TextDisplay.of(
            """
            ▫ ATM Cost (14/14.5 WLs each) -> `%s-%s` %s
            ▫ Cost from Store: `%s` gems\
            """
                .formatted(
                    formatNumber(atmCount * 14L),
                    formatNumber((long) (atmCount * 14.5)),
                    AppEmojis.WORLD_LOCK,
                    formatNumber(atmCount * 8_000L))));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt provider tackle}.
   *
   * <p>Estimates bait drops from harvesting Tackle Boxes. Each drop is picked from a weighted
   * pool, and every harvest yields 1.5 items on average.
   *
   * @param event the slash interaction
   * @param tackleCount number of Tackle Boxes harvested per day; must be between 50 and 500,000
   *     (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      group = "provider",
      subcommand = "tackle",
      description = "Estimates Tackle Box earnings.")
  public void onSlashTackle(
      GlobalSlashEvent event, @SlashOption(description = "How many Tackle Boxes?") long tackleCount) {
    logger.debug("onSlashTackle: tackleCount={}", tackleCount);
    if (tackleCount < 50 || tackleCount > 500_000) {
      logger.debug("onSlashTackle: rejected tackleCount={}", tackleCount);
      event.reply("Must be between 50 and 500,000 Tackle Boxes.").queue();
      return;
    }

    int poolTotal = TACKLE_DROPS.stream().mapToInt(TackleDrop::weight).sum();

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        Section.of(
            Thumbnail.fromUrl(ItemUtils.getItemSpriteUrl(TACKLE_BOX_ID)),
            TextDisplay.of("### Harvesting %s Tackle Boxes".formatted(formatNumber(tackleCount))),
            TextDisplay.of("-# Tackle Boxes cost 10,000 Gems from the Fishin' Pack.")));
    components.add(Separator.create(true, Separator.Spacing.LARGE));

    StringBuilder drops = new StringBuilder();
    for (TackleDrop drop : TACKLE_DROPS) {
      long total = (long) (tackleCount * ((double) drop.weight() / poolTotal) * 1.5);
      drops.append("▫ %s: `~%s`\n".formatted(drop.name(), formatNumber(total)));
    }
    components.add(TextDisplay.of(drops.toString()));
    components.add(Separator.create(true, Separator.Spacing.SMALL));
    components.add(
        TextDisplay.of(
            """
            ▫ Tackle Box Cost (3/3.5 WLs each) -> `%s-%s` %s
            ▫ Cost from Store: `%s` gems\
            """
                .formatted(
                    formatNumber(tackleCount * 3L),
                    formatNumber((long) (tackleCount * 3.5)),
                    AppEmojis.WORLD_LOCK,
                    formatNumber(tackleCount * 10_000L))));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * Handles {@code /gt provider science}.
   *
   * <p>Estimates chemical drops from harvesting Science Stations, using the in-game drop chances
   * per chemical color.
   *
   * @param event the slash interaction
   * @param scienceCount number of Science Stations harvested per day; must be between 50 and
   *     500,000 (inclusive)
   */
  @JDASlashCommand(
      name = "gt",
      group = "provider",
      subcommand = "science",
      description = "Estimates Science Station earnings.")
  public void onSlashScience(
      GlobalSlashEvent event,
      @SlashOption(description = "How many Science Stations?") long scienceCount) {
    logger.debug("onSlashScience: scienceCount={}", scienceCount);
    if (scienceCount < 50 || scienceCount > 500_000) {
      logger.debug("onSlashScience: rejected scienceCount={}", scienceCount);
      event.reply("Must be between 50 and 500,000 Science Stations.").queue();
      return;
    }

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        Section.of(
            Thumbnail.fromUrl(ItemUtils.getItemSpriteUrl(SCIENCE_STATION_ID)),
            TextDisplay.of(
                "### Harvesting %s Science Stations".formatted(formatNumber(scienceCount)))));
    components.add(Separator.create(true, Separator.Spacing.LARGE));

    StringBuilder drops = new StringBuilder();
    for (ChemicalDrop drop : CHEMICAL_DROPS) {
      long total = (long) (scienceCount * (drop.chance() / 100));
      drops.append("▫ %s: `~%s`\n".formatted(drop.name(), formatNumber(total)));
    }
    components.add(TextDisplay.of(drops.toString()));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  /**
   * A weighted entry of the Tackle Box drop pool.
   *
   * @param name display name of the dropped item
   * @param weight relative weight within the pool
   */
  private record TackleDrop(String name, int weight) {}

  /**
   * A chemical produced by Science Stations with its drop chance.
   *
   * @param name display name of the chemical
   * @param chance drop chance in percent
   */
  private record ChemicalDrop(String name, double chance) {}
}
