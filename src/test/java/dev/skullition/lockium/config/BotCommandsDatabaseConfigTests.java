package dev.skullition.lockium.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.freya022.botcommands.api.core.db.HikariSourceSupplier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Verifies Spring initializes the BotCommands schema before exposing its JDBC supplier. */
class BotCommandsDatabaseConfigTests {

  @Test
  void databaseSupplierIsInitializedBeforeAnEagerBotCommandsConsumer() {
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class))
        .withUserConfiguration(
            FlywayConfig.class, BotCommandsDatabaseConfig.class, EagerConsumerConfiguration.class)
        .withPropertyValues(
            "spring.datasource.url=%s".formatted(jdbcUrl()),
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.hikari.maximum-pool-size=2",
            "spring.flyway.locations=classpath:db/migration",
            "spring.flyway.schemas=lockium",
            "spring.flyway.default-schema=lockium",
            "spring.flyway.create-schemas=true")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(
                      context
                          .getSourceApplicationContext()
                          .getBeanFactory()
                          .getBeanDefinition("hikariSourceSupplier")
                          .getDependsOn())
                  .contains("flywayInitializer");
              assertThat(context).hasSingleBean(BotCommandsSchemaVersion.class);
              assertThat(context.getBean(BotCommandsSchemaVersion.class).version())
                  .isEqualTo("3.0.0");
            });
  }

  private static String jdbcUrl() {
    return ("jdbc:h2:mem:botcommands-init-%s;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
            + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
        .formatted(UUID.randomUUID());
  }

  /** Creates an eager consumer that queries the framework's version table during bean creation. */
  @Configuration(proxyBeanMethods = false)
  static class EagerConsumerConfiguration {
    @Bean
    BotCommandsSchemaVersion botCommandsSchemaVersion(HikariSourceSupplier sourceSupplier)
        throws SQLException {
      try (Connection connection = sourceSupplier.getConnection();
          Statement statement = connection.createStatement();
          ResultSet results = statement.executeQuery("SELECT version FROM bc.bc_version")) {
        if (!results.next()) {
          throw new IllegalStateException("BotCommands schema version row is missing");
        }
        return new BotCommandsSchemaVersion(results.getString("version"));
      }
    }
  }

  /** The schema version read by the eager context consumer. */
  record BotCommandsSchemaVersion(String version) {}
}
