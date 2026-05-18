package dev.skullition.lockium;

import dev.skullition.lockium.properties.DiscordProperties;
import dev.skullition.lockium.properties.WikiApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.skullition.lockium")
@EnableConfigurationProperties({DiscordProperties.class, WikiApiProperties.class})
@EnableScheduling
public class LockiumApplication {

	static void main(String[] args) {
		SpringApplication.run(LockiumApplication.class, args);
	}

}
