package dev.skullition.lockium.resolver;

import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jspecify.annotations.Nullable;

/**
 * BotCommands resolver that converts a slash-command string into an {@link ItemCatalogue}.
 *
 * <p>Register this once with {@code @Resolver} and any command parameter of type
 * {@code ItemCatalogue} will be automatically resolved:
 * <pre>
 * public void onSlashItem(ItemCatalogue item) { ... }
 * </pre>
 *
 * <p>Resolution steps:
 * <ol>
 *   <li>Discord sends the raw string</li>
 *   <li>Whitespace is trimmed</li>
 *   <li>{@link WikiService#findByName(String)} performs fuzzy lookup</li>
 * </ol>
 *
 * <p>If no match is found, {@code null} is returned and BotCommands replies
 * with the standard "Invalid option" message, keeping command handlers clean.
 *
 * @see WikiService
 */
@Resolver
public class ItemCatalogueResolver
    extends ClassParameterResolver<ItemCatalogueResolver, ItemCatalogue>
    implements SlashParameterResolver<ItemCatalogueResolver, ItemCatalogue> {
  private final WikiService wikiService;

  /**
   * Creates the resolver.
   *
   * @param wikiService service used for name lookups; never {@code null}
   */
  public ItemCatalogueResolver(WikiService wikiService) {
    super(ItemCatalogue.class);
    this.wikiService = wikiService;
  }

  @Override
  public OptionType getOptionType() {
    return OptionType.STRING;
  }

  @Override
  @Nullable
  public ItemCatalogue resolve(
      SlashCommandOption option, CommandInteractionPayload event, OptionMapping optionMapping) {
    return resolveItemByName(optionMapping);
  }

  @Nullable
  private ItemCatalogue resolveItemByName(OptionMapping optionMapping) {
    String itemName = optionMapping.getAsString().trim();

    return wikiService.findByName(itemName);
  }
}
