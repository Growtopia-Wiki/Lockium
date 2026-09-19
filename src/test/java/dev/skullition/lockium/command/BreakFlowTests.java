package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.modal.SlashBreakModal;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

/** End-to-end unit coverage for opening and submitting the break calculator modal. */
class BreakFlowTests {
  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @Test
  void snapshotsBreakModalOpenedBySlashCommand() throws IOException {
    Modals modals = mock(Modals.class);
    ModalBuilder builder = mock(ModalBuilder.class, Answers.RETURNS_SELF);
    List<ModalTopLevelComponent> components = new ArrayList<>();
    when(modals.create("Break Dirt")).thenReturn(builder);
    doAnswer(
            invocation -> {
              Arrays.stream(invocation.getArguments())
                  .map(ModalTopLevelComponent.class::cast)
                  .forEach(components::add);
              return builder;
            })
        .when(builder)
        .addComponents(any(ModalTopLevelComponent[].class));
    when(builder.build())
        .thenAnswer(
            invocation -> {
              net.dv8tion.jda.api.modals.Modal modal =
                  net.dv8tion.jda.api.modals.Modal.create("break-test", "Break Dirt")
                      .addComponents(components)
                      .build();
              return mock(
                  io.github.freya022.botcommands.api.modals.Modal.class, delegatesTo(modal));
            });
    WikiService wiki = mock(WikiService.class);
    when(wiki.getItemDetail(CommandFixtures.DIRT_CATALOGUE))
        .thenReturn(CommandFixtures.dirtDetail());
    GtCommands commands = commands(modals, wiki);
    DiscordEventHarness harness = new DiscordEventHarness();

    commands.onSlashBreak(harness.slashEvent(), CommandFixtures.DIRT_CATALOGUE, 10_000);

    SnapshotAssertions.assertMatches("gt-break-modal", harness.snapshot());
    verify(builder)
        .bindTo(
            SlashBreakModal.MODAL_NAME,
            CommandFixtures.dirtDetail(),
            CommandFixtures.DIRT_CATALOGUE,
            10_000);
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
