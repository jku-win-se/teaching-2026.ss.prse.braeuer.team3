package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.ActivityLog;
import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.repository.ActivityLogRepository;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Service for managing activity log entries in the SmartHome Orchestrator.
 *
 * <p>Handles creation, paginated retrieval with optional filters, and deletion
 * of activity log entries. All operations are scoped to the authenticated user.</p>
 *
 * <p>Implements FR-08: Aktivitätsprotokoll.</p>
 *
 * <p>FR-13: Reading or deleting the activity log is owner-only. Member actions
 * are still recorded through {@link #log(Device, User, String, String)} using
 * the owner for scoping and the member name as actor.</p>
 *
 * <p>FR-16: CSV export via {@link #exportActivityLogCsv(String)}.</p>
 */
@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final MemberService memberService;
    private final CsvExportService csvExportService;

    /**
     * Constructs an ActivityLogService with the required repositories.
     *
     * @param activityLogRepository the repository for activity log persistence
     * @param userRepository        the repository for resolving users
     * @param deviceRepository      the repository for resolving devices
     * @param memberService         the service used for owner-only authorization (FR-13)
     * @param csvExportService      the service used to build CSV output (FR-16)
     */
    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              UserRepository userRepository,
                              DeviceRepository deviceRepository,
                              MemberService memberService,
                              CsvExportService csvExportService) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.memberService = memberService;
        this.csvExportService = csvExportService;
    }

    /**
     * Creates and persists a new activity log entry.
     *
     * @param device    the device that was changed
     * @param user      the owning user (for scoping)
     * @param actorName the display name of the actor who performed the action
     * @param action    the human-readable description of the action
     * @return the persisted log entry as a response DTO
     */
    @Transactional
    public ActivityLogResponse log(Device device, User user, String actorName, String action) {
        ActivityLog entry = new ActivityLog(Instant.now(), device, user, actorName, action);
        ActivityLog saved = activityLogRepository.save(entry);
        return toResponse(saved);
    }

    /**
     * Returns a paginated list of activity log entries for the authenticated user,
     * optionally filtered by date range and/or device.
     *
     * @param email    the email of the authenticated user
     * @param page     zero-based page index
     * @param size     page size (number of entries per page)
     * @param from     optional start of the date range filter (inclusive); {@code null} = no lower bound
     * @param to       optional end of the date range filter (inclusive); {@code null} = no upper bound
     * @param deviceId optional device id filter; {@code null} = all devices
     * @return a page of activity log response DTOs ordered by timestamp descending (newest first)
     * @throws ResponseStatusException with status 404 if the user is not found
     * @throws ResponseStatusException with status 404 if the specified device is not found
     *                                 or does not belong to the user
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getLogs(String email, int page, int size,
                                             Instant from, Instant to, Long deviceId) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        boolean hasDate = from != null || to != null;
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now().plusSeconds(5);

        Page<ActivityLog> resultPage;
        if (deviceId != null) {
            Device device = deviceRepository.findById(deviceId)
                    .filter(d -> Objects.equals(d.getRoom().getUser().getId(), user.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
            if (hasDate) {
                resultPage = activityLogRepository.findByUserAndTimestampBetweenAndDevice(
                        user, effectiveFrom, effectiveTo, device, pageable);
            } else {
                resultPage = activityLogRepository.findByUserAndDevice(user, device, pageable);
            }
        } else if (hasDate) {
            resultPage = activityLogRepository.findByUserAndTimestampBetween(
                    user, effectiveFrom, effectiveTo, pageable);
        } else {
            resultPage = activityLogRepository.findByUser(user, pageable);
        }

        return resultPage.map(ActivityLogService::toResponse);
    }

    /**
     * Deletes a single activity log entry owned by the authenticated user.
     *
     * @param email the email of the authenticated user
     * @param logId the primary key of the log entry to delete
     * @throws ResponseStatusException with status 404 if the log entry is not found
     *                                 or does not belong to the user
     */
    @Transactional
    public void deleteLog(String email, Long logId) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        ActivityLog entry = activityLogRepository.findByIdAndUser(logId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Activity log entry not found."));
        activityLogRepository.delete(entry);
    }

    /**
     * Builds a human-readable action description based on the device type and the
     * fields present in the state change request.
     *
     * <p>Used by {@link DeviceService} when logging a manual state change.
     * Returns a descriptive string for each device type:</p>
     * <ul>
     *   <li>SWITCH: "Turned on" / "Turned off"</li>
     *   <li>DIMMER: "Brightness set to {n}%" / "Turned on" / "Turned off"</li>
     *   <li>THERMOSTAT: "Temperature set to {n}°C" / "Turned on" / "Turned off"</li>
     *   <li>SENSOR: "Sensor value set to {n}"</li>
     *   <li>COVER: "Cover opened" / "Cover closed" / "Cover position set to {n}%"</li>
     * </ul>
     *
     * @param device  the device being changed
     * @param request the state change request containing the new values
     * @return a human-readable description of the action
     */
    public String buildActionDescription(Device device, DeviceStateRequest request) {
        switch (device.getType()) {
            case SWITCH:
                return Boolean.TRUE.equals(request.getStateOn()) ? "Turned on" : "Turned off";
            case DIMMER:
                if (request.getBrightness() != null) {
                    return "Brightness set to " + request.getBrightness() + "%";
                }
                return Boolean.TRUE.equals(request.getStateOn()) ? "Turned on" : "Turned off";
            case THERMOSTAT:
                if (request.getTemperature() != null) {
                    return "Temperature set to " + request.getTemperature() + "°C";
                }
                return Boolean.TRUE.equals(request.getStateOn()) ? "Turned on" : "Turned off";
            case SENSOR:
                if (request.getSensorValue() != null) {
                    return "Sensor value set to " + request.getSensorValue();
                }
                return "Sensor updated";
            case COVER:
                if (request.getCoverPosition() != null) {
                    Integer pos = request.getCoverPosition();
                    if (pos == 0) {
                        return "Cover closed";
                    } else if (pos == 100) {
                        return "Cover opened";
                    }
                    return "Cover position set to " + pos + "%";
                }
                return Boolean.TRUE.equals(request.getStateOn()) ? "Cover opened" : "Cover closed";
            default:
                return "State updated";
        }
    }

    /**
     * Exports the complete activity log for the authenticated user as a CSV string.
     *
     * <p>Owner-only (FR-13): a Member caller receives 403 Forbidden.</p>
     * <p>All entries are included without pagination, ordered by timestamp ascending.</p>
     *
     * @param email the email of the authenticated user
     * @return RFC-4180 CSV content with header row; columns: Timestamp, Device, Room, Actor, Action
     * @throws ResponseStatusException with status 403 if the caller is not the home Owner
     */
    @Transactional(readOnly = true)
    public String exportActivityLogCsv(String email) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        List<ActivityLog> entries = activityLogRepository.findAllByUser(
                user, Sort.by(Sort.Direction.ASC, "timestamp"));
        return csvExportService.buildActivityLogCsv(entries);
    }

    /**
     * Resolves a user by email, throwing 401 if not found.
     *
     * @param email the user's email
     * @return the resolved user entity
     * @throws ResponseStatusException with status 401 if the user is not found
     */
    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    /**
     * Converts an {@link ActivityLog} entity to an {@link ActivityLogResponse} DTO.
     *
     * @param log the entity to convert
     * @return the response DTO
     */
    private static ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getTimestamp(),
                log.getDevice().getId(),
                log.getDevice().getName(),
                log.getDevice().getRoom().getName(),
                log.getActorName(),
                log.getAction()
        );
    }
}
