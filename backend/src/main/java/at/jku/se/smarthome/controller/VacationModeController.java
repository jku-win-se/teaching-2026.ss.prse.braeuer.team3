package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.VacationModeRequest;
import at.jku.se.smarthome.dto.VacationModeResponse;
import at.jku.se.smarthome.service.VacationModeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for vacation mode management.
 *
 * <p>All endpoints require a valid JWT Bearer token and are restricted to the
 * home owner. Only one vacation mode may exist per user at a time.</p>
 *
 * <p>Implements FR-21: Urlaubsmodus.</p>
 *
 * <pre>
 * GET    /api/vacation-modes
 * POST   /api/vacation-modes
 * PATCH  /api/vacation-modes/{id}/deactivate
 * DELETE /api/vacation-modes/{id}
 * </pre>
 */
@RestController
@RequestMapping("/api/vacation-modes")
public class VacationModeController {

    private final VacationModeService vacationModeService;

    /**
     * Constructs a {@code VacationModeController} with the required service.
     *
     * @param vacationModeService the service handling vacation mode business logic
     */
    public VacationModeController(VacationModeService vacationModeService) {
        this.vacationModeService = vacationModeService;
    }

    /**
     * Returns all vacation modes for the authenticated owner.
     *
     * @param principal the authenticated user injected by Spring Security
     * @return 200 OK with a list of {@link VacationModeResponse} DTOs
     */
    @GetMapping
    public ResponseEntity<List<VacationModeResponse>> getVacationModes(
            @AuthenticationPrincipal UserDetails principal) {
        List<VacationModeResponse> result = vacationModeService.getVacationModes(principal.getUsername());
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a new vacation mode for the authenticated owner.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param request   the vacation mode creation request
     * @return 201 Created with the new {@link VacationModeResponse}
     */
    @PostMapping
    public ResponseEntity<VacationModeResponse> createVacationMode(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody VacationModeRequest request) {
        VacationModeResponse response = vacationModeService.createVacationMode(
                principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Permanently deactivates a vacation mode owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the vacation mode to deactivate
     * @return 200 OK with the updated {@link VacationModeResponse}, or 404 if not found
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<VacationModeResponse> deactivateVacationMode(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        VacationModeResponse response = vacationModeService.deactivateVacationMode(
                principal.getUsername(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a vacation mode owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the vacation mode to delete
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVacationMode(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        vacationModeService.deleteVacationMode(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
