package at.jku.se.smarthome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the SmartHome Orchestrator Spring Boot application.
 */
@SpringBootApplication
@EnableScheduling
public class SmarthomeApplication {

    /**
     * Main method — starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SmarthomeApplication.class);
        app.setAdditionalProfiles("local");
        app.run(args);
    }
}
