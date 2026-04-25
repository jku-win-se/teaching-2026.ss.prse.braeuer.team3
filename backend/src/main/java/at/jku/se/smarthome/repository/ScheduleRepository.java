package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.Schedule;
import at.jku.se.smarthome.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Schedule} entities.
 *
 * <p>Provides queries needed by {@link at.jku.se.smarthome.service.ScheduleService}
 * for CRUD operations and the minute-based polling scheduler.</p>
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Returns all schedules associated with the given device.
     *
     * @param device the target device
     * @return list of schedules for that device, in no particular order
     */
    List<Schedule> findByDevice(Device device);

    /**
     * Returns all enabled schedules configured for the given hour and minute.
     * Used by the {@code @Scheduled} polling method to find schedules due to run.
     *
     * @param hour   the hour of day (0–23)
     * @param minute the minute of hour (0–59)
     * @return list of enabled schedules matching the given time
     */
    List<Schedule> findByEnabledTrueAndHourAndMinute(int hour, int minute);

    /**
     * Returns all schedules whose device is in the given list.
     * Used to retrieve all schedules visible to a user (across all their devices).
     *
     * @param devices list of devices owned by the user
     * @return list of schedules for those devices
     */
    List<Schedule> findByDeviceIn(List<Device> devices);

    /**
     * Returns the schedule with the given id if it belongs to the given user.
     * Ownership is verified via the device → room → user relationship.
     *
     * @param id   the schedule's primary key
     * @param user the owning user
     * @return an {@link Optional} containing the schedule, or empty if not found or not owned
     */
    Optional<Schedule> findByIdAndDeviceRoomUser(Long id, User user);
}
