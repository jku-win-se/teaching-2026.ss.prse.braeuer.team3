package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.MqttConfigRequest;
import at.jku.se.smarthome.dto.MqttConfigResponse;
import at.jku.se.smarthome.dto.MqttMessageDto;
import at.jku.se.smarthome.service.MqttSimulatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the simulated MQTT integration (US-019).
 *
 * <p>Endpoints allow the owner to configure, connect, and disconnect a simulated
 * MQTT broker, and to retrieve the rolling in-memory message log. All endpoints
 * require a valid JWT Bearer token.</p>
 *
 * <p>No real MQTT broker is ever contacted. The simulation is entirely in-memory
 * via {@link MqttSimulatorService}.</p>
 */
@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    private final MqttSimulatorService mqttSimulatorService;

    /**
     * Constructs the controller with the required service.
     *
     * @param mqttSimulatorService the service handling simulated MQTT operations
     */
    public MqttController(MqttSimulatorService mqttSimulatorService) {
        this.mqttSimulatorService = mqttSimulatorService;
    }

    /**
     * Returns the current MQTT configuration and connection status for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return 200 OK with {@link MqttConfigResponse}
     */
    @GetMapping("/config")
    public ResponseEntity<MqttConfigResponse> getConfig(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mqttSimulatorService.getConfig(userDetails.getUsername()));
    }

    /**
     * Saves (creates or updates) the MQTT broker configuration for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @param request     the broker URL and topic to persist
     * @return 200 OK with the updated {@link MqttConfigResponse}
     */
    @PutMapping("/config")
    public ResponseEntity<MqttConfigResponse> saveConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MqttConfigRequest request) {
        return ResponseEntity.ok(
                mqttSimulatorService.saveConfig(userDetails.getUsername(), request));
    }

    /**
     * Simulates establishing the MQTT connection for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return 200 OK with updated {@link MqttConfigResponse} (connected = true)
     */
    @PostMapping("/connect")
    public ResponseEntity<MqttConfigResponse> connect(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mqttSimulatorService.connect(userDetails.getUsername()));
    }

    /**
     * Simulates disconnecting the MQTT connection for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return 200 OK with updated {@link MqttConfigResponse} (connected = false)
     */
    @PostMapping("/disconnect")
    public ResponseEntity<MqttConfigResponse> disconnect(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mqttSimulatorService.disconnect(userDetails.getUsername()));
    }

    /**
     * Returns the in-memory simulated MQTT message log for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return 200 OK with the ordered list of {@link MqttMessageDto} (oldest first)
     */
    @GetMapping("/messages")
    public ResponseEntity<List<MqttMessageDto>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mqttSimulatorService.getMessages(userDetails.getUsername()));
    }

    /**
     * Clears the in-memory simulated MQTT message log for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return 204 No Content
     */
    @DeleteMapping("/messages")
    public ResponseEntity<Void> clearMessages(
            @AuthenticationPrincipal UserDetails userDetails) {
        mqttSimulatorService.clearMessages(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
