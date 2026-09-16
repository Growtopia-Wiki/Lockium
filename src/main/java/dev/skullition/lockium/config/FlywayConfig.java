package dev.skullition.lockium.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs the two independent migration timelines this application needs.
 *
 * <p>BotCommands ships its schema as Flyway scripts inside its own jar ({@code
 * classpath:bc_database_scripts}), targeting a {@code bc} schema with its own history table.
 * Lockium's tables live in the {@code lockium} schema with their own history, configured through
 * the usual {@code spring.flyway.*} properties.
 *
 * <p>A single {@link Flyway} instance cannot own two timelines, and declaring a {@code Flyway} bean
 * would disable Spring Boot's autoconfigured one — {@code FlywayAutoConfiguration} backs off on
 * {@code @ConditionalOnMissingBean}. Running the framework migration from inside the migration
 * strategy keeps both, and guarantees the framework schema exists before BotCommands validates it.
 *
 * <p>BotCommands refuses to start unless {@code bc.bc_version} reads exactly {@code 3.0.0}. Its
 * scripts include a baseline migration that reaches that state in one step, so the older
 * PostgreSQL-specific statements in the versioned chain never run against H2.
 *
 * @see BotCommandsDatabaseConfig
 */
@Configuration
public class FlywayConfig {
  private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

  /** Location of the migration scripts packaged inside the BotCommands jar. */
  private static final String BC_SCRIPT_LOCATION = "classpath:bc_database_scripts";

  /** Schema owned by the BotCommands framework. */
  private static final String BC_SCHEMA = "bc";

  /**
   * Migrates the BotCommands schema, then the application's own.
   *
   * @param dataSource the pooled data source shared by both timelines
   * @return a strategy that runs both migrations in order
   */
  @Bean
  public FlywayMigrationStrategy lockiumFlywayMigrationStrategy(DataSource dataSource) {
    return applicationFlyway -> {
      logger.info("Migrating the BotCommands schema");
      var result =
          Flyway.configure()
              .dataSource(dataSource)
              // The scripts open with SET SCHEMA 'bc' but never create it.
              .schemas(BC_SCHEMA)
              .defaultSchema(BC_SCHEMA)
              .createSchemas(true)
              // A dedicated history table keeps the two timelines from interleaving.
              .table("bc_schema_history")
              .locations(BC_SCRIPT_LOCATION)
              .load()
              .migrate();
      logger.info(
          "BotCommands schema at version {} after applying {} migration(s)",
          result.targetSchemaVersion,
          result.migrationsExecuted);

      logger.info("Migrating the Lockium schema");
      applicationFlyway.migrate();
    };
  }
}
