package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.SimulationRequest;
import at.jku.se.smarthome.dto.SimulationResponse;
import at.jku.se.smarthome.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the day-simulation feature (US-020).
 *
 * <p>Exposes a single endpoint that runs a 24-hour simulation in-memory and returns
 * the ordered list of device state changes. The live system is never modified.</p>
 *
 * <p>Owner-only: enforced inside {@link SimulationService#run}.</p>
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * Constructs a {@code SimulationController} with the required service.
     *
     * @param simulationService the service that executes the simulation engine
     */
    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * Runs a full 24-hour day simulation for the authenticated owner's home.
     *
     * <p>The simulation evaluates all enabled automation rules for the specified
     * day of the week against the provided (or current live) device start states,
     * without touching the database or broadcasting WebSocket events.</p>
     *
     * @param userDetails the authenticated user (injected from JWT)
     * @param request     the simulation parameters (day of week + start conditions)
     * @return 200 OK with the {@link SimulationResponse} containing the event timeline
     */
    @PostMapping("/run")
    public ResponseEntity<SimulationResponse> runSimulation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SimulationRequest request) {
        SimulationResponse response = simulationService.run(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
