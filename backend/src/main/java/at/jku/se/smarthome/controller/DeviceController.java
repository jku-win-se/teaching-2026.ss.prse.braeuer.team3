package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.RenameDeviceRequest;
import at.jku.se.smarthome.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing devices within rooms.
 *
 * <p>All endpoints require a valid JWT Bearer token.
 * Devices are always scoped to a room owned by the authenticated user.</p>
 *
 * <p>Implements FR-04: add virtual smart devices to a room.
 * FR-05: rename and remove devices.
 * FR-06: manual device control with state persistence.</p>
 */
@RestController
@RequestMapping("/api/rooms/{roomId}/devices")
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Constructs a DeviceController with the required service.
     *
     * @param deviceService the service handling device operations
     */
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * Returns all devices in the specified room.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param roomId    the room's primary key
     * @return 200 OK with the list of devices
     */
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long roomId) {
        return ResponseEntity.ok(deviceService.getDevices(principal.getUsername(), roomId));
    }

    /**
     * Adds a new virtual device to the specified room.
     * FR-04: Gerät hinzufügen.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param roomId    the room's primary key
     * @param request   the device creation request
     * @return 201 Created with the new device data
     */
    @PostMapping
    public ResponseEntity<DeviceResponse> addDevice(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long roomId,
            @Valid @RequestBody DeviceRequest request) {
        DeviceResponse response = deviceService.addDevice(principal.getUsername(), roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Renames an existing virtual device.
     * FR-05: Gerät umbenennen.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param roomId    the room's primary key
     * @param deviceId  the device's primary key
     * @param request   the rename request containing the new name
     * @return 200 OK with the updated device data
     */
    @PutMapping("/{deviceId}")
    public ResponseEntity<DeviceResponse> renameDevice(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long roomId,
            @PathVariable Long deviceId,
            @Valid @RequestBody RenameDeviceRequest request) {
        DeviceResponse response = deviceService.renameDevice(principal.getUsername(), roomId, deviceId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a virtual device from a room.
     * FR-05: Gerät löschen.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param roomId    the room's primary key
     * @param deviceId  the device's primary key
     * @return 204 No Content on success
     */
    /**
     * Partially updates the runtime state of a virtual device.
     * FR-06: Gerät manuell steuern.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param roomId    the room's primary key
     * @param deviceId  the device's primary key
     * @param request   the state update request (all fields optional)
     * @return 200 OK with the updated device including its new state
     */
    @PatchMapping("/{deviceId}/state")
    public ResponseEntity<DeviceResponse> updateState(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long roomId,
            @PathVariable Long deviceId,
            @RequestBody DeviceStateRequest request) {
        DeviceResponse response = deviceService.updateState(principal.getUsername(), roomId, deviceId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteDevice(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long roomId,
            @PathVariable Long deviceId) {
        deviceService.deleteDevice(principal.getUsername(), roomId, deviceId);
        return ResponseEntity.noContent().build();
    }
}
