package dev.skullition.lockium.service;

import io.github.freya022.botcommands.api.commands.ratelimit.RateLimitScope;
import io.github.freya022.botcommands.api.commands.ratelimit.declaration.RateLimitManager;
import io.github.freya022.botcommands.api.commands.ratelimit.declaration.RateLimitProvider;
import java.time.Duration;
import org.springframework.stereotype.Service;

/** Declares the shared per-user cooldown for auction refresh buttons. */
@Service
public class AuctionRefreshCooldowns implements RateLimitProvider {
  public static final String GROUP = "auction-refresh";
  private static final Duration USER_COOLDOWN = Duration.ofSeconds(10);

  @Override
  public void declareRateLimit(RateLimitManager manager) {
    manager.cooldown(GROUP, USER_COOLDOWN, RateLimitScope.USER, false);
  }
}
