package dev.skullition.lockium;

import dev.skullition.lockium.config.SecretsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "dev.skullition.lockium")
@EnableConfigurationProperties(SecretsConfig.class)
public class LockiumApplication {

	static void main(String[] args) {
		SpringApplication.run(LockiumApplication.class, args);
	}

}
