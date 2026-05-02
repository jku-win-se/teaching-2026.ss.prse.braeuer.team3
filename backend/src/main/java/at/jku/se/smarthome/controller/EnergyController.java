package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import at.jku.se.smarthome.service.EnergyService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for energy consumption estimates.
 *
 * <p>US-016: Exposes device-level consumption data. The frontend aggregates the
 * returned list into room and household totals for the dashboard.</p>
 */
@RestController
@RequestMapping("/api/energy")
public class EnergyController {

    private final EnergyService energyService;

    /**
     * Constructs an EnergyController with the required service.
     *
     * @param energyService the service that calculates energy estimates
     */
    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    /**
     * Returns estimated energy consumption for all devices in the household.
     *
     * @param principal authenticated user
     * @return device-level energy responses
     */
    @GetMapping("/devices")
    public ResponseEntity<List<EnergyDeviceResponse>> getDeviceEnergy(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(energyService.getDeviceEnergy(principal.getUsername()));
    }
}
