package dev.skullition.lockium.command;

import dev.skullition.lockium.util.AppEmojis;
import dev.skullition.lockium.util.ContainerUtil;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;

/**
 * Slash commands under {@code /gt provider} for provider-block earning estimates.
 *
 * <p>Providers are blocks that produce items over time (ATM Machines, Tackle Boxes, Science
 * Stations). All commands are pure calculations over hardcoded in-game drop rates - no external
 * API calls are made.
 */
@Command
public class ProviderCommands {
  /** In-game item ID of the ATM Machine, used for the sprite thumbnail. */
  private static final int ATM_MACHINE_ID = 1008;

  /**
   * Handles {@code /gt provider atm}.
   *
   * <p>Estimates gem earnings from harvesting ATM Machines. Every ATM drops 1-19 gems (average
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
      GlobalSlashEvent event, @SlashOption(description = "How many ATM Machines?") int atmCount) {
    if (atmCount < 1 || atmCount > 500_000) {
      event.reply("Must be between 1 and 500,000 ATMs.").queue();
      return;
    }

    // Non-jackpot drops are 1-19 gems, so an average of 10.
    final double normalDropAverage = 10.0;
    int countJackpot = atmCount / 100;
    int countNormal = atmCount - countJackpot;
    long totalGems = (long) (countJackpot * 100 + countNormal * normalDropAverage);

    List<ContainerChildComponent> components = new ArrayList<>();
    components.add(
        Section.of(
            Thumbnail.fromUrl(ItemUtils.getItemSpriteUrl(ATM_MACHINE_ID)),
            TextDisplay.of("### Harvesting %s ATM Machines".formatted(formatNumber(atmCount))),
            TextDisplay.of("-# ATM Machines cost 8,000 Gems from the City Pack.")));
    components.add(Separator.create(true, Separator.Spacing.LARGE));
    components.add(
        TextDisplay.of(
            """
            %1$s Total Gems: `~%2$s`
            %1$s Total Gems (1 week): `~%3$s`
            %1$s Total Gems (30 days): `~%4$s`"""
                .formatted(
                    AppEmojis.GEM,
                    formatNumber(totalGems),
                    formatNumber(totalGems * 7),
                    formatNumber(totalGems * 30))));
    components.add(Separator.create(true, Separator.Spacing.SMALL));
    components.add(
        TextDisplay.of(
            """
            ▫ ATM Cost (3/3.5 WLs each) -> `%s-%s` %s
            ▫ Cost from Store: `%s` gems"""
                .formatted(
                    formatNumber(atmCount * 3L),
                    formatNumber((long) (atmCount * 3.5)),
                    AppEmojis.WORLD_LOCK,
                    formatNumber(atmCount * 8_000L))));

    Container container = ContainerUtil.createGenericContainer(components);
    event.replyComponents(container).useComponentsV2().queue();
  }

  private static String formatNumber(long value) {
    return String.format(Locale.US, "%,d", value);
  }
}
