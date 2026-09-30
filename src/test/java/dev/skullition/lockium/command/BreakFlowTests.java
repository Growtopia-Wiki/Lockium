package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.modal.SlashBreakModal;
import dev.skullition.lockium.model.ItemCatalogue;
import dev.skullition.lockium.properties.ProxyProperties;
import dev.skullition.lockium.service.GrowtopiaDetailService;
import dev.skullition.lockium.service.GrowtopiaLeaderboardService;
import dev.skullition.lockium.service.ItemEffectService;
import dev.skullition.lockium.service.PlayerCountService;
import dev.skullition.lockium.service.RiddleService;
import dev.skullition.lockium.service.TreeFruitService;
import dev.skullition.lockium.service.WikiService;
import dev.skullition.lockium.service.WorldRenderService;
import io.github.freya022.botcommands.api.modals.ModalBuilder;
import io.github.freya022.botcommands.api.modals.Modals;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.modals.Modal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

/** End-to-end unit coverage for opening and submitting the break calculator modal. */
class BreakFlowTests {
  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @Test
  void snapshotsBreakModalOpenedBySlashCommand() throws IOException {
    DiscordEventHarness harness = new DiscordEventHarness();

    openBreakModal(CommandFixtures.DIRT_CATALOGUE, harness);

    SnapshotAssertions.assertMatches("gt-break-modal", harness.snapshot());
  }

  @ParameterizedTest
  @MethodSource("breakModalTitles")
  void opensBreakModalWithinTitleLimit(String itemName, String expectedTitle) {
    ItemCatalogue itemQuery = new ItemCatalogue(1, 2, 3, itemName, "");
    DiscordEventHarness harness = new DiscordEventHarness();

    Modal modal = openBreakModal(itemQuery, harness);

    assertEquals(expectedTitle, modal.getTitle());
    assertTrue(modal.getTitle().length() <= Modal.MAX_TITLE_LENGTH);
    assertTrue(harness.snapshot().startsWith("delivery=modal\n"));
  }

  private static Stream<Arguments> breakModalTitles() {
    return Stream.of(
        Arguments.of("A".repeat(38), "Break " + "A".repeat(38)),
        Arguments.of("A".repeat(39), "Break " + "A".repeat(39)),
        Arguments.of("A".repeat(40), "Break " + "A".repeat(38) + "…"),
        Arguments.of("A".repeat(200), "Break " + "A".repeat(38) + "…"),
        Arguments.of(
            "Bountiful Growtopian-Eating Looming Plant",
            "Break Bountiful Growtopian-Eating Looming Pl…"));
  }

  private static Modal openBreakModal(ItemCatalogue itemQuery, DiscordEventHarness harness) {
    Modals modals = mock(Modals.class);
    ModalBuilder builder = mock(ModalBuilder.class, Answers.RETURNS_SELF);
    when(modals.create(anyString()))
        .thenAnswer(
            invocation -> {
              Modal.Builder jdaBuilder = Modal.create("break-test", invocation.getArgument(0));
              doAnswer(
                      componentInvocation -> {
                        Arrays.stream(componentInvocation.getArguments())
                            .map(ModalTopLevelComponent.class::cast)
                            .forEach(jdaBuilder::addComponents);
                        return builder;
                      })
                  .when(builder)
                  .addComponents(any(ModalTopLevelComponent[].class));
              when(builder.build())
                  .thenAnswer(
                      buildInvocation ->
                          mock(
                              io.github.freya022.botcommands.api.modals.Modal.class,
                              delegatesTo(jdaBuilder.build())));
              return builder;
            });
    WikiService wiki = mock(WikiService.class);
    when(wiki.getItemDetail(itemQuery)).thenReturn(CommandFixtures.dirtDetail());
    GtCommands commands = commands(modals, wiki);
    var event = harness.slashEvent();

    commands.onSlashBreak(event, itemQuery, 10_000);

    verify(builder)
        .bindTo(SlashBreakModal.MODAL_NAME, CommandFixtures.dirtDetail(), itemQuery, 10_000);
    ArgumentCaptor<Modal> modal = ArgumentCaptor.forClass(Modal.class);
    verify(event).replyModal(modal.capture());
    return modal.getValue();
  }

  @Test
  void snapshotsBreakModalCalculation() throws IOException {
    TreeFruitService fruits = mock(TreeFruitService.class);
    when(fruits.getMaxDrop(2)).thenReturn(8);
    SlashBreakModal modal = new SlashBreakModal(fruits);
    DiscordEventHarness harness = new DiscordEventHarness();

    modal.onBreakModal(
        harness.modalEvent(),
        CommandFixtures.dirtDetail(),
        CommandFixtures.DIRT_CATALOGUE,
        10_000,
        "True",
        List.of(SlashBreakModal.CLOTHING_BBH, SlashBreakModal.CLOTHING_GALAXY),
        "6");

    SnapshotAssertions.assertMatches("gt-break-result", harness.snapshot());
  }

  @Test
  void rejectsInvalidTesseractLevel() {
    SlashBreakModal modal = new SlashBreakModal(mock(TreeFruitService.class));
    DiscordEventHarness harness = new DiscordEventHarness();

    modal.onBreakModal(
        harness.modalEvent(),
        CommandFixtures.dirtDetail(),
        CommandFixtures.DIRT_CATALOGUE,
        100,
        "False",
        List.of(),
        "seven");

    assertTrue(harness.snapshot().contains("not a valid integer"));
  }

  private static GtCommands commands(Modals modals, WikiService wiki) {
    return new GtCommands(
        modals,
        wiki,
        mock(GrowtopiaDetailService.class),
        mock(GrowtopiaLeaderboardService.class),
        mock(PlayerCountService.class),
        mock(ProxyProperties.class),
        mock(TreeFruitService.class),
        mock(WorldRenderService.class),
        mock(RiddleService.class),
        mock(ItemEffectService.class),
        Clock.fixed(Instant.parse("2026-07-04T17:22:00Z"), ZoneOffset.UTC),
        () -> 123_456);
  }
}
