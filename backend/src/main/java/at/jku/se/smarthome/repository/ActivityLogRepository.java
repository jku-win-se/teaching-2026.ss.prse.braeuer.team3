package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.ActivityLog;
import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ActivityLog} entities.
 *
 * <p>All query methods are scoped by {@link User} so that users can only
 * access their own log entries. Supports optional filtering by date range
 * and by specific device for FR-08.</p>
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /**
     * Returns a page of log entries for a user, filtered by date range and device.
     *
     * @param user     the owning user
     * @param from     the inclusive start of the date range
     * @param to       the inclusive end of the date range
     * @param device   the device to filter by
     * @param pageable pagination and sort parameters
     * @return a page of matching log entries
     */
    Page<ActivityLog> findByUserAndTimestampBetweenAndDevice(
            User user, Instant from, Instant to, Device device, Pageable pageable);

    /**
     * Returns a page of log entries for a user, filtered by date range only.
     *
     * @param user     the owning user
     * @param from     the inclusive start of the date range
     * @param to       the inclusive end of the date range
     * @param pageable pagination and sort parameters
     * @return a page of matching log entries
     */
    Page<ActivityLog> findByUserAndTimestampBetween(
            User user, Instant from, Instant to, Pageable pageable);

    /**
     * Returns all log entries for a user without date or device filtering.
     *
     * @param user     the owning user
     * @param pageable pagination and sort parameters
     * @return a page of log entries
     */
    Page<ActivityLog> findByUser(User user, Pageable pageable);

    /**
     * Returns a page of log entries for a user filtered by device only (no date bounds).
     *
     * @param user     the owning user
     * @param device   the device to filter by
     * @param pageable pagination and sort parameters
     * @return a page of matching log entries
     */
    Page<ActivityLog> findByUserAndDevice(User user, Device device, Pageable pageable);

    /**
     * Finds a single log entry by id, scoped to the given user.
     * Used by the delete operation to verify ownership.
     *
     * @param id   the log entry primary key
     * @param user the owning user
     * @return an optional containing the entry if found and owned by the user
     */
    Optional<ActivityLog> findByIdAndUser(Long id, User user);
}
