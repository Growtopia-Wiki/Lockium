package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.skullition.lockium.handler.AuctionRefreshChannelCooldown;
import dev.skullition.lockium.service.AuctionRefreshCooldowns;
import io.github.freya022.botcommands.api.components.Button;
import io.github.freya022.botcommands.api.components.Buttons;
import io.github.freya022.botcommands.api.components.IGroupHolder;
import io.github.freya022.botcommands.api.components.builder.button.ButtonFactory;
import io.github.freya022.botcommands.api.components.builder.button.EphemeralButtonBuilder;
import io.github.freya022.botcommands.api.components.builder.group.ComponentGroupFactory;
import io.github.freya022.botcommands.api.components.builder.group.EphemeralComponentGroupBuilder;
import io.github.freya022.botcommands.api.components.ratelimit.ComponentRateLimitReference;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Tests that refresh controls share their rate limit while navigation remains unrestricted. */
class AuctionControlFactoryTests {
  @Test
  void sharesOneRefreshRateLimitReferenceAndRestrictsOnlyRefreshByChannel() {
    Buttons buttons = mock(Buttons.class);
    AuctionRefreshChannelCooldown channelCooldown = mock(AuctionRefreshChannelCooldown.class);
    ComponentRateLimitReference sharedReference =
        new ComponentRateLimitReference(AuctionRefreshCooldowns.GROUP, "shared-refresh");
    when(buttons.createRateLimitReference(AuctionRefreshCooldowns.GROUP, "shared-refresh"))
        .thenReturn(sharedReference);

    ButtonFactory previousFactory = mock(ButtonFactory.class);
    ButtonFactory nextFactory = mock(ButtonFactory.class);
    ButtonFactory refreshFactory = mock(ButtonFactory.class);
    EphemeralButtonBuilder previousBuilder = mock(EphemeralButtonBuilder.class, RETURNS_SELF);
    EphemeralButtonBuilder nextBuilder = mock(EphemeralButtonBuilder.class, RETURNS_SELF);
    EphemeralButtonBuilder refreshBuilder = mock(EphemeralButtonBuilder.class, RETURNS_SELF);
    when(buttons.secondary("Previous")).thenReturn(previousFactory);
    when(buttons.secondary("Next")).thenReturn(nextFactory);
    when(buttons.primary("Refresh")).thenReturn(refreshFactory);
    when(previousFactory.ephemeral()).thenReturn(previousBuilder);
    when(nextFactory.ephemeral()).thenReturn(nextBuilder);
    when(refreshFactory.ephemeral()).thenReturn(refreshBuilder);
    when(previousBuilder.build()).thenReturn(mock(Button.class));
    when(nextBuilder.build()).thenReturn(mock(Button.class));
    when(refreshBuilder.build()).thenReturn(mock(Button.class));

    ComponentGroupFactory groupFactory = mock(ComponentGroupFactory.class);
    EphemeralComponentGroupBuilder groupBuilder =
        mock(EphemeralComponentGroupBuilder.class, RETURNS_SELF);
    when(buttons.group(any(IGroupHolder[].class))).thenReturn(groupFactory);
    when(groupFactory.ephemeral()).thenReturn(groupBuilder);

    AuctionControlFactory factory = AuctionControlFactory.using(buttons, channelCooldown);
    assertNotNull(create(factory, 101L));
    assertNotNull(create(factory, 202L));

    verify(buttons, times(1))
        .createRateLimitReference(AuctionRefreshCooldowns.GROUP, "shared-refresh");
    verify(refreshBuilder, times(2)).rateLimitReference(same(sharedReference));
    verify(refreshBuilder, times(2)).addFilter(same(channelCooldown));
    verify(refreshBuilder).addUserIds(101L);
    verify(refreshBuilder).addUserIds(202L);

    verify(previousBuilder, never()).rateLimitReference(any(ComponentRateLimitReference.class));
    verify(nextBuilder, never()).rateLimitReference(any(ComponentRateLimitReference.class));
    verify(previousBuilder, never()).addFilter(same(channelCooldown));
    verify(nextBuilder, never()).addFilter(same(channelCooldown));
  }

  private static AuctionControlFactory.Controls create(
      AuctionControlFactory factory, long ownerId) {
    return factory.create(
        ownerId, Duration.ofMinutes(10), ignored -> {}, ignored -> {}, ignored -> {}, () -> {});
  }
}
