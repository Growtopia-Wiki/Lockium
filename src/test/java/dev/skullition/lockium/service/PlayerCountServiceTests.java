package dev.skullition.lockium.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.skullition.lockium.model.PlayerCountSample;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Tests the player-count statements against a real in-memory database.
 *
 * <p>Uses H2 with the same PostgreSQL compatibility settings as production and runs the real
 * migration, so this exercises the schema itself rather than a mocked {@link JdbcClient}. Mocking
 * would assert on SQL strings and would not catch, for example, an identifier-case problem caused
 * by {@code DATABASE_TO_LOWER}.
 *
 * <p>Each test gets a uniquely named database, so no state is shared.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PlayerCountServiceTests {

  private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

  @Test
  void recordsAndReadsSamplesInTimeOrder() {
    PlayerCountService service = newService();

    service.record(NOW.minus(Duration.ofMinutes(2)), 100);
    service.record(NOW.minus(Duration.ofMinutes(1)), 200);
    service.record(NOW, 300);

    List<PlayerCountSample> samples = service.since(NOW.minus(Duration.ofHours(1)));

    assertEquals(3, samples.size());
    assertEquals(100, samples.get(0).onlineCount());
    assertEquals(200, samples.get(1).onlineCount());
    assertEquals(300, samples.get(2).onlineCount());
    assertEquals(NOW, samples.get(2).sampledAt());
  }

  @Test
  void sinceExcludesSamplesBeforeTheCutoff() {
    PlayerCountService service = newService();

    service.record(NOW.minus(Duration.ofHours(30)), 1);
    service.record(NOW.minus(Duration.ofHours(2)), 2);

    List<PlayerCountSample> samples = service.since(NOW.minus(Duration.ofHours(24)));

    assertEquals(1, samples.size());
    assertEquals(2, samples.getFirst().onlineCount());
  }

  @Test
  void pruneDeletesOnlyExpiredSamples() {
    PlayerCountService service = newService();

    service.record(NOW.minus(Duration.ofHours(30)), 1);
    service.record(NOW.minus(Duration.ofHours(27)), 2);
    service.record(NOW.minus(Duration.ofHours(1)), 3);

    int deleted = service.prune(NOW.minus(Duration.ofHours(26)));

    assertEquals(2, deleted);
    List<PlayerCountSample> remaining = service.since(Instant.EPOCH);
    assertEquals(1, remaining.size());
    assertEquals(3, remaining.getFirst().onlineCount());
  }

  @Test
  void recordsDuplicateTimestampsWithoutError() {
    PlayerCountService service = newService();

    // A scheduler tick landing alongside /owner reload must not raise a constraint violation.
    service.record(NOW, 100);
    service.record(NOW, 200);

    assertEquals(2, service.since(Instant.EPOCH).size());
  }

  @Test
  void returnsNoSamplesBeforeAnythingIsRecorded() {
    assertTrue(newService().since(Instant.EPOCH).isEmpty());
  }

  /**
   * Creates a service over a freshly migrated, uniquely named in-memory database.
   *
   * @return the service under test
   */
  private static PlayerCountService newService() {
    var dataSource = new DriverManagerDataSource();
    dataSource.setUrl(
        "jdbc:h2:mem:pct-%s;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
            .formatted(UUID.randomUUID()));
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    migrate(dataSource);
    return new PlayerCountService(JdbcClient.create(dataSource));
  }

  /** Runs Lockium's migrations, mirroring the {@code spring.flyway.*} properties. */
  private static void migrate(DataSource dataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .schemas("lockium")
        .defaultSchema("lockium")
        .createSchemas(true)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }
}
