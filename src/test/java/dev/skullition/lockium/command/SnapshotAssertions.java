package dev.skullition.lockium.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads response snapshots and optionally rewrites them during intentional updates. */
final class SnapshotAssertions {
  private static final Path SNAPSHOT_DIRECTORY = Path.of("src", "test", "resources", "snapshots");

  private SnapshotAssertions() {}

  static void assertMatches(String name, String actual) throws IOException {
    Path snapshot = SNAPSHOT_DIRECTORY.resolve(name + ".txt");
    String normalized = actual.replace("\r\n", "\n") + "\n";
    if (Boolean.getBoolean("updateSnapshots")) {
      Files.createDirectories(snapshot.getParent());
      Files.writeString(snapshot, normalized, StandardCharsets.UTF_8);
    }
    assertEquals(Files.readString(snapshot, StandardCharsets.UTF_8), normalized);
  }
}
