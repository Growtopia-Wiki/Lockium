package dev.skullition.lockium.command;

import dev.skullition.lockium.handler.AuctionRefreshChannelCooldown;
import dev.skullition.lockium.model.GrowtopiaAuction;
import dev.skullition.lockium.model.ProxyPayload;
import dev.skullition.lockium.properties.AuctionProperties;
import dev.skullition.lockium.service.GrowtopiaAuctionService;
import dev.skullition.lockium.util.ContainerUtil;
import dev.skullition.lockium.util.ItemUtils;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.components.Buttons;
import io.github.freya022.botcommands.api.components.event.ButtonEvent;
import io.github.freya022.botcommands.api.localization.interaction.LocalizableInteractionHook;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only auction browsing over the shared proxy snapshot.
 *
 * <p>Each invocation owns a temporary browser: page changes use its frozen snapshot, while Refresh
 * replaces that snapshot from memory. Neither action performs an upstream request. User-restricted
 * component groups expire within ten minutes and are replaced on each edit to prevent stale clicks.
 */
@Command
public class AuctionCommands {
  private static final Logger logger = LoggerFactory.getLogger(AuctionCommands.class);
  private static final Duration VIEW_LIFETIME = Duration.ofMinutes(10);
  private static final int MAX_FILTER_LENGTH = 100;

  private final GrowtopiaAuctionService service;
  private final AuctionControlFactory controls;
  private final Duration maxAge;
  private final Clock clock;
  private final AuctionPageRenderer renderer;

  /** Creates the command using the framework's temporary button registry. */
  @Autowired
  public AuctionCommands(
      GrowtopiaAuctionService service,
      Buttons buttons,
      AuctionProperties properties,
      AuctionRefreshChannelCooldown channelCooldown) {
    this(
        service,
        AuctionControlFactory.using(buttons, channelCooldown),
        properties,
        Clock.systemUTC());
  }

  /** Creates a browser with deterministic controls and time for isolated command tests. */
  AuctionCommands(
      GrowtopiaAuctionService service,
      AuctionControlFactory controls,
      AuctionProperties properties,
      Clock clock) {
    this.service = service;
    this.controls = controls;
    this.maxAge = properties.maxAge();
    this.clock = clock;
    this.renderer = new AuctionPageRenderer(clock, maxAge);
  }

  /**
   * Opens the first page of auction listings, optionally filtered by item name.
   *
   * @param event the slash interaction
   * @param item optional item-name substring, limited to 100 characters
   */
  @JDASlashCommand(
      name = "gt",
      subcommand = "auction",
      description = "Browse Growtopia auction listings.")
  public void onSlashAuction(
      GlobalSlashEvent event,
      @SlashOption(description = "Filter by part of an item name.") @Nullable String item) {
    if (item != null && item.length() > MAX_FILTER_LENGTH) {
      event
          .replyComponents(notice("Auction item filters must be at most 100 characters."))
          .setAllowedMentions(List.of())
          .useComponentsV2()
          .queue();
      return;
    }
    var state = service.getState();
    View view =
        new View(
            event.getUser().getIdLong(),
            item == null ? "" : ItemUtils.norm(item),
            clock.instant().plus(VIEW_LIFETIME),
            event.getHook(),
            state.payload());
    synchronized (view) {
      Container container = render(view, state.failedAt());
      event
          .replyComponents(container)
          .setAllowedMentions(List.of())
          .useComponentsV2()
          .queue(
              null,
              failure -> {
                logger.warn(
                    "Could not send auction browser: {}", failure.getClass().getSimpleName());
                close(view);
              });
    }
  }

