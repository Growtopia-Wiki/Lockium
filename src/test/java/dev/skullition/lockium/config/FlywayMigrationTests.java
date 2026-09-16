package dev.skullition.lockium.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * Tests that both Flyway timelines migrate onto H2 in PostgreSQL compatibility mode.
 *
 * <p>Lockium owns the {@code lockium} schema; BotCommands ships its own scripts inside its jar and
 * owns the {@code bc} schema. BotCommands validates its schema version in the {@code DatabaseImpl}
 * constructor and refuses to start unless {@code bc.bc_version} reads exactly {@code 3.0.0}, so a
 * migration failure here would be a startup failure in production. Running both timelines offline
 * catches that at build time instead.
 *
 * <p>Each test uses a uniquely named in-memory database so they never share state.
 */
class FlywayMigrationTests {

  /** Connection settings mirroring {@code spring.datasource.url} in {@code application.properties}. */
  private static String jdbcUrl() {
    return "jdbc:h2:mem:flyway-%s;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
        .formatted(UUID.randomUUID());
  }

  @Test
  void lockiumSchemaCreatesThePlayerCountSampleTable() throws SQLException {
    String url = jdbcUrl();
    migrateLockium(url);

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          INSERT INTO lockium.player_count_sample (sampled_at, online_count)
          VALUES (TIMESTAMP WITH TIME ZONE '2026-09-16 12:00:00+00', 37308)
          """);
      try (ResultSet rs =
          statement.executeQuery("SELECT online_count FROM lockium.player_count_sample")) {
        assertTrue(rs.next());
        assertEquals(37308, rs.getInt("online_count"));
      }
    }
  }

  @Test
  void lockiumSchemaAcceptsDuplicateTimestamps() throws SQLException {
    String url = jdbcUrl();
    migrateLockium(url);

    // A scheduler tick landing alongside an /owner reload must not violate a constraint.
    Instant at = Instant.parse("2026-09-16T12:00:00Z");
    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      insertSample(connection, at, 100);
      insertSample(connection, at, 200);

      try (Statement statement = connection.createStatement();
          ResultSet rs =
              statement.executeQuery("SELECT count(*) FROM lockium.player_count_sample")) {
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
      }
    }
  }

  @Test
  void botCommandsSchemaMigratesToTheVersionTheFrameworkRequires() throws SQLException {
    String url = jdbcUrl();
    migrateBotCommands(url);

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT version FROM bc.bc_version")) {
      assertTrue(rs.next());
      assertEquals("3.0.0", rs.getString("version"));
    }
  }

  @Test
  void bothTimelinesCoexistInOneDatabase() throws SQLException {
    String url = jdbcUrl();
    migrateBotCommands(url);
    migrateLockium(url);

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery("SELECT version FROM bc.bc_version")) {
        assertTrue(rs.next());
        assertEquals("3.0.0", rs.getString("version"));
      }
      try (ResultSet rs =
          statement.executeQuery("SELECT count(*) FROM lockium.player_count_sample")) {
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
      }
    }
  }

  private static void insertSample(Connection connection, Instant at, int onlineCount)
      throws SQLException {
    try (var ps =
        connection.prepareStatement(
            "INSERT INTO lockium.player_count_sample (sampled_at, online_count) VALUES (?, ?)")) {
      ps.setObject(1, at.atOffset(java.time.ZoneOffset.UTC));
      ps.setInt(2, onlineCount);
      ps.executeUpdate();
    }
  }

  /** Runs Lockium's own migrations, mirroring the {@code spring.flyway.*} properties. */
  private static void migrateLockium(String url) {
    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("lockium")
        .defaultSchema("lockium")
        .createSchemas(true)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  /** Runs the BotCommands framework migrations, mirroring {@code FlywayConfig}. */
  private static void migrateBotCommands(String url) {
    Flyway.configure()
        .dataSource(url, "sa", "")
        .schemas("bc")
        .defaultSchema("bc")
        .createSchemas(true)
        .table("bc_schema_history")
        .locations("classpath:bc_database_scripts")
        .load()
        .migrate();
  }
}
