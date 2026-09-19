package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Regression tests for provider calculation commands. */
class ProviderCommandsTests {
  private final ProviderCommands commands = new ProviderCommands();

  @BeforeAll
  static void initializeEmojis() throws ClassNotFoundException {
    TestAppEmojis.initialize();
  }

  @Test
  void snapshotsSuccessfulProviderReplies() throws IOException {
    DiscordEventHarness atm = new DiscordEventHarness();
    commands.onSlashAtm(atm.slashEvent(), 1_000);

    DiscordEventHarness tackle = new DiscordEventHarness();
    commands.onSlashTackle(tackle.slashEvent(), 1_000);

    DiscordEventHarness science = new DiscordEventHarness();
    commands.onSlashScience(science.slashEvent(), 1_000);

    SnapshotAssertions.assertMatches("provider-atm", atm.snapshot());
    SnapshotAssertions.assertMatches("provider-tackle", tackle.snapshot());
    SnapshotAssertions.assertMatches("provider-science", science.snapshot());
  }

  @Test
  void rejectsProviderCountsOutsideSupportedRanges() {
    DiscordEventHarness atm = new DiscordEventHarness();
    commands.onSlashAtm(atm.slashEvent(), 0);

    DiscordEventHarness tackle = new DiscordEventHarness();
    commands.onSlashTackle(tackle.slashEvent(), 49);

    DiscordEventHarness science = new DiscordEventHarness();
    commands.onSlashScience(science.slashEvent(), 500_001);

    assertAll(
        () -> org.junit.jupiter.api.Assertions.assertTrue(atm.snapshot().contains("1 and 500,000")),
        () ->
            org.junit.jupiter.api.Assertions.assertTrue(
                tackle.snapshot().contains("50 and 500,000")),
        () ->
            org.junit.jupiter.api.Assertions.assertTrue(
                science.snapshot().contains("50 and 500,000")));
  }
}
