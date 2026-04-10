package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.RenameDeviceRequest;
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
 * specifying type and name. FR-05: rename and remove devices.
 * FR-06: manual device control with state persistence.</p>
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
                .map(DeviceService::toResponse)
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
        return toResponse(saved);
    }

    /**
     * Renames an existing virtual device in a room.
     * FR-05: Gerät umbenennen.
     *
     * @param email    the email of the authenticated user
     * @param roomId   the room's primary key
     * @param deviceId the device's primary key
     * @param request  the rename request containing the new name
     * @return the updated device
     * @throws ResponseStatusException with status 404 if the room or device is not found
     * @throws ResponseStatusException with status 409 if another device in the room
     *                                 already has the requested name
     */
    @Transactional
    public DeviceResponse renameDevice(String email, Long roomId, Long deviceId, RenameDeviceRequest request) {
        Room room = getOwnedRoom(email, roomId);
        Device device = deviceRepository.findByIdAndRoomId(deviceId, room.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
        if (deviceRepository.existsByRoomIdAndNameAndIdNot(room.getId(), request.getName(), deviceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A device named '" + request.getName() + "' already exists in this room.");
        }
        device.setName(request.getName());
        Device saved = deviceRepository.save(device);
        return toResponse(saved);
    }

    /**
     * Deletes a virtual device from a room.
     * FR-05: Gerät löschen.
     *
     * @param email    the email of the authenticated user
     * @param roomId   the room's primary key
     * @param deviceId the device's primary key
     * @throws ResponseStatusException with status 404 if the room or device is not found
     */
    @Transactional
    public void deleteDevice(String email, Long roomId, Long deviceId) {
        Room room = getOwnedRoom(email, roomId);
        Device device = deviceRepository.findByIdAndRoomId(deviceId, room.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
        deviceRepository.delete(device);
    }

    /**
     * Partially updates the runtime state of a virtual device.
     * FR-06: Gerät manuell steuern.
     *
     * <p>Only non-null fields in the request are applied; all others remain unchanged.</p>
     *
     * @param email    the email of the authenticated user
     * @param roomId   the room's primary key
     * @param deviceId the device's primary key
     * @param request  the state update request
     * @return the updated device with its new state
     * @throws ResponseStatusException with status 404 if the room or device is not found
     */
    @Transactional
    public DeviceResponse updateState(String email, Long roomId, Long deviceId, DeviceStateRequest request) {
        Room room = getOwnedRoom(email, roomId);
        Device device = deviceRepository.findByIdAndRoomId(deviceId, room.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
        if (request.getStateOn() != null) {
            device.setStateOn(request.getStateOn());
        }
        if (request.getBrightness() != null) {
            device.setBrightness(request.getBrightness());
        }
        if (request.getTemperature() != null) {
            device.setTemperature(request.getTemperature());
        }
        if (request.getSensorValue() != null) {
            device.setSensorValue(request.getSensorValue());
        }
        if (request.getCoverPosition() != null) {
            device.setCoverPosition(request.getCoverPosition());
        }
        return toResponse(deviceRepository.save(device));
    }

    private static DeviceResponse toResponse(Device d) {
        return new DeviceResponse(
                d.getId(), d.getName(), d.getType(),
                d.isStateOn(), d.getBrightness(), d.getTemperature(),
                d.getSensorValue(), d.getCoverPosition());
    }

    private Room getOwnedRoom(String email, Long roomId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        return roomRepository.findByIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
    }
}
