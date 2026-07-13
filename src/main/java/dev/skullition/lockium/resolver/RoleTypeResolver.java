package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.RoleType;
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
 * Resolves the {@code role} option of {@code /gt role} into a {@link RoleType}.
 *
 * <p>Replaces the library's built-in enum resolver. Choices use each role's {@link
 * RoleType#getRoleName() display name} (e.g. {@code "Star Captain"}) rather than the humanized
 * constant name, and resolution no longer depends on the framework reflecting over the enum's extra
 * fields.
 */
@Resolver
public class RoleTypeResolver extends ClassParameterResolver<RoleTypeResolver, RoleType>
    implements SlashParameterResolver<RoleTypeResolver, RoleType> {
  private static final Logger logger = LoggerFactory.getLogger(RoleTypeResolver.class);

  /** Creates the resolver. */
  public RoleTypeResolver() {
    super(RoleType.class);
  }

  @Override
  public OptionType getOptionType() {
    return OptionType.STRING;
  }

  @Override
  public Collection<Command.Choice> getPredefinedChoices(@Nullable Guild guild) {
    return Arrays.stream(RoleType.values())
        .map(role -> new Command.Choice(role.getRoleName(), role.name()))
        .toList();
  }

  @Override
  public RoleType resolve(
      SlashCommandOption option, CommandInteractionPayload event, OptionMapping optionMapping) {
    String value = optionMapping.getAsString();
    logger.debug("resolve: role={}", value);
    return RoleType.valueOf(value);
  }
}
