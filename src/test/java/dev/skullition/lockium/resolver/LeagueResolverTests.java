package dev.skullition.lockium.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.model.League;
import java.util.List;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.junit.jupiter.api.Test;

/** Tests the predefined choices and resolution of the {@code league} option. */
class LeagueResolverTests {

  @Test
  void choicesUseDisplayNamesAndConstantValues() {
    List<Command.Choice> choices = List.copyOf(new LeagueResolver().getPredefinedChoices(null));

    assertEquals(League.values().length, choices.size());
    assertEquals("Bronze I", choices.getFirst().getName());
    assertEquals("BRONZE_1", choices.getFirst().getAsString());
    assertEquals("Sapphire III", choices.getLast().getName());
    assertEquals("SAPPHIRE_3", choices.getLast().getAsString());
  }

  @Test
  void everyChoiceRoundTripsThroughResolve() {
    LeagueResolver resolver = new LeagueResolver();

    for (Command.Choice choice : resolver.getPredefinedChoices(null)) {
      OptionMapping mapping = mock(OptionMapping.class);
      when(mapping.getAsString()).thenReturn(choice.getAsString());

      assertSame(League.valueOf(choice.getAsString()), resolver.resolve(null, null, mapping));
    }
  }

  @Test
  void usesStringOptions() {
    assertEquals(OptionType.STRING, new LeagueResolver().getOptionType());
  }

  @Test
  void choiceCountFitsDiscordLimit() {
    assertTrue(new LeagueResolver().getPredefinedChoices(null).size() <= 25);
  }
}
