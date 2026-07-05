package dev.skullition.lockium.handler;

import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteMode;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import java.util.Collection;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

/**
 * Autocomplete provider for item-name options across the {@code /gt} subcommands.
 *
 * <p>BotCommands calls this handler as the user types the {@code itemQuery} option. It returns the
 * full set of known item names from the cached {@link WikiService#getNameIndex()}; the framework
 * then filters and ranks the collection and sends Discord the top choices, so we can safely return
 * all keys (~12k entries) without additional filtering.
 *
 * <p>Uses {@link AutocompleteMode#CONTINUITY} to keep the interaction alive across keystrokes.
 */
@Handler
public class ItemNameAutocompleteHandler {

  /** Autocomplete identifier referenced in {@code @SlashOption(autocomplete = ...)}. */
  public static final String ITEM_AUTOCOMPLETE_NAME = "SlashItem: itemQuery";

  private final WikiService wikiService;

  /**
   * Creates the handler.
   *
   * @param wikiService service that holds the cached item name index
   */
  public ItemNameAutocompleteHandler(WikiService wikiService) {
    this.wikiService = wikiService;
  }

  /**
   * Supplies autocomplete choices for item names.
   *
   * <p>Invoked by BotCommands on every {@link CommandAutoCompleteInteractionEvent}.
   *
   * @param event the autocomplete interaction (unused, required by the framework)
   * @return an immutable collection of all item names; never {@code null}
   */
  @AutocompleteHandler(value = ITEM_AUTOCOMPLETE_NAME, mode = AutocompleteMode.CONTINUITY)
  public Collection<String> onItemNameAutocomplete(CommandAutoCompleteInteractionEvent event) {
    return wikiService.getNameIndex().keySet();
  }
}
