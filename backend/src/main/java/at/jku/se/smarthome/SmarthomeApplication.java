package at.jku.se.smarthome;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Entry point for the SmartHome Orchestrator Spring Boot application.
 */
@SpringBootApplication
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

    /**
     * Tests the database connection on startup.
     *
     * @param jdbcTemplate the JDBC template used to execute the test query
     * @return a CommandLineRunner that verifies connectivity
     */
    @Bean
    public CommandLineRunner testDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {

            System.out.println("\n Wenn exception geworfen wird -> Datenbankverbindung fehlerhaft, ansonsten iO \n");
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        };
    }
};
