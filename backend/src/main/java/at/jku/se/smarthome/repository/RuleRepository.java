package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.Rule;
import at.jku.se.smarthome.domain.TriggerType;
import at.jku.se.smarthome.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Rule} entities.
 *
 * <p>Provides queries needed by {@link at.jku.se.smarthome.service.RuleService}
 * for CRUD operations and reactive rule evaluation.</p>
 */
public interface RuleRepository extends JpaRepository<Rule, Long> {

    /**
     * Returns all rules owned by the given user.
     *
     * @param user the owning user
     * @return list of rules for that user, in no particular order
     */
    List<Rule> findByUser(User user);

    /**
     * Returns all enabled rules whose trigger device matches the given device.
     * Used by the reactive evaluation pipeline after every device state update.
     *
     * @param device the device that was just updated
     * @return list of enabled rules watching that device
     */
    List<Rule> findByEnabledTrueAndTriggerDevice(Device device);

    /**
     * Returns the rule with the given id if it belongs to the given user.
     *
     * @param id   the rule's primary key
     * @param user the owning user
     * @return an {@link Optional} containing the rule, or empty if not found or not owned
     */
    Optional<Rule> findByIdAndUser(Long id, User user);

    /**
     * Returns all enabled TIME rules scheduled for the given hour and minute.
     * Used by {@link at.jku.se.smarthome.service.RuleScheduler} every minute.
     *
     * @param triggerType  must be {@link TriggerType#TIME}
     * @param triggerHour  current hour (0–23)
     * @param triggerMinute current minute (0–59)
     * @return list of matching enabled TIME rules
     */
    List<Rule> findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
            TriggerType triggerType, int triggerHour, int triggerMinute);

    /**
     * Returns all enabled rules owned by the given user that target the given action device.
     * Used by conflict detection (US-014) to find rules that compete for the same device.
     *
     * @param user         the owning user
     * @param actionDevice the device being controlled by the rule's action
     * @return list of enabled rules targeting that action device for that user
     */
    List<Rule> findByEnabledTrueAndUserAndActionDevice(User user, Device actionDevice);
}
