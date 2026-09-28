package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.SpriteVariant;
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jspecify.annotations.Nullable;

/** Resolves the optional {@code /gt sprite} choice from a Discord string option. */
@Resolver
public class SpriteVariantResolver
    extends ClassParameterResolver<SpriteVariantResolver, SpriteVariant>
    implements SlashParameterResolver<SpriteVariantResolver, SpriteVariant> {

  /** Creates the resolver. */
  public SpriteVariantResolver() {
    super(SpriteVariant.class);
  }

  @Override
  public OptionType getOptionType() {
    return OptionType.STRING;
  }

  @Override
  public List<Command.Choice> getPredefinedChoices(@Nullable Guild guild) {
    return List.of(new Command.Choice("Seed", "SEED"), new Command.Choice("Tree", "TREE"));
  }

  @Override
  public SpriteVariant resolve(
      SlashCommandOption option, CommandInteractionPayload event, OptionMapping optionMapping) {
    return SpriteVariant.valueOf(optionMapping.getAsString());
  }
}
