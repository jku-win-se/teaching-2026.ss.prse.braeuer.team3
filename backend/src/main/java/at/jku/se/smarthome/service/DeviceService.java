package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service for managing virtual devices in rooms.
 *
 * <p>Implements FR-04: add virtual smart devices to a room,
 * specifying type and name.</p>
 *
 * <p>All operations are scoped to the authenticated user — the target room
 * must be owned by that user.</p>
 */
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a DeviceService with the required repositories.
     *
     * @param deviceRepository the repository for device persistence
     * @param roomRepository   the repository for room lookups
     * @param userRepository   the repository for resolving the current user
     */
    public DeviceService(DeviceRepository deviceRepository,
                         RoomRepository roomRepository,
                         UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns all devices in a room owned by the authenticated user.
     *
     * @param email  the email of the authenticated user
     * @param roomId the room's primary key
     * @return list of device responses ordered by creation date
     * @throws ResponseStatusException with status 404 if the room is not found
     *                                 or belongs to another user
     */
    public List<DeviceResponse> getDevices(String email, Long roomId) {
        Room room = getOwnedRoom(email, roomId);
        return deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())
                .stream()
                .map(d -> new DeviceResponse(d.getId(), d.getName(), d.getType()))
                .toList();
    }

    /**
     * Adds a new virtual device to a room.
     * FR-04: Gerät hinzufügen.
     *
     * @param email   the email of the authenticated user
     * @param roomId  the room's primary key
     * @param request the device creation request
     * @return the newly created device
     * @throws ResponseStatusException with status 404 if the room is not found
     *                                 or belongs to another user
     * @throws ResponseStatusException with status 409 if a device with the same
     *                                 name already exists in the room
     */
    @Transactional
    public DeviceResponse addDevice(String email, Long roomId, DeviceRequest request) {
        Room room = getOwnedRoom(email, roomId);
        if (deviceRepository.existsByRoomIdAndName(room.getId(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A device named '" + request.getName() + "' already exists in this room.");
        }
        Device device = new Device(room, request.getName(), request.getType());
        Device saved = deviceRepository.save(device);
        return new DeviceResponse(saved.getId(), saved.getName(), saved.getType());
    }

    private Room getOwnedRoom(String email, Long roomId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        return roomRepository.findByIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
    }
}
