package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.MqttConfig;
import at.jku.se.smarthome.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MqttConfig} entities (US-019).
 */
public interface MqttConfigRepository extends JpaRepository<MqttConfig, Long> {

    /**
     * Finds the MQTT configuration for the given user.
     *
     * @param user the owner of the configuration
     * @return an {@link Optional} containing the config, or empty if not yet created
     */
    Optional<MqttConfig> findByUser(User user);
}
