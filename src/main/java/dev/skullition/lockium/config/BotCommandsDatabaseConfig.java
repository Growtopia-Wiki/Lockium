package dev.skullition.lockium.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.freya022.botcommands.api.core.db.HikariSourceSupplier;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the application's {@link DataSource} to BotCommands.
 *
 * <p>BotCommands reaches JDBC through a {@link HikariSourceSupplier}. Spring Boot already
 * autoconfigures a {@link HikariDataSource} from {@code spring.datasource.*}, so this adapts the
 * existing pool rather than creating a second one: one pool, configured in one place, owned by
 * Spring, borrowed by the framework.
 *
 * <p>Registering this bean enables BotCommands' database-backed features, such as persistent
 * components and the database application-commands cache.
 *
 * <p><b>Consequence:</b> with this bean present BotCommands validates its own schema during startup
 * and refuses to start unless {@code bc.bc_version} reads {@code 3.0.0}. {@link FlywayConfig} is
 * what puts it there, which is why the framework migration runs ahead of the application's own.
 *
 * @see FlywayConfig
 */
@Configuration
public class BotCommandsDatabaseConfig {
  private static final Logger logger = LoggerFactory.getLogger(BotCommandsDatabaseConfig.class);

  /**
   * Adapts the autoconfigured Hikari pool to the interface BotCommands expects.
   *
   * <p>BotCommands derives its connection limit and transaction-duration warnings from the pool's
   * own settings, so {@code spring.datasource.hikari.*} governs both frameworks.
   *
   * @param dataSource the autoconfigured data source, which Spring Boot backs with HikariCP
   * @return a supplier handing BotCommands the same pool the application uses
   */
  @Bean
  public HikariSourceSupplier hikariSourceSupplier(DataSource dataSource) {
    if (!(dataSource instanceof HikariDataSource hikari)) {
      throw new IllegalStateException(
          "BotCommands requires a HikariDataSource, but the configured data source is %s"
              .formatted(dataSource.getClass().getName()));
    }
    logger.info("Exposing connection pool {} to BotCommands", hikari.getPoolName());
    return () -> hikari;
  }
}
