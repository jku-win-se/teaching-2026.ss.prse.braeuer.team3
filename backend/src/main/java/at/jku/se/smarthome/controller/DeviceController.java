package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
 * <p>Implements FR-04: add virtual smart devices to a room.</p>
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
}
