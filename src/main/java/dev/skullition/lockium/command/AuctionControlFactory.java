package dev.skullition.lockium.command;

import io.github.freya022.botcommands.api.components.Buttons;
import io.github.freya022.botcommands.api.components.event.ButtonEvent;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import net.dv8tion.jda.api.components.buttons.Button;

/** Creates short-lived auction controls, with a narrow seam for deterministic interaction tests. */
@FunctionalInterface
interface AuctionControlFactory {

  /** Registers one generation of controls and its shared expiry callback. */
  Controls create(
      long ownerId,
      Duration lifetime,
      Consumer<ButtonEvent> previous,
      Consumer<ButtonEvent> next,
      Consumer<ButtonEvent> refresh,
      Runnable expired);

  /** Adapts BotCommands' temporary, user-restricted buttons without persisting browser state. */
  static AuctionControlFactory using(Buttons buttons) {
    return (ownerId, lifetime, previous, next, refresh, expired) -> {
      var previousButton =
          buttons
              .secondary("Previous")
              .ephemeral()
              .addUserIds(ownerId)
              .noTimeout()
              .bindTo(previous)
              .build();
      var nextButton =
          buttons
              .secondary("Next")
              .ephemeral()
              .addUserIds(ownerId)
              .noTimeout()
              .bindTo(next)
              .build();
      var refreshButton =
          buttons
              .primary("Refresh")
              .ephemeral()
              .addUserIds(ownerId)
              .noTimeout()
              .bindTo(refresh)
              .build();
      var group =
          buttons
              .group(previousButton, nextButton, refreshButton)
              .ephemeral()
              .resetTimeoutOnUse(false)
              .timeout(lifetime, expired)
              .build();
      return new Controls(
          previousButton, nextButton, refreshButton, () -> buttons.deleteComponents(group));
    };
  }

  /** Buttons and cleanup for a generation; disabled copies retain their registered identifiers. */
  record Controls(Button previous, Button next, Button refresh, Runnable dispose) {

    /** Disables navigation at the first and last page while leaving Refresh available. */
    List<Button> forPage(int index, int count) {
      return List.of(
          previous.withDisabled(index <= 0), next.withDisabled(index >= count - 1), refresh);
    }
  }
}
