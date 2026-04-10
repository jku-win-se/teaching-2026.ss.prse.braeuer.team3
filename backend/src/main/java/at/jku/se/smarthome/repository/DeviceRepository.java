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
     * Checks whether a device with the given name already exists in the room.
     *
     * @param roomId the room's primary key
     * @param name   the device name to check
     * @return {@code true} if a device with that name already exists in the room
     */
    boolean existsByRoomIdAndName(Long roomId, String name);
}
