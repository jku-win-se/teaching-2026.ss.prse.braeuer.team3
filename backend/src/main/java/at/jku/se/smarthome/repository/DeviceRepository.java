package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Device} entities.
 *
 * <p>Provides room-scoped queries for virtual devices.</p>
 */
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * Returns all devices in a room, ordered by creation date ascending.
     *
     * @param roomId the room's primary key
     * @return list of devices belonging to that room
     */
    List<Device> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    /**
     * Finds a device by its id within a specific room.
     * Returns empty if the device does not exist or belongs to a different room.
     *
     * @param id     the device's primary key
     * @param roomId the room's primary key
     * @return an optional containing the device, or empty if not found
     */
    java.util.Optional<Device> findByIdAndRoomId(Long id, Long roomId);

    /**
     * Checks whether a device with the given name already exists in the room.
     *
     * @param roomId the room's primary key
     * @param name   the device name to check
     * @return {@code true} if a device with that name already exists in the room
     */
    boolean existsByRoomIdAndName(Long roomId, String name);

    /**
     * Checks whether another device (excluding the given id) with the same name
     * exists in the room. Used during rename to allow keeping the same name.
     *
     * @param roomId the room's primary key
     * @param name   the device name to check
     * @param id     the id of the device being renamed (excluded from the check)
     * @return {@code true} if a different device with that name already exists in the room
     */
    boolean existsByRoomIdAndNameAndIdNot(Long roomId, String name, Long id);

    /**
     * Returns all devices belonging to rooms owned by the given user.
     * Used to retrieve all devices accessible to a user across all their rooms.
     *
     * @param userId the primary key of the owning user
     * @return list of devices owned by the user
     */
    List<Device> findAllByRoomUserId(Long userId);
}
