package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.Schedule;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.domain.VacationMode;
import at.jku.se.smarthome.domain.VacationModeAction;
import at.jku.se.smarthome.dto.VacationModeRequest;
import at.jku.se.smarthome.dto.VacationModeResponse;
import at.jku.se.smarthome.repository.ScheduleRepository;
import at.jku.se.smarthome.repository.VacationModeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacationModeServiceTest {

    @Mock private VacationModeRepository vacationModeRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private MemberService memberService;

    private VacationModeService vacationModeService;

    private static final String EMAIL = "owner@test.com";

    private User user;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        vacationModeService = new VacationModeService(
                vacationModeRepository, scheduleRepository, memberService);

        user = new User("Owner", EMAIL, "hashed");
        ReflectionTestUtils.setField(user, "id", 1L);

        Room room = new Room(user, "Living Room", "home");
        ReflectionTestUtils.setField(room, "id", 2L);

        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        ReflectionTestUtils.setField(device, "id", 3L);

        schedule = new Schedule();
        ReflectionTestUtils.setField(schedule, "id", 10L);
        schedule.setName("Evening Routine");
        schedule.setDevice(device);
        schedule.setDaysOfWeek("MONDAY,FRIDAY");
        schedule.setHour(20);
        schedule.setMinute(0);
        schedule.setActionPayload("{\"stateOn\":true}");
        schedule.setEnabled(false);
    }

    // --- getVacationModes ---

    @Test
    void getVacationModes_returnsAllForUser() {
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.ENABLE);
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByUser(user)).thenReturn(List.of(vm));

        List<VacationModeResponse> result = vacationModeService.getVacationModes(EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Summer Holiday");
    }

    @Test
    void getVacationModes_throwsForbidden_whenNotOwner() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Owner role required."))
                .when(memberService).requireOwnerRole(EMAIL);

        assertThatThrownBy(() -> vacationModeService.getVacationModes(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // --- createVacationMode ---

    @Test
    void createVacationMode_persistsAndReturnsResponse() {
        VacationModeRequest req = buildRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7),
                VacationModeAction.ENABLE);
        VacationMode saved = buildVacationMode(req.getStartDate(), req.getEndDate(), VacationModeAction.ENABLE);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(vacationModeRepository.save(any(VacationMode.class))).thenReturn(saved);

        VacationModeResponse response = vacationModeService.createVacationMode(EMAIL, req);

        assertThat(response.getName()).isEqualTo("Summer Holiday");
        verify(vacationModeRepository).save(any(VacationMode.class));
    }

    @Test
    void createVacationMode_enablesSchedule_immediately_whenWindowActiveAndEnableAction() {
        LocalDate today = LocalDate.now();
        VacationModeRequest req = buildRequest(today, today.plusDays(7), VacationModeAction.ENABLE);
        VacationMode saved = buildVacationMode(today, today.plusDays(7), VacationModeAction.ENABLE);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(vacationModeRepository.save(any(VacationMode.class))).thenReturn(saved);

        vacationModeService.createVacationMode(EMAIL, req);

        assertThat(schedule.isEnabled()).isTrue();
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void createVacationMode_disablesSchedule_immediately_whenWindowActiveAndDisableAction() {
        LocalDate today = LocalDate.now();
        schedule.setEnabled(true);
        VacationModeRequest req = buildRequest(today, today.plusDays(7), VacationModeAction.DISABLE);
        VacationMode saved = buildVacationMode(today, today.plusDays(7), VacationModeAction.DISABLE);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(vacationModeRepository.save(any(VacationMode.class))).thenReturn(saved);

        vacationModeService.createVacationMode(EMAIL, req);

        assertThat(schedule.isEnabled()).isFalse();
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void createVacationMode_doesNotTouchSchedule_whenStartDateInFuture() {
        VacationModeRequest req = buildRequest(LocalDate.now().plusDays(3), LocalDate.now().plusDays(10),
                VacationModeAction.ENABLE);
        VacationMode saved = buildVacationMode(req.getStartDate(), req.getEndDate(), VacationModeAction.ENABLE);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(vacationModeRepository.save(any(VacationMode.class))).thenReturn(saved);

        vacationModeService.createVacationMode(EMAIL, req);

        assertThat(schedule.isEnabled()).isFalse();
        verify(scheduleRepository, never()).save(schedule);
    }

    @Test
    void createVacationMode_throwsBadRequest_whenNameBlank() {
        VacationModeRequest req = buildRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7),
                VacationModeAction.ENABLE);
        req.setName("  ");
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);

        assertThatThrownBy(() -> vacationModeService.createVacationMode(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createVacationMode_throwsBadRequest_whenActionNull() {
        VacationModeRequest req = buildRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7), null);
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);

        assertThatThrownBy(() -> vacationModeService.createVacationMode(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createVacationMode_throwsBadRequest_whenEndDateBeforeStartDate() {
        VacationModeRequest req = buildRequest(LocalDate.now().plusDays(5), LocalDate.now().plusDays(1),
                VacationModeAction.ENABLE);
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);

        assertThatThrownBy(() -> vacationModeService.createVacationMode(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createVacationMode_throwsNotFound_whenScheduleNotOwned() {
        VacationModeRequest req = buildRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7),
                VacationModeAction.ENABLE);
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacationModeService.createVacationMode(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- deactivateVacationMode ---

    @Test
    void deactivateVacationMode_restoresOriginalEnabledState() {
        schedule.setEnabled(true);
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.ENABLE);
        vm.setOriginalEnabled(false); // schedule was originally disabled before vacation mode enabled it

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(vm));
        when(vacationModeRepository.save(any())).thenReturn(vm);

        VacationModeResponse response = vacationModeService.deactivateVacationMode(EMAIL, 1L);

        assertThat(vm.isDeactivated()).isTrue();
        assertThat(schedule.isEnabled()).isFalse(); // restored to originalEnabled=false
        verify(scheduleRepository).save(schedule);
        assertThat(response.isDeactivated()).isTrue();
    }

    @Test
    void deactivateVacationMode_doesNotTouchSchedule_whenNotYetApplied() {
        VacationMode vm = buildVacationMode(LocalDate.now().plusDays(3), LocalDate.now().plusDays(10),
                VacationModeAction.ENABLE);
        // originalEnabled is null — window hasn't started yet

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(vm));
        when(vacationModeRepository.save(any())).thenReturn(vm);

        vacationModeService.deactivateVacationMode(EMAIL, 1L);

        assertThat(vm.isDeactivated()).isTrue();
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void deactivateVacationMode_isIdempotent_whenAlreadyDeactivated() {
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.ENABLE);
        vm.setDeactivated(true);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(vm));

        vacationModeService.deactivateVacationMode(EMAIL, 1L);

        verify(scheduleRepository, never()).save(any());
        verify(vacationModeRepository, never()).save(any());
    }

    @Test
    void deactivateVacationMode_throwsNotFound_whenNotOwned() {
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacationModeService.deactivateVacationMode(EMAIL, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- deleteVacationMode ---

    @Test
    void deleteVacationMode_removesRecord_withoutRestoringSchedule_whenNotYetApplied() {
        VacationMode vm = buildVacationMode(LocalDate.now().plusDays(3), LocalDate.now().plusDays(10),
                VacationModeAction.ENABLE);
        // originalEnabled is null — window hasn't started

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(vm));

        vacationModeService.deleteVacationMode(EMAIL, 1L);

        verify(scheduleRepository, never()).save(any());
        verify(vacationModeRepository).delete(vm);
    }

    @Test
    void deleteVacationMode_restoresSchedule_whenActiveAtDeletion() {
        schedule.setEnabled(true);
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.ENABLE);
        vm.setOriginalEnabled(false); // was enabled by vacation mode; original was false

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(vm));

        vacationModeService.deleteVacationMode(EMAIL, 1L);

        assertThat(schedule.isEnabled()).isFalse(); // restored
        verify(scheduleRepository).save(schedule);
        verify(vacationModeRepository).delete(vm);
    }

    @Test
    void deleteVacationMode_throwsNotFound_whenNotOwned() {
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
        when(vacationModeRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacationModeService.deleteVacationMode(EMAIL, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- checkAndUpdateStates ---

    @Test
    void checkAndUpdateStates_enablesSchedule_whenWindowJustStarted_withEnableAction() {
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.ENABLE);
        // originalEnabled is null — not yet applied
        when(vacationModeRepository.findByDeactivatedFalse()).thenReturn(List.of(vm));

        vacationModeService.checkAndUpdateStates();

        assertThat(schedule.isEnabled()).isTrue();
        assertThat(vm.getOriginalEnabled()).isFalse(); // was false before activation
        verify(scheduleRepository).save(schedule);
        verify(vacationModeRepository).save(vm);
    }

    @Test
    void checkAndUpdateStates_disablesSchedule_whenWindowJustStarted_withDisableAction() {
        schedule.setEnabled(true);
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.DISABLE);
        // originalEnabled is null — not yet applied
        when(vacationModeRepository.findByDeactivatedFalse()).thenReturn(List.of(vm));

        vacationModeService.checkAndUpdateStates();

        assertThat(schedule.isEnabled()).isFalse();
        assertThat(vm.getOriginalEnabled()).isTrue(); // was true before deactivation
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void checkAndUpdateStates_autoExpires_andRestoresSchedule_whenEndDatePassed() {
        schedule.setEnabled(true); // currently enabled by vacation mode
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(5), LocalDate.now().minusDays(1),
                VacationModeAction.ENABLE);
        vm.setOriginalEnabled(false); // was disabled before vacation mode enabled it

        when(vacationModeRepository.findByDeactivatedFalse()).thenReturn(List.of(vm));

        vacationModeService.checkAndUpdateStates();

        assertThat(schedule.isEnabled()).isFalse(); // restored to originalEnabled=false
        assertThat(vm.isDeactivated()).isTrue();
        verify(scheduleRepository).save(schedule);
        verify(vacationModeRepository).save(vm);
    }

    @Test
    void checkAndUpdateStates_doesNothing_whenFutureVacation() {
        VacationMode vm = buildVacationMode(LocalDate.now().plusDays(3), LocalDate.now().plusDays(10),
                VacationModeAction.ENABLE);
        when(vacationModeRepository.findByDeactivatedFalse()).thenReturn(List.of(vm));

        vacationModeService.checkAndUpdateStates();

        verify(scheduleRepository, never()).save(any());
        verify(vacationModeRepository, never()).save(any());
    }

    @Test
    void checkAndUpdateStates_doesNotReapply_whenAlreadyApplied() {
        schedule.setEnabled(true); // already enabled by the rule
        VacationMode vm = buildVacationMode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VacationModeAction.ENABLE);
        vm.setOriginalEnabled(false); // already applied

        when(vacationModeRepository.findByDeactivatedFalse()).thenReturn(List.of(vm));

        vacationModeService.checkAndUpdateStates();

        // Window is active and already applied — no changes expected
        verify(scheduleRepository, never()).save(any());
        verify(vacationModeRepository, never()).save(any());
    }

    // --- helpers ---

    private VacationMode buildVacationMode(LocalDate start, LocalDate end, VacationModeAction action) {
        VacationMode vm = new VacationMode();
        ReflectionTestUtils.setField(vm, "id", 1L);
        vm.setUser(user);
        vm.setSchedule(schedule);
        vm.setName("Summer Holiday");
        vm.setAction(action);
        vm.setStartDate(start);
        vm.setEndDate(end);
        return vm;
    }

    private VacationModeRequest buildRequest(LocalDate start, LocalDate end, VacationModeAction action) {
        VacationModeRequest req = new VacationModeRequest();
        req.setName("Summer Holiday");
        req.setScheduleId(10L);
        req.setAction(action);
        req.setStartDate(start);
        req.setEndDate(end);
        return req;
    }
}
