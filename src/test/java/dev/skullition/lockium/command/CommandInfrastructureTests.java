package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.handler.ItemNameAutocompleteHandler;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.model.RoleType;
import dev.skullition.lockium.resolver.ItemCatalogueResolver;
import dev.skullition.lockium.resolver.RoleTypeResolver;
import dev.skullition.lockium.service.WikiService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.junit.jupiter.api.Test;

/** Tests the autocomplete and resolver plumbing used before command methods run. */
class CommandInfrastructureTests {
  @Test
  void itemResolverTrimsInputAndReturnsTheServiceResult() {
    WikiService wiki = mock(WikiService.class);
    ItemCatalogue expected = CommandFixtures.DIRT_CATALOGUE;
    when(wiki.findByName("Dirt")).thenReturn(expected);
    OptionMapping mapping = mock(OptionMapping.class);
    when(mapping.getAsString()).thenReturn("  Dirt  ");

    ItemCatalogue actual = new ItemCatalogueResolver(wiki).resolve(null, null, mapping);

    assertSame(expected, actual);
    verify(wiki).findByName("Dirt");
  }

  @Test
  void itemResolverKeepsUnknownItemsInvalid() {
    WikiService wiki = mock(WikiService.class);
    OptionMapping mapping = mock(OptionMapping.class);
    when(mapping.getAsString()).thenReturn("Unknown");

    assertNull(new ItemCatalogueResolver(wiki).resolve(null, null, mapping));
  }

  @Test
  void roleChoicesUseDisplayNamesAndRoundTrip() {
    RoleTypeResolver resolver = new RoleTypeResolver();
    List<Command.Choice> choices = List.copyOf(resolver.getPredefinedChoices(null));
    OptionMapping mapping = mock(OptionMapping.class);
    when(mapping.getAsString()).thenReturn(RoleType.STAR_CAPTAIN.name());

    assertEquals(OptionType.STRING, resolver.getOptionType());
    assertEquals("Star Captain", choices.get(1).getName());
    assertSame(RoleType.STAR_CAPTAIN, resolver.resolve(null, null, mapping));
  }

  @Test
  void autocompleteReturnsTheCurrentWikiIndexNames() {
    WikiService wiki = mock(WikiService.class);
    when(wiki.getNameIndex())
        .thenReturn(
            Map.of(
                "Dirt", CommandFixtures.DIRT_CATALOGUE,
                "Dirt Seed", CommandFixtures.DIRT_CATALOGUE));

    assertEquals(
        Set.of("Dirt", "Dirt Seed"),
        new ItemNameAutocompleteHandler(wiki).onItemNameAutocomplete(null));
  }
}
