package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import at.jku.se.smarthome.service.EnergyService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 *
 * <p>FR-16: CSV export via {@code GET /api/energy/export}.</p>
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

    /**
     * Exports the energy usage summary as a CSV file download (FR-16).
     *
     * <p>Accessible to all authenticated users (Owner and Member), consistent
     * with the energy dashboard visibility.</p>
     *
     * @param principal authenticated user
     * @return 200 OK with {@code Content-Type: text/csv} and
     *         {@code Content-Disposition: attachment; filename="energy-summary.csv"}
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal UserDetails principal) {
        String csv = energyService.exportEnergyCsv(principal.getUsername());
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"energy-summary.csv\"")
                .body(bytes);
    }
}
