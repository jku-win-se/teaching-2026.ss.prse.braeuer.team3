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
     * <p>Activates the {@code local} profile automatically when no explicit profile
     * is set via {@code spring.profiles.active} (system property or env var).
     * When running as a packaged desktop app, the launcher sets
     * {@code -Dspring.profiles.active=dist}, which prevents the {@code local}
     * profile from being added.</p>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SmarthomeApplication.class);
        if (isNoProfileExplicitlySet()) {
            app.setAdditionalProfiles("local");
        }
        app.run(args);
    }

    /**
     * Returns {@code true} when neither the {@code spring.profiles.active} system
     * property nor the {@code SPRING_PROFILES_ACTIVE} environment variable is set.
     *
     * @return {@code true} if no Spring profile has been explicitly configured
     */
    private static boolean isNoProfileExplicitlySet() {
        String sysProp = System.getProperty("spring.profiles.active", "");
        String envVar = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "");
        return sysProp.isEmpty() && envVar.isEmpty();
    }
}
