package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.RenameDeviceRequest;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.websocket.DeviceWebSocketHandler;
import org.springframework.context.annotation.Lazy;
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
 * FR-06: manual device control with state persistence.
 * FR-07: broadcasts state changes to connected WebSocket clients after each update.
 * FR-08: logs every manual state change via {@link ActivityLogService}.
 * FR-10: triggers IF-THEN rule evaluation after every user-initiated state update.</p>
 *
 * <p>FR-13: Members may read devices and update device state in their owner's
 * home. Device management operations remain owner-only.</p>
 */
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final DeviceWebSocketHandler webSocketHandler;
    private final ActivityLogService activityLogService;
    private final RuleService ruleService;
    private final MemberService memberService;

    /**
     * Constructs a DeviceService with the required repositories, WebSocket handler,
     * activity log service, and rule service.
     *
     * @param deviceRepository   the repository for device persistence
     * @param roomRepository     the repository for room lookups
     * @param userRepository     the repository for resolving the current user
     * @param webSocketHandler   the handler used to push real-time state updates to WebSocket clients
     * @param activityLogService the service used to record activity log entries (FR-08)
     * @param ruleService        the service used to evaluate IF-THEN rules after state changes (FR-10)
     * @param memberService      the service used for role checks and owner resolution (FR-13)
     */
    public DeviceService(DeviceRepository deviceRepository,
                         RoomRepository roomRepository,
                         UserRepository userRepository,
                         DeviceWebSocketHandler webSocketHandler,
                         ActivityLogService activityLogService,
                         @Lazy RuleService ruleService,
                         MemberService memberService) {
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.webSocketHandler = webSocketHandler;
        this.activityLogService = activityLogService;
        this.ruleService = ruleService;
        this.memberService = memberService;
    }

    /**
     * Returns all devices in a room visible to the authenticated user.
     *
     * <p>FR-13: Members read devices from their owner's home.</p>
     *
     * @param email  the email of the authenticated user
     * @param roomId the room's primary key
     * @return list of device responses ordered by creation date
     * @throws ResponseStatusException with status 404 if the room is not found
     *                                 or belongs to another user
     */
    public List<DeviceResponse> getDevices(String email, Long roomId) {
        Room room = getEffectiveRoom(email, roomId);
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
        memberService.requireOwnerRole(email, "add devices");
        Room room = getEffectiveRoom(email, roomId);
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
        memberService.requireOwnerRole(email, "rename devices");
        Room room = getEffectiveRoom(email, roomId);
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
        memberService.requireOwnerRole(email, "remove devices");
        Room room = getEffectiveRoom(email, roomId);
        Device device = deviceRepository.findByIdAndRoomId(deviceId, room.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
        deviceRepository.delete(device);
    }

    /**
     * Partially updates the runtime state of a virtual device, logs the change in the
     * activity log (FR-08), and broadcasts the update to all connected WebSocket clients
     * of the user (FR-07).
     *
     * <p>Only non-null fields in the request are applied; all others remain unchanged.
     * After persisting the new state, {@link DeviceWebSocketHandler#broadcast} is called so
     * all connected clients receive the device update without a manual reload.
     * Additionally, a new activity log entry is created and broadcast via
     * {@link DeviceWebSocketHandler#broadcastActivityLog}.</p>
     *
     * <p>FR-13: Members may control devices in the owner's home. Activity log
     * entries are scoped to the owner, while the actor name records the caller.</p>
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
        Room room = getEffectiveRoom(email, roomId);
        User effectiveOwner = room.getUser();
        User caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        Device device = deviceRepository.findByIdAndRoomId(deviceId, room.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
        boolean stateOnChanged = request.getStateOn() != null && request.getStateOn() != device.isStateOn();
        applyStateFields(device, request);
        DeviceResponse response = toResponse(deviceRepository.save(device));
        webSocketHandler.broadcast(effectiveOwner.getEmail(), response);

        String action = activityLogService.buildActionDescription(device, request);
        ActivityLogResponse logEntry = activityLogService.log(device, effectiveOwner, caller.getName(), action);
        webSocketHandler.broadcastActivityLog(effectiveOwner.getEmail(), logEntry);

        ruleService.evaluateRulesForDevice(device, request, stateOnChanged);
        return response;
    }

    /**
     * Partially updates the runtime state of a device using a caller-supplied actor name.
     *
     * <p>Intended for internal callers (e.g. {@link ScheduleService}) that need to apply
     * a device state change without an authenticated HTTP request. Skips ownership lookup —
     * the caller is responsible for providing a valid device ID and owner. Broadcasts the
     * updated state via WebSocket and records an activity log entry with the given actor name.</p>
     *
     * @param deviceId  the primary key of the device to update
     * @param request   the state fields to apply (null fields are ignored)
     * @param owner     the user who owns the device (used for WebSocket routing and activity log)
     * @param actorName the display name to record as the actor in the activity log
     * @return the updated device response DTO
     * @throws ResponseStatusException with status 404 if the device is not found
     */
    @Transactional
    public DeviceResponse updateStateAsActor(Long deviceId, DeviceStateRequest request,
                                             User owner, String actorName) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
        applyStateFields(device, request);
        DeviceResponse response = toResponse(deviceRepository.save(device));
        webSocketHandler.broadcast(owner.getEmail(), response);
        String action = activityLogService.buildActionDescription(device, request);
        ActivityLogResponse logEntry = activityLogService.log(device, owner, actorName, action);
        webSocketHandler.broadcastActivityLog(owner.getEmail(), logEntry);
        return response;
    }

    private void applyStateFields(Device device, DeviceStateRequest request) {
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
    }

    /**
     * Converts a {@link Device} entity to a {@link DeviceResponse}, applying type-aware
     * null filtering so that state fields irrelevant to the device type are returned as
     * {@code null} instead of a misleading default value.
     *
     * @param d the device entity to convert
     * @return a response DTO with only the applicable state fields populated
     */
    private static DeviceResponse toResponse(Device d) {
        Boolean stateOn = null;
        Integer brightness = null;
        Double temperature = null;
        Double sensorValue = null;
        Integer coverPosition = null;

        switch (d.getType()) {
            case SWITCH:
                stateOn = d.isStateOn();
                break;
            case DIMMER:
                stateOn = d.isStateOn();
                brightness = d.getBrightness();
                break;
            case THERMOSTAT:
                stateOn = d.isStateOn();
                temperature = d.getTemperature();
                break;
            case SENSOR:
                temperature = d.getTemperature();
                sensorValue = d.getSensorValue();
                break;
            case COVER:
                stateOn = d.isStateOn();
                coverPosition = d.getCoverPosition();
                break;
            default:
                break;
        }

        return new DeviceResponse(
                d.getId(), d.getName(), d.getType(),
                stateOn, brightness, temperature, sensorValue, coverPosition);
    }

    private Room getEffectiveRoom(String email, Long roomId) {
        User effectiveOwner = memberService.resolveEffectiveOwner(email);
        return roomRepository.findByIdAndUserId(roomId, effectiveOwner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
    }
}
