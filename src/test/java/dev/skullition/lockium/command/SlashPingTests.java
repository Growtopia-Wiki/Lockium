package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.service.WikiService;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.RestAction;
import org.junit.jupiter.api.Test;

/** Tests the deferred ping response without making Discord or Wiki requests. */
class SlashPingTests {
  @Test
  void snapshotsSuccessfulPing() throws IOException {
    WikiService wiki = mock(WikiService.class);
    SlashPing command = new SlashPing(wiki, sequence(10_000_000L, 15_000_000L));
    DiscordEventHarness harness = new DiscordEventHarness();
    GlobalSlashEvent event = eventWithDiscordPing(harness, 42L);

    command.onSlashPing(event);

    SnapshotAssertions.assertMatches("ping", harness.snapshot());
  }

  @Test
  void reportsWikiFailureAsDown() {
    WikiService wiki = mock(WikiService.class);
    doThrow(new IllegalStateException("offline")).when(wiki).health();
    SlashPing command = new SlashPing(wiki, sequence(10_000_000L));
    DiscordEventHarness harness = new DiscordEventHarness();

    command.onSlashPing(eventWithDiscordPing(harness, 42L));

    assertTrue(harness.snapshot().contains("Wiki API: DOWN"));
  }

  @SuppressWarnings("unchecked")
  private static GlobalSlashEvent eventWithDiscordPing(DiscordEventHarness harness, long latency) {
    GlobalSlashEvent event = harness.slashEvent();
    JDA jda = mock(JDA.class);
    RestAction<Long> restPing = mock(RestAction.class);
    doAnswer(
            invocation -> {
              Consumer<Long> success = invocation.getArgument(0);
              success.accept(latency);
              return null;
            })
        .when(restPing)
        .queue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    when(jda.getRestPing()).thenReturn(restPing);
    when(event.getJDA()).thenReturn(jda);
    return event;
  }

  private static LongSupplier sequence(long... values) {
    AtomicInteger index = new AtomicInteger();
    return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
  }
}
