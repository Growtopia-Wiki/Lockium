package dev.skullition.lockium.service;

import dev.skullition.lockium.model.PlayerCountSample;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Stores and reads the history of online player counts.
 *
 * <p>Owns every statement against {@code lockium.player_count_sample}. Samples are written by the
 * scheduled {@code /detail} poll and read back to draw the graph shown by {@code /gt stats}.
 *
 * <p>Queries go through Spring's {@link JdbcClient} rather than BotCommands' {@code
 * BlockingDatabase}. BotCommands' service container starts on {@code ApplicationReadyEvent}, so
 * injecting it into a plain {@code @Service} that a scheduler depends on would fight that
 * lifecycle, and it would drag the framework's schema check into unit tests. The database itself is
 * still fully available to BotCommands — see {@code BotCommandsDatabaseConfig}. To switch, inject
 * {@code ObjectProvider<BlockingDatabase>} and resolve it on first use, never in the constructor.
 *
 * <p>Table names are schema-qualified so the statements never depend on the connection's current
 * schema.
 */
@Service
public class PlayerCountService {
  private static final Logger logger = LoggerFactory.getLogger(PlayerCountService.class);

  private final JdbcClient jdbcClient;

  /**
   * Creates the service.
   *
   * @param jdbcClient the autoconfigured JDBC client backed by the application's pool
   */
  public PlayerCountService(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /**
   * Records one observation of the online player count.
   *
   * <p>Duplicate timestamps are accepted deliberately: a scheduler tick landing alongside a manual
   * refresh must not raise a constraint violation, and a repeated sample is harmless to a graph.
   *
   * @param at when the count was observed
   * @param onlineCount number of players online, which must not be negative
   */
  public void record(Instant at, int onlineCount) {
    jdbcClient
        .sql(
            """
            INSERT INTO lockium.player_count_sample (sampled_at, online_count)
            VALUES (?, ?)
            """)
        .param(at.atOffset(ZoneOffset.UTC))
        .param(onlineCount)
        .update();
  }

  /**
   * Reads every sample at or after the given instant, oldest first.
   *
   * @param cutoff the oldest instant to include
   * @return samples in ascending time order; empty when nothing has been recorded yet
   */
  public List<PlayerCountSample> since(Instant cutoff) {
    return jdbcClient
        .sql(
            """
            SELECT sampled_at, online_count
            FROM lockium.player_count_sample
            WHERE sampled_at >= ?
            ORDER BY sampled_at
            """)
        .param(cutoff.atOffset(ZoneOffset.UTC))
        .query(
            (rs, _) ->
                new PlayerCountSample(
                    rs.getObject("sampled_at", OffsetDateTime.class).toInstant(),
                    rs.getInt("online_count")))
        .list();
  }

  /**
   * Deletes samples older than the given instant.
   *
   * @param cutoff the oldest instant to keep
   * @return how many rows were removed
   */
  public int prune(Instant cutoff) {
    int deleted =
        jdbcClient
            .sql("DELETE FROM lockium.player_count_sample WHERE sampled_at < ?")
            .param(cutoff.atOffset(ZoneOffset.UTC))
            .update();
    if (deleted > 0) {
      logger.debug("prune: removed {} expired player-count sample(s)", deleted);
    }
    return deleted;
  }
}
