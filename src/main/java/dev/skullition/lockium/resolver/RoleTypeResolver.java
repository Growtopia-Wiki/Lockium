package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.RoleType;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ParameterResolver;
import io.github.freya022.botcommands.api.parameters.Resolvers;
import java.util.EnumSet;
import org.springframework.stereotype.Service;

/** BotCommands resolver that converts a slash-command string into an {@link RoleType}. */
@Service
public class RoleTypeResolver {
  @Resolver
  public static ParameterResolver<?, RoleType> getRoleTypeResolver() {
    return Resolvers.enumResolver(
            RoleType.class,
            EnumSet.of(
                RoleType.FISHING,
                RoleType.STAR_CAPTAIN,
                RoleType.BUILDER,
                RoleType.CHEF,
                RoleType.FARMER,
                RoleType.SURGEON))
        .build();
  }
}
