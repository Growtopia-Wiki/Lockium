package dev.skullition.lockium.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

/** Interaction tests for the auction Refresh channel cooldown. */
class AuctionRefreshChannelCooldownTests {
  private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");

  @Test
  void sharesTheCooldownAcrossUsersInOneChannelAndRepliesWithAnEphemeralV2Deadline() {
    MutableTimeMeter timeMeter = new MutableTimeMeter();
    AuctionRefreshChannelCooldown cooldown = cooldown(timeMeter);
    Interaction firstUser = interaction(100, 1);
    Interaction secondUser = interaction(100, 2);

    assertNull(cooldown.check(firstUser.event, null));
    assertEquals(
        "Auction refresh channel cooldown is active", cooldown.check(secondUser.event, null));

    ArgumentCaptor<Container> containerCaptor = ArgumentCaptor.forClass(Container.class);
    verify(secondUser.event).replyComponents(containerCaptor.capture());
    verify(secondUser.reply).setEphemeral(true);
    verify(secondUser.reply).setAllowedMentions(List.of());
    verify(secondUser.reply).useComponentsV2();
    verify(secondUser.reply).queue();
    String payload =
        new MessageCreateBuilder()
            .setComponents(List.of(containerCaptor.getValue()))
            .useComponentsV2(true)
            .build()
            .toData()
            .toPrettyString();
    assertTrue(payload.contains("Auction refresh is cooling down in this channel."));
    assertTrue(payload.contains("<t:"));
    assertTrue(payload.contains(":R>"));
  }

  @Test
  void channelFilterLeavesUserScopingToTheFramework() {
    AuctionRefreshChannelCooldown cooldown = cooldown(new MutableTimeMeter());

    assertNull(cooldown.check(interaction(100, 1).event, null));
    assertNull(cooldown.check(interaction(200, 1).event, null));
  }

  @Test
  void allowsAnotherRefreshAtTheExactCooldownDeadline() {
    MutableTimeMeter timeMeter = new MutableTimeMeter();
    AuctionRefreshChannelCooldown cooldown = cooldown(timeMeter);

    assertNull(cooldown.check(interaction(100, 1).event, null));
    assertTrue(cooldown.check(interaction(100, 2).event, null) != null);
    timeMeter.advance(Duration.ofSeconds(5).minusNanos(1));
    assertTrue(cooldown.check(interaction(100, 2).event, null) != null);
    timeMeter.advance(Duration.ofNanos(1));
    assertNull(cooldown.check(interaction(100, 3).event, null));
  }

  @Test
  void acceptsAfterIdleExpiryAndRestartsCooldown() {
    MutableTimeMeter timeMeter = new MutableTimeMeter();
    AuctionRefreshChannelCooldown cooldown = cooldown(timeMeter);
    assertNull(cooldown.check(interaction(100, 1).event, null));

    timeMeter.advance(Duration.ofMinutes(2));

    assertNull(cooldown.check(interaction(100, 2).event, null));
    assertTrue(cooldown.check(interaction(100, 3).event, null) != null);
  }

  @Test
  void concurrentClicksInOneChannelAcceptOnlyOneRefresh() throws Exception {
    AuctionRefreshChannelCooldown cooldown = cooldown(new MutableTimeMeter());
    int clickCount = 16;
    ExecutorService executor = Executors.newFixedThreadPool(clickCount);
    CountDownLatch ready = new CountDownLatch(clickCount);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> clicks = new ArrayList<>();
      for (int index = 0; index < clickCount; index++) {
        int userId = index + 1;
        clicks.add(
            executor.submit(
                () -> {
                  Interaction interaction = interaction(100, userId);
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Concurrent click barrier timed out");
                  }
                  return cooldown.check(interaction.event, null) == null;
                }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      int accepted = 0;
      for (Future<Boolean> click : clicks) {
        if (click.get(5, TimeUnit.SECONDS)) {
          accepted++;
        }
      }
      assertEquals(1, accepted);
    } finally {
      executor.shutdownNow();
    }
  }

  private static AuctionRefreshChannelCooldown cooldown(MutableTimeMeter timeMeter) {
    return new AuctionRefreshChannelCooldown(timeMeter, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static Interaction interaction(long channelId, long userId) {
    GenericComponentInteractionCreateEvent event =
        mock(GenericComponentInteractionCreateEvent.class);
    when(event.getChannelIdLong()).thenReturn(channelId);
    User user = mock(User.class);
    when(user.getIdLong()).thenReturn(userId);
    when(event.getUser()).thenReturn(user);
    ReplyCallbackAction reply = mock(ReplyCallbackAction.class, Answers.RETURNS_SELF);
    doReturn(reply).when(event).replyComponents(any(Container.class));
    return new Interaction(event, reply);
  }

  private record Interaction(
      GenericComponentInteractionCreateEvent event, ReplyCallbackAction reply) {}

  private static final class MutableTimeMeter implements TimeMeter {
    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long currentTimeNanos() {
      return nanos.get();
    }

    @Override
    public boolean isWallClockBased() {
      return false;
    }

    private void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }
}
