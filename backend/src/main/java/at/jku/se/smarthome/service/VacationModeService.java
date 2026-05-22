package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Schedule;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.domain.VacationMode;
import at.jku.se.smarthome.domain.VacationModeAction;
import at.jku.se.smarthome.dto.VacationModeRequest;
import at.jku.se.smarthome.dto.VacationModeResponse;
import at.jku.se.smarthome.repository.ScheduleRepository;
import at.jku.se.smarthome.repository.VacationModeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing vacation mode rules in the SmartHome Orchestrator.
 *
 * <p>Each vacation mode rule targets a specific {@link Schedule} and defines an
 * {@link VacationModeAction action} (ENABLE or DISABLE) to apply during a date window.
 * When the window starts, the schedule's original {@code enabled} state is captured
 * and the action is applied. When the window expires or the rule is manually deactivated,
 * the original state is restored.</p>
 *
 * <p>The daily state check is triggered by {@link VacationModeScheduler} every midnight.
 * Immediate state changes also happen on create (if window is already active),
 * deactivate, and delete.</p>
 *
 * <p>Multiple rules per user are allowed (no singleton constraint).</p>
 *
 * <p>Implements FR-21: Urlaubsmodus.</p>
 */
@Service
public class VacationModeService {

    private static final Logger log = LoggerFactory.getLogger(VacationModeService.class);

    private final VacationModeRepository vacationModeRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberService memberService;

    /**
     * Constructs a {@code VacationModeService} with all required dependencies.
     *
     * @param vacationModeRepository the repository for vacation mode persistence
     * @param scheduleRepository     the repository used to toggle schedule enabled state
     * @param memberService          the service used for owner-only authorization
     */
    public VacationModeService(VacationModeRepository vacationModeRepository,
                               ScheduleRepository scheduleRepository,
                               MemberService memberService) {
        this.vacationModeRepository = vacationModeRepository;
        this.scheduleRepository = scheduleRepository;
        this.memberService = memberService;
    }

