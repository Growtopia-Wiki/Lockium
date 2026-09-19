package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.skullition.lockium.handler.ItemNameAutocompleteHandler;
import dev.skullition.lockium.modal.SlashBreakModal;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler;
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Guards the registered Discord command names and framework linkage annotations. */
class CommandContractTests {
  @Test
  void exposesTheExpectedSlashCommandSurface() {
    Set<String> actual =
        Stream.of(GtCommands.class, ProviderCommands.class, OwnerCommands.class, SlashPing.class)
            .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
            .filter(method -> method.isAnnotationPresent(JDASlashCommand.class))
            .map(CommandContractTests::commandPath)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of(
            "gt item",
            "gt sprite",
            "gt break",
            "gt harvest",
            "gt recycle",
            "gt mooncakes",
            "gt role",
            "gt events",
            "gt telephone",
            "gt xp",
            "gt startdate",
            "gt time",
            "gt riddle",
            "gt search",
            "gt world",
            "gt wotd",
            "gt stats",
            "gt leaderboard",
            "gt provider atm",
            "gt provider tackle",
            "gt provider science",
            "owner activity",
            "owner guilds",
            "owner reload",
            "ping"),
        actual);
  }

  @Test
  void everySlashEntryPointCarriesRegistrationMetadata() {
    for (Class<?> type :
        List.of(GtCommands.class, ProviderCommands.class, OwnerCommands.class, SlashPing.class)) {
      for (Method method : type.getDeclaredMethods()) {
        if (method.getName().startsWith("onSlash")) {
          assertTrue(
              method.isAnnotationPresent(JDASlashCommand.class),
              () -> type.getSimpleName() + "." + method.getName() + " is not registered");
        }
      }
    }
  }

  @Test
  void autocompleteAndModalNamesStayLinkedToTheirConsumers() throws ReflectiveOperationException {
    Method item = GtCommands.class.getMethod("onSlashItem", eventType(), itemCatalogueType());
    Parameter itemQuery = item.getParameters()[1];
    SlashOption slashOption = itemQuery.getAnnotation(SlashOption.class);
    Method autocomplete =
        ItemNameAutocompleteHandler.class.getMethod(
            "onItemNameAutocomplete",
            net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
                .class);
    Method modal =
        SlashBreakModal.class.getMethod(
            "onBreakModal",
            io.github.freya022.botcommands.api.modals.ModalEvent.class,
            dev.skullition.lockium.model.ItemDetailResponse.class,
            dev.skullition.lockium.model.ItemCatalogue.class,
            int.class,
            String.class,
            java.util.List.class,
            String.class);

    assertEquals(
        autocomplete.getAnnotation(AutocompleteHandler.class).value(), slashOption.autocomplete());
    assertEquals(SlashBreakModal.MODAL_NAME, modal.getAnnotation(ModalHandler.class).value());
  }

  private static String commandPath(Method method) {
    JDASlashCommand command = method.getAnnotation(JDASlashCommand.class);
    return Stream.of(command.name(), command.group(), command.subcommand())
        .filter(part -> !part.isBlank())
        .collect(Collectors.joining(" "));
  }

  private static Class<?> eventType() {
    return io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent.class;
  }

  private static Class<?> itemCatalogueType() {
    return dev.skullition.lockium.model.ItemCatalogue.class;
  }
}
