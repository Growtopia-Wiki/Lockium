package dev.skullition.lockium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.skullition.lockium")
@ConfigurationPropertiesScan("dev.skullition.lockium.properties")
@EnableScheduling
public class LockiumApplication {

    static void main(String[] args) {
        SpringApplication.run(LockiumApplication.class, args);
    }

}
