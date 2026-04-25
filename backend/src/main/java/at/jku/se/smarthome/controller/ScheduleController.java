package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.ScheduleRequest;
import at.jku.se.smarthome.dto.ScheduleResponse;
import at.jku.se.smarthome.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for device schedule management.
 *
 * <p>All endpoints require a valid JWT Bearer token. Schedules are always
 * scoped to devices owned by the authenticated user.</p>
 *
 * <p>Implements FR-09: Zeitpläne konfigurieren.</p>
 *
 * <pre>
 * GET    /api/schedules[?deviceId={id}]
 * POST   /api/schedules
 * PUT    /api/schedules/{id}
 * PATCH  /api/schedules/{id}/enabled
 * DELETE /api/schedules/{id}
 * </pre>
 */
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Constructs a {@code ScheduleController} with the required service.
     *
     * @param scheduleService the service handling schedule business logic
     */
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Returns all schedules for the authenticated user, optionally filtered by device.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param deviceId  optional device ID filter; omit to get schedules for all devices
     * @return 200 OK with a list of {@link ScheduleResponse} DTOs
     */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long deviceId) {
        List<ScheduleResponse> result = scheduleService.getSchedules(principal.getUsername(), deviceId);
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a new schedule for a device owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param request   the schedule creation request
     * @return 201 Created with the new {@link ScheduleResponse}
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing schedule owned by the authenticated user.
     *
     * @param principal  the authenticated user injected by Spring Security
     * @param id         the primary key of the schedule to update
     * @param request    the update request
     * @return 200 OK with the updated {@link ScheduleResponse}, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.updateSchedule(principal.getUsername(), id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles the enabled flag of a schedule.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the schedule
     * @param body      a JSON object with a single {@code "enabled"} boolean field
     * @return 200 OK with the updated {@link ScheduleResponse}, or 404 if not found
     */
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<ScheduleResponse> setEnabled(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        ScheduleResponse response = scheduleService.setEnabled(principal.getUsername(), id, enabled);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a schedule owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the schedule to delete
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        scheduleService.deleteSchedule(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
