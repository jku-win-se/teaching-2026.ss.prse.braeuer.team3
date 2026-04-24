package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST controller for the activity log.
 *
 * <p>All endpoints require a valid JWT Bearer token. Log entries are always
 * scoped to the authenticated user.</p>
 *
 * <p>Implements FR-08: Aktivitätsprotokoll.</p>
 *
 * <pre>
 * GET    /api/activity-log?page=0&amp;size=20&amp;from={ISO}&amp;to={ISO}&amp;deviceId={id}
 * DELETE /api/activity-log/{id}
 * </pre>
 */
@RestController
@RequestMapping("/api/activity-log")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /**
     * Constructs an ActivityLogController with the required service.
     *
     * @param activityLogService the service handling activity log operations
     */
    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    /**
     * Returns a paginated, optionally filtered page of activity log entries
     * for the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param page      zero-based page index (default: 0)
     * @param size      number of entries per page (default: 20)
     * @param from      optional ISO-8601 timestamp — only entries at or after this time are included
     * @param to        optional ISO-8601 timestamp — only entries at or before this time are included
     * @param deviceId  optional device id — when provided, only entries for that device are returned
     * @return 200 OK with a Spring Data {@link Page} of {@link ActivityLogResponse} DTOs
     */
    @GetMapping
    public ResponseEntity<Page<ActivityLogResponse>> getLogs(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long deviceId) {
        Page<ActivityLogResponse> result = activityLogService.getLogs(
                principal.getUsername(), page, size, from, to, deviceId);
        return ResponseEntity.ok(result);
    }

    /**
     * Deletes a single activity log entry owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the log entry to delete
     * @return 204 No Content on success, or 404 if the entry is not found or not owned by the user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        activityLogService.deleteLog(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
