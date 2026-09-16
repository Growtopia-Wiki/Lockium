package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.League;
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import java.util.Arrays;
import java.util.Collection;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@code league} option of {@code /gt leaderboard} into a {@link League}.
 *
 * <p>Follows the same pattern as {@link RoleTypeResolver}: choices use each league's {@link
 * League#getDisplayName() display name} (e.g. {@code "Sapphire III"}) rather than the humanized
 * constant name. The 18 leagues fit within Discord's limit of 25 choices per option.
 */
@Resolver
public class LeagueResolver extends ClassParameterResolver<LeagueResolver, League>
    implements SlashParameterResolver<LeagueResolver, League> {
  private static final Logger logger = LoggerFactory.getLogger(LeagueResolver.class);

  /** Creates the resolver. */
  public LeagueResolver() {
    super(League.class);
  }

  @Override
  public OptionType getOptionType() {
    return OptionType.STRING;
  }

  @Override
  public Collection<Command.Choice> getPredefinedChoices(@Nullable Guild guild) {
    return Arrays.stream(League.values())
        .map(league -> new Command.Choice(league.getDisplayName(), league.name()))
        .toList();
  }

  @Override
  public League resolve(
      SlashCommandOption option, CommandInteractionPayload event, OptionMapping optionMapping) {
    String value = optionMapping.getAsString();
    logger.debug("resolve: league={}", value);
    return League.valueOf(value);
  }
}
