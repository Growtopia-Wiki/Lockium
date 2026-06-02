package dev.skullition.lockium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Lockium Discord bot application.
 *
 * <p>Bootstraps Spring Boot with the following configuration:
 *
 * <ul>
 *   <li>{@link SpringBootApplication} – enables autoconfiguration, component scanning, and
 *       configuration support. Scanning is explicitly limited to {@code dev.skullition.lockium} to
 *       avoid picking up unrelated packages.
 *   <li>{@link ConfigurationPropertiesScan} – registers {@code @ConfigurationProperties} beans in
 *       {@code dev.skullition.lockium.properties} (Discord token, Wiki API URL, bot status, etc.).
 *   <li>{@link EnableScheduling} – activates Spring's scheduler for {@code @Scheduled} tasks, such
 *       as the periodic Wiki cache refresh.
 * </ul>
 *
 * <p>The application is started via the standard Spring Boot mechanism and runs until the JDA
 * connection is closed.
 */
@SpringBootApplication(scanBasePackages = "dev.skullition.lockium")
@ConfigurationPropertiesScan("dev.skullition.lockium.properties")
@EnableScheduling
public class LockiumApplication {

  static void main(String[] args) {
    SpringApplication.run(LockiumApplication.class, args);
  }
}
