package dev.skullition.lockium.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.freya022.botcommands.api.commands.ratelimit.RateLimitScope;
import io.github.freya022.botcommands.api.commands.ratelimit.declaration.RateLimitManager;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Verifies the BotCommands per-user cooldown declared for Refresh controls. */
class AuctionRefreshCooldownsTests {
  @Test
  void declaresTenSecondUserCooldownWithoutDeletingItsMessage() {
    RateLimitManager manager = mock(RateLimitManager.class);

    new AuctionRefreshCooldowns().declareRateLimit(manager);

    verify(manager)
        .cooldown(
            AuctionRefreshCooldowns.GROUP, Duration.ofSeconds(10), RateLimitScope.USER, false);
  }
}
