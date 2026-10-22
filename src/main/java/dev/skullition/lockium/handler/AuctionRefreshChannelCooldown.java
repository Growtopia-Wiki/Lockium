package dev.skullition.lockium.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.skullition.lockium.util.ContainerUtil;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.github.freya022.botcommands.api.commands.ratelimit.bucket.Buckets;
import io.github.freya022.botcommands.api.components.ComponentInteractionFilter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Applies a shared per-channel cooldown to auction Refresh buttons. */
@Service
public final class AuctionRefreshChannelCooldown implements ComponentInteractionFilter {
  private static final Duration CHANNEL_COOLDOWN = Duration.ofSeconds(5);
  private static final Duration BUCKET_IDLE_EXPIRY = Duration.ofMinutes(1);
  private static final BucketConfiguration BUCKET_CONFIGURATION =
      Buckets.ofCooldown(CHANNEL_COOLDOWN);

  private final Clock clock;
  private final TimeMeter timeMeter;
  private final Cache<Long, Bucket> channelBuckets;

  /** Creates the filter with the system clock and Bucket4j's monotonic time source. */
  @Autowired
  public AuctionRefreshChannelCooldown() {
    this(TimeMeter.SYSTEM_NANOTIME, Clock.systemUTC());
  }

  AuctionRefreshChannelCooldown(TimeMeter timeMeter, Clock clock) {
    this.timeMeter = Objects.requireNonNull(timeMeter);
    this.clock = Objects.requireNonNull(clock);
    this.channelBuckets =
        Caffeine.newBuilder()
            .expireAfterAccess(BUCKET_IDLE_EXPIRY)
            .ticker(timeMeter::currentTimeNanos)
            .build();
  }

  @Override
  public boolean getGlobal() {
    return false;
  }

  @Override
  public @Nullable String check(
      GenericComponentInteractionCreateEvent event, @Nullable String handlerName) {
    Bucket bucket = channelBuckets.get(event.getChannelIdLong(), ignored -> newChannelBucket());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return null;
    }

    long retryNanos = Math.max(0, probe.getNanosToWaitForRefill());
    retryNanos = Math.min(retryNanos, CHANNEL_COOLDOWN.toNanos());
    Instant retryAt = clock.instant().plusNanos(retryNanos);
    Container notice =
        ContainerUtil.createGenericContainer(
            TextDisplay.of(
                "Auction refresh is cooling down in this channel. Try again "
                    + TimeFormat.RELATIVE.format(retryAt)
                    + "."));
    event
        .replyComponents(notice)
        .setEphemeral(true)
        .setAllowedMentions(List.of())
        .useComponentsV2()
        .queue();
    return "Auction refresh channel cooldown is active";
  }

  private Bucket newChannelBucket() {
    var builder = Bucket.builder().withCustomTimePrecision(timeMeter);
    for (var bandwidth : BUCKET_CONFIGURATION.getBandwidths()) {
      builder.addLimit(bandwidth);
    }
    return builder.build();
  }
}