  private void interact(View view, long generation, ButtonEvent event, Navigation navigation) {
    if (event.getUser().getIdLong() != view.ownerId) {
      event
          .replyComponents(notice("Only the person who opened this auction browser can use it."))
          .setEphemeral(true)
          .setAllowedMentions(List.of())
          .useComponentsV2()
          .queue();
      return;
    }
    synchronized (view) {
      Duration remaining = lifetime(view);
      if (view.expired || remaining.isZero() || remaining.isNegative()) {
        close(view);
        event
            .editComponents(renderer.expired())
            .setAllowedMentions(List.of())
            .useComponentsV2()
            .queue();
        return;
      }
      if (generation != view.generation) {
        event
            .replyComponents(notice("This page has changed. Please use its current buttons."))
            .setEphemeral(true)
            .setAllowedMentions(List.of())
            .useComponentsV2()
            .queue();
        return;
      }
      var state = service.getState();
      if (navigation == Navigation.REFRESH) {
        view.payload = state.payload();
        view.page = 0;
      } else {
        view.page += navigation == Navigation.NEXT ? 1 : -1;
      }
      Container container = render(view, state.failedAt());
      event
          .editComponents(container)
          .setAllowedMentions(List.of())
          .useComponentsV2()
          .queue(
              null,
              failure -> {
                logger.warn(
                    "Could not edit auction browser: {}", failure.getClass().getSimpleName());
                close(view);
              });
    }
  }

  /** Replaces the old component group so its timeout cannot expire a newer refreshed page. */
  private Container render(View view, @Nullable Instant failedAt) {
    Duration remaining = lifetime(view);
    if (view.expired || remaining.isZero() || remaining.isNegative()) {
      close(view);
      return renderer.expired();
    }
    var page = renderer.render(view.payload, failedAt, view.filter, view.page);
    view.page = page.pageIndex();
    long generation = ++view.generation;
    var replacement =
        controls.create(
            view.ownerId,
            remaining,
            event -> interact(view, generation, event, Navigation.PREVIOUS),
            event -> interact(view, generation, event, Navigation.NEXT),
            event -> interact(view, generation, event, Navigation.REFRESH),
            () -> expire(view, generation));
    var previous = view.controls;
    view.controls = replacement;
    if (previous != null) {
      dispose(previous);
    }
    return page.container(replacement.forPage(page.pageIndex(), page.pageCount()));
  }

  private Duration lifetime(View view) {
    Instant now = clock.instant();
    Duration remaining = Duration.between(now, view.expiresAt);
    var payload = view.payload;
    if (payload != null) {
      Duration age = Duration.between(payload.fetchedAt(), now);
      Duration snapshotRemaining = maxAge.minus(age.isNegative() ? Duration.ZERO : age);
      if (snapshotRemaining.compareTo(remaining) < 0) {
        remaining = snapshotRemaining;
      }
    }
    return remaining;
  }

  private void expire(View view, long generation) {
    synchronized (view) {
      if (view.expired || generation != view.generation) {
        return;
      }
      close(view);
      view.hook
          .editOriginalComponents(renderer.expired())
          .setAllowedMentions(List.of())
          .useComponentsV2()
          .queue(
              null,
              failure ->
                  logger.debug(
                      "Could not expire auction browser message: {}",
                      failure.getClass().getSimpleName()));
    }
  }

  private static void close(View view) {
    synchronized (view) {
      view.expired = true;
      var active = view.controls;
      view.controls = null;
      if (active != null) {
        dispose(active);
      }
    }
  }

  private static void dispose(AuctionControlFactory.Controls controls) {
    try {
      controls.dispose().run();
    } catch (RuntimeException failure) {
      logger.debug("Could not remove auction controls: {}", failure.getClass().getSimpleName());
    }
  }

  private static Container notice(String text) {
    return ContainerUtil.createGenericContainer(TextDisplay.of(text));
  }

  private enum Navigation {
    PREVIOUS,
    NEXT,
    REFRESH
  }

  /** Mutable navigation state guarded by the view monitor, never shared between invocations. */
  private static final class View {
    private final long ownerId;
    private final String filter;
    private final Instant expiresAt;
    private final LocalizableInteractionHook hook;
    private @Nullable ProxyPayload<GrowtopiaAuction> payload;
    private AuctionControlFactory.@Nullable Controls controls;
    private int page;
    private long generation;
    private boolean expired;

    private View(
        long ownerId,
        String filter,
        Instant expiresAt,
        LocalizableInteractionHook hook,
        @Nullable ProxyPayload<GrowtopiaAuction> payload) {
      this.ownerId = ownerId;
      this.filter = filter;
      this.expiresAt = expiresAt;
      this.hook = hook;
      this.payload = payload;
    }
  }
}
