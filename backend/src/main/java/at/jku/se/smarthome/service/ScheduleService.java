package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.Schedule;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.ScheduleRequest;
import at.jku.se.smarthome.dto.ScheduleResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.ScheduleRepository;
import at.jku.se.smarthome.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for managing device schedules in the SmartHome Orchestrator.
 *
 * <p>Handles CRUD operations for {@link Schedule} entities and fires due schedules
 * once per minute via a {@link Scheduled} polling method. Execution failures are
 * caught and recorded in the activity log rather than propagated to the caller.</p>
 *
 * <p>Implements FR-09: Zeitpläne konfigurieren.</p>
 *
 * <p>FR-13: Schedule management endpoints are owner-only. Scheduled execution
 * itself remains internal and continues to run for owner-owned devices.</p>
 */
@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final ActivityLogService activityLogService;
    private final ObjectMapper objectMapper;
    private final MemberService memberService;

    /**
     * Constructs a {@code ScheduleService} with all required dependencies.
     *
     * @param scheduleRepository  the repository for schedule persistence
     * @param deviceRepository    the repository for device lookups
     * @param userRepository      the repository for user lookups
     * @param deviceService       the service used to apply device state on execution
     * @param activityLogService  the service used to log execution results
     * @param objectMapper        the Jackson mapper for action payload deserialization
     * @param memberService       the service used for owner-only authorization (FR-13)
     */
    public ScheduleService(ScheduleRepository scheduleRepository,
                           DeviceRepository deviceRepository,
                           UserRepository userRepository,
                           DeviceService deviceService,
                           ActivityLogService activityLogService,
                           ObjectMapper objectMapper,
                           MemberService memberService) {
        this.scheduleRepository = scheduleRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.deviceService = deviceService;
        this.activityLogService = activityLogService;
        this.objectMapper = objectMapper;
        this.memberService = memberService;
    }

    /**
     * Fires all enabled schedules that are due at the current minute.
     *
     * <p>Runs automatically at the start of every minute (cron {@code "0 * * * * *"}).
     * Queries for enabled schedules matching the current hour and minute, then checks
     * whether today's day of week is included. Matching schedules are executed
     * immediately; failures are caught and logged per schedule.</p>
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void runDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        String today = now.getDayOfWeek().name();
        List<Schedule> candidates =
                scheduleRepository.findByEnabledTrueAndHourAndMinute(now.getHour(), now.getMinute());
        for (Schedule schedule : candidates) {
            List<String> days = Arrays.asList(schedule.getDaysOfWeek().split(","));
            if (days.contains(today)) {
                executeSchedule(schedule.getId());
            }
        }
    }

    /**
     * Returns all schedules visible to the authenticated user, optionally filtered by device.
     *
     * @param userEmail the email of the authenticated user
     * @param deviceId  optional device ID filter; {@code null} returns schedules for all owned devices
     * @return list of schedule response DTOs
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 404 if the specified device is not found
     *                                 or not owned by the user
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedules(String userEmail, Long deviceId) {
        memberService.requireOwnerRole(userEmail);
        User user = resolveUser(userEmail);
        List<Schedule> schedules;
        if (deviceId != null) {
            Device device = resolveOwnedDevice(user, deviceId);
            schedules = scheduleRepository.findByDevice(device);
        } else {
            List<Device> devices = deviceRepository.findAllByRoomUserId(user.getId());
            schedules = scheduleRepository.findByDeviceIn(devices);
        }
        return schedules.stream().map(this::toResponse).toList();
    }

    /**
     * Creates a new schedule and persists it.
     *
     * @param userEmail the email of the authenticated user
     * @param request   the schedule creation request
     * @return the created schedule as a response DTO
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 404 if the device is not found or not owned
     * @throws ResponseStatusException with status 400 if validation fails
     */
    @Transactional
    public ScheduleResponse createSchedule(String userEmail, ScheduleRequest request) {
        memberService.requireOwnerRole(userEmail);
        User user = resolveUser(userEmail);
        Device device = resolveOwnedDevice(user, request.getDeviceId());
        validateRequest(request);

        Schedule schedule = new Schedule();
        schedule.setName(request.getName().trim());
        schedule.setDevice(device);
        schedule.setDaysOfWeek(joinDays(request.getDaysOfWeek()));
        schedule.setHour(request.getHour());
        schedule.setMinute(request.getMinute());
        schedule.setActionPayload(request.getActionPayload());
        schedule.setEnabled(request.isEnabled());

        schedule = scheduleRepository.save(schedule);
        if (log.isInfoEnabled()) {
            log.info("Schedule {} created for device {}", schedule.getId(), device.getId());
        }
        return toResponse(schedule);
    }

    /**
     * Updates an existing schedule.
     *
     * @param userEmail  the email of the authenticated user
     * @param scheduleId the primary key of the schedule to update
     * @param request    the update request
     * @return the updated schedule as a response DTO
     * @throws ResponseStatusException with status 404 if the schedule is not found or not owned
     * @throws ResponseStatusException with status 400 if validation fails
     */
    @Transactional
    public ScheduleResponse updateSchedule(String userEmail, Long scheduleId, ScheduleRequest request) {
        memberService.requireOwnerRole(userEmail);
        User user = resolveUser(userEmail);
        Schedule schedule = resolveOwnedSchedule(user, scheduleId);
        validateRequest(request);

        if (!schedule.getDevice().getId().equals(request.getDeviceId())) {
            Device newDevice = resolveOwnedDevice(user, request.getDeviceId());
            schedule.setDevice(newDevice);
        }

        schedule.setName(request.getName().trim());
        schedule.setDaysOfWeek(joinDays(request.getDaysOfWeek()));
        schedule.setHour(request.getHour());
        schedule.setMinute(request.getMinute());
        schedule.setActionPayload(request.getActionPayload());
        schedule.setEnabled(request.isEnabled());

        return toResponse(scheduleRepository.save(schedule));
    }

    /**
     * Toggles the enabled flag of a schedule.
     *
     * <p>When disabled, the polling method skips the schedule automatically.
     * No external scheduler state needs to be updated.</p>
     *
     * @param userEmail  the email of the authenticated user
     * @param scheduleId the primary key of the schedule
     * @param enabled    the new enabled state
     * @return the updated schedule as a response DTO
     * @throws ResponseStatusException with status 404 if the schedule is not found or not owned
     */
    @Transactional
    public ScheduleResponse setEnabled(String userEmail, Long scheduleId, boolean enabled) {
        memberService.requireOwnerRole(userEmail);
        User user = resolveUser(userEmail);
        Schedule schedule = resolveOwnedSchedule(user, scheduleId);
        schedule.setEnabled(enabled);
        return toResponse(scheduleRepository.save(schedule));
    }

    /**
     * Deletes a schedule.
     *
     * @param userEmail  the email of the authenticated user
     * @param scheduleId the primary key of the schedule to delete
     * @throws ResponseStatusException with status 404 if the schedule is not found or not owned
     */
    @Transactional
    public void deleteSchedule(String userEmail, Long scheduleId) {
        memberService.requireOwnerRole(userEmail);
        User user = resolveUser(userEmail);
        Schedule schedule = resolveOwnedSchedule(user, scheduleId);
        scheduleRepository.delete(schedule);
        if (log.isInfoEnabled()) {
            log.info("Schedule {} deleted", scheduleId);
        }
    }

    /**
     * Executes a schedule by applying its stored action payload to the target device.
     * Called by {@link #runDueSchedules()} when a schedule is due to fire.
     *
     * <p>If the schedule is not found (e.g. deleted between query and execution),
     * a warning is logged and the method returns silently. Any execution failure is
     * caught and recorded in the activity log.</p>
     *
     * @param scheduleId the primary key of the schedule to execute
     */
    public void executeSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            if (log.isWarnEnabled()) {
                log.warn("Schedule {} not found during execution - skipping", scheduleId);
            }
            return;
        }

        Device device = schedule.getDevice();
        User owner = device.getRoom().getUser();
        String actorName = "Scheduler (" + schedule.getName() + ")";

        try {
            DeviceStateRequest stateRequest = objectMapper.readValue(
                    schedule.getActionPayload(), DeviceStateRequest.class);
            deviceService.updateStateAsActor(device.getId(), stateRequest, owner, actorName);
            if (log.isInfoEnabled()) {
                log.info("Schedule {} executed for device {}", scheduleId, device.getId());
            }
        } catch (JsonProcessingException e) {
            if (log.isWarnEnabled()) {
                log.warn("Schedule {} has invalid action payload: {}", scheduleId, e.getMessage());
            }
            activityLogService.log(device, owner, actorName, "Execution failed: invalid action payload");
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Schedule {} execution failed: {}", scheduleId, e.getMessage());
            }
            activityLogService.log(device, owner, actorName, "Execution failed: " + e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private Device resolveOwnedDevice(User user, Long deviceId) {
        return deviceRepository.findById(deviceId)
                .filter(d -> Objects.equals(d.getRoom().getUser().getId(), user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
    }

    private Schedule resolveOwnedSchedule(User user, Long scheduleId) {
        return scheduleRepository.findByIdAndDeviceRoomUser(scheduleId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found."));
    }

    private void validateRequest(ScheduleRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule name must not be blank.");
        }
        if (request.getName().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule name must not exceed 100 characters.");
        }
        if (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one day of week is required.");
        }
        if (request.getHour() < 0 || request.getHour() > 23) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hour must be between 0 and 23.");
        }
        if (request.getMinute() < 0 || request.getMinute() > 59) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minute must be between 0 and 59.");
        }
        if (request.getActionPayload() == null || request.getActionPayload().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action payload must not be blank.");
        }
        try {
            objectMapper.readValue(request.getActionPayload(), DeviceStateRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action payload is not valid JSON.");
        }
    }

    private String joinDays(List<String> days) {
        return days.stream().map(String::toUpperCase).collect(Collectors.joining(","));
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        List<String> days = Arrays.asList(schedule.getDaysOfWeek().split(","));
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getName(),
                schedule.getDevice().getId(),
                schedule.getDevice().getName(),
                schedule.getDevice().getRoom().getName(),
                days,
                schedule.getHour(),
                schedule.getMinute(),
                schedule.getActionPayload(),
                schedule.isEnabled()
        );
    }
}