    /**
     * Returns all vacation mode rules for the authenticated owner.
     *
     * @param userEmail the email of the authenticated user
     * @return list of vacation mode response DTOs (past, current, and upcoming)
     * @throws ResponseStatusException with status 403 if the user is not an owner
     */
    @Transactional(readOnly = true)
    public List<VacationModeResponse> getVacationModes(String userEmail) {
        memberService.requireOwnerRole(userEmail);
        User user = memberService.resolveEffectiveOwner(userEmail);
        return vacationModeRepository.findByUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates a new vacation mode rule and persists it.
     *
     * <p>If today falls within {@code [startDate, endDate]}, the action is applied
     * immediately and {@code originalEnabled} is captured.</p>
     *
     * @param userEmail the email of the authenticated user
     * @param request   the vacation mode creation request
     * @return the created vacation mode as a response DTO
     * @throws ResponseStatusException with status 403 if the user is not an owner
     * @throws ResponseStatusException with status 400 if validation fails
     * @throws ResponseStatusException with status 404 if the schedule is not found or not owned
     */
    @Transactional
    public VacationModeResponse createVacationMode(String userEmail, VacationModeRequest request) {
        memberService.requireOwnerRole(userEmail);
        User user = memberService.resolveEffectiveOwner(userEmail);

        validateRequest(request);

        Schedule schedule = resolveOwnedSchedule(user, request.getScheduleId());

        VacationMode vm = new VacationMode();
        vm.setUser(user);
        vm.setSchedule(schedule);
        vm.setName(request.getName().trim());
        vm.setAction(request.getAction());
        vm.setStartDate(request.getStartDate());
        vm.setEndDate(request.getEndDate());

        LocalDate today = LocalDate.now();
        if (!today.isBefore(request.getStartDate()) && !today.isAfter(request.getEndDate())) {
            vm.setOriginalEnabled(schedule.isEnabled());
            applyAction(vm);
            scheduleRepository.save(schedule);
            if (log.isInfoEnabled()) {
                log.info("Vacation mode creation: {} action applied to schedule {} immediately",
                        request.getAction(), schedule.getId());
            }
        }

        vm = vacationModeRepository.save(vm);
        if (log.isInfoEnabled()) {
            log.info("Vacation mode {} created for user {}", vm.getId(), user.getEmail());
        }
        return toResponse(vm);
    }

    /**
     * Permanently deactivates a vacation mode rule and restores the schedule to its original state.
     *
     * <p>If the rule has not yet been applied (window not started), no schedule change occurs.
     * Deactivation is idempotent — calling this on an already-deactivated rule returns the
     * current state without error.</p>
     *
     * @param userEmail      the email of the authenticated user
     * @param vacationModeId the primary key of the vacation mode to deactivate
     * @return the updated vacation mode as a response DTO
     * @throws ResponseStatusException with status 403 if the user is not an owner
     * @throws ResponseStatusException with status 404 if the vacation mode is not found or not owned
     */
    @Transactional
    public VacationModeResponse deactivateVacationMode(String userEmail, Long vacationModeId) {
        memberService.requireOwnerRole(userEmail);
        User user = memberService.resolveEffectiveOwner(userEmail);
        VacationMode vm = resolveOwnedVacationMode(user, vacationModeId);

        if (!vm.isDeactivated()) {
            if (vm.getOriginalEnabled() != null) {
                vm.getSchedule().setEnabled(vm.getOriginalEnabled());
                scheduleRepository.save(vm.getSchedule());
                if (log.isInfoEnabled()) {
                    log.info("Vacation mode {} deactivated; schedule {} restored to enabled={}",
                            vm.getId(), vm.getSchedule().getId(), vm.getOriginalEnabled());
                }
            }
            vm.setDeactivated(true);
            vacationModeRepository.save(vm);
        }
        return toResponse(vm);
    }

    /**
     * Deletes a vacation mode rule.
     *
     * <p>If the rule was active at deletion time (action already applied), the schedule
     * is restored to its original state before the record is removed.</p>
     *
     * @param userEmail      the email of the authenticated user
     * @param vacationModeId the primary key of the vacation mode to delete
     * @throws ResponseStatusException with status 403 if the user is not an owner
     * @throws ResponseStatusException with status 404 if the vacation mode is not found or not owned
     */
    @Transactional
    public void deleteVacationMode(String userEmail, Long vacationModeId) {
        memberService.requireOwnerRole(userEmail);
        User user = memberService.resolveEffectiveOwner(userEmail);
        VacationMode vm = resolveOwnedVacationMode(user, vacationModeId);

        if (!vm.isDeactivated() && vm.getOriginalEnabled() != null) {
            vm.getSchedule().setEnabled(vm.getOriginalEnabled());
            scheduleRepository.save(vm.getSchedule());
            if (log.isInfoEnabled()) {
                log.info("Vacation mode {} deleted while active; schedule {} restored to enabled={}",
                        vm.getId(), vm.getSchedule().getId(), vm.getOriginalEnabled());
            }
        }

        vacationModeRepository.delete(vm);
        if (log.isInfoEnabled()) {
            log.info("Vacation mode {} deleted", vacationModeId);
        }
    }

    /**
     * Checks all non-deactivated vacation mode rules and updates each schedule's
     * {@code enabled} flag based on whether today falls within their date window.
     *
     * <p>Called daily at midnight by {@link VacationModeScheduler}.
     * Rules whose {@code endDate} has passed are auto-expired (deactivated) and the
     * schedule is restored. Rules whose window just started have their action applied
     * and {@code originalEnabled} captured.</p>
     */
    @Transactional
    public void checkAndUpdateStates() {
        LocalDate today = LocalDate.now();
        List<VacationMode> candidates = vacationModeRepository.findByDeactivatedFalse();
        for (VacationMode vm : candidates) {
            if (today.isAfter(vm.getEndDate())) {
                if (vm.getOriginalEnabled() != null) {
                    vm.getSchedule().setEnabled(vm.getOriginalEnabled());
                    scheduleRepository.save(vm.getSchedule());
                }
                vm.setDeactivated(true);
                vacationModeRepository.save(vm);
                if (log.isInfoEnabled()) {
                    log.info("Vacation mode {} auto-expired; schedule {} restored",
                            vm.getId(), vm.getSchedule().getId());
                }
            } else if (!today.isBefore(vm.getStartDate()) && vm.getOriginalEnabled() == null) {
                vm.setOriginalEnabled(vm.getSchedule().isEnabled());
                applyAction(vm);
                scheduleRepository.save(vm.getSchedule());
                vacationModeRepository.save(vm);
                if (log.isInfoEnabled()) {
                    log.info("Vacation mode {} activated; {} action applied to schedule {}",
                            vm.getId(), vm.getAction(), vm.getSchedule().getId());
                }
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void applyAction(VacationMode vm) {
        vm.getSchedule().setEnabled(vm.getAction() == VacationModeAction.ENABLE);
    }

    private Schedule resolveOwnedSchedule(User user, Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .filter(s -> Objects.equals(s.getDevice().getRoom().getUser().getId(), user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found."));
    }

    private VacationMode resolveOwnedVacationMode(User user, Long vacationModeId) {
        return vacationModeRepository.findByIdAndUser(vacationModeId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation mode not found."));
    }

    private void validateRequest(VacationModeRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vacation mode name must not be blank.");
        }
        if (request.getName().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vacation mode name must not exceed 100 characters.");
        }
        if (request.getAction() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must not be null.");
        }
        if (request.getScheduleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduleId must not be null.");
        }
        if (request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be null.");
        }
        if (request.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be null.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate.");
        }
    }

    private VacationModeResponse toResponse(VacationMode vm) {
        boolean active = vm.getOriginalEnabled() != null && !vm.isDeactivated();
        return new VacationModeResponse(
                vm.getId(),
                vm.getName(),
                vm.getSchedule().getId(),
                vm.getSchedule().getName(),
                vm.getStartDate(),
                vm.getEndDate(),
                vm.isDeactivated(),
                active,
                vm.getAction()
        );
    }
}
