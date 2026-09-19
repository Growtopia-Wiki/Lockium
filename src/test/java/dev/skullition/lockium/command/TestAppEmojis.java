package dev.skullition.lockium.command;

import io.github.freya022.botcommands.api.emojis.AppEmojisRegistry;
import net.dv8tion.jda.internal.entities.emoji.ApplicationEmojiImpl;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Installs deterministic application emojis before {@code AppEmojis} is initialized. */
final class TestAppEmojis {
  private TestAppEmojis() {}

  static void initialize() throws ClassNotFoundException {
    try (MockedStatic<AppEmojisRegistry> registry = Mockito.mockStatic(AppEmojisRegistry.class)) {
      registry
          .when(() -> AppEmojisRegistry.get(Mockito.anyString()))
          .thenAnswer(
              invocation -> {
                String name = invocation.getArgument(0);
                long id = Integer.toUnsignedLong(name.hashCode());
                return new ApplicationEmojiImpl(id, null, null).setName(name);
              });
      Class.forName("dev.skullition.lockium.util.AppEmojis");
    }
  }
}
