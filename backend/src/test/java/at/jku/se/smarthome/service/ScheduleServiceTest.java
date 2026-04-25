package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.Schedule;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.ScheduleRequest;
import at.jku.se.smarthome.dto.ScheduleResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.ScheduleRepository;
import at.jku.se.smarthome.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeviceService deviceService;
    @Mock private ActivityLogService activityLogService;

    private ScheduleService scheduleService;
    private ObjectMapper objectMapper;

    private User user;
    private Room room;
    private Device device;

    private static final String VALID_PAYLOAD = "{\"stateOn\":true}";
    private static final String EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        scheduleService = new ScheduleService(
                scheduleRepository, deviceRepository, userRepository,
                deviceService, activityLogService, objectMapper);

        user = new User("Test User", EMAIL, "hashed");
        ReflectionTestUtils.setField(user, "id", 1L);
        room = new Room(user, "Living Room", "weekend");
        ReflectionTestUtils.setField(room, "id", 2L);
        device = new Device(room, "Lamp", DeviceType.SWITCH);
        ReflectionTestUtils.setField(device, "id", 10L);
    }

    // --- getSchedules ---

    @Test
    void getSchedules_noFilter_returnsAllForUser() {
        Schedule s = buildSchedule();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findAllByRoomUserId(user.getId())).thenReturn(List.of(device));
        when(scheduleRepository.findByDeviceIn(List.of(device))).thenReturn(List.of(s));

        List<ScheduleResponse> result = scheduleService.getSchedules(EMAIL, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Morning");
    }

    @Test
    void getSchedules_withDeviceFilter_returnsOnlyForDevice() {
        Schedule s = buildSchedule();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(scheduleRepository.findByDevice(device)).thenReturn(List.of(s));

        List<ScheduleResponse> result = scheduleService.getSchedules(EMAIL, device.getId());

        assertThat(result).hasSize(1);
    }

    // --- createSchedule ---

    @Test
    void createSchedule_persistsSchedule() {
        ScheduleRequest req = buildRequest();
        Schedule saved = buildSchedule();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(saved);

        ScheduleResponse response = scheduleService.createSchedule(EMAIL, req);

        assertThat(response.getName()).isEqualTo("Morning");
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void createSchedule_throwsBadRequest_whenNameBlank() {
        ScheduleRequest req = buildRequest();
        req.setName("  ");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> scheduleService.createSchedule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createSchedule_throwsBadRequest_whenNoDaysSelected() {
        ScheduleRequest req = buildRequest();
        req.setDaysOfWeek(List.of());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> scheduleService.createSchedule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createSchedule_throwsNotFound_whenDeviceNotOwned() {
        ScheduleRequest req = buildRequest();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.createSchedule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- setEnabled ---

    @Test
    void setEnabled_false_disablesSchedule() {
        Schedule s = buildSchedule();
        s.setEnabled(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(scheduleRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(scheduleRepository.save(any())).thenReturn(s);

        ScheduleResponse result = scheduleService.setEnabled(EMAIL, s.getId(), false);

        verify(scheduleRepository).save(s);
        assertThat(s.isEnabled()).isFalse();
    }

    @Test
    void setEnabled_true_enablesSchedule() {
        Schedule s = buildSchedule();
        s.setEnabled(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(scheduleRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(scheduleRepository.save(any())).thenReturn(s);

        scheduleService.setEnabled(EMAIL, s.getId(), true);

        verify(scheduleRepository).save(s);
        assertThat(s.isEnabled()).isTrue();
    }

    // --- deleteSchedule ---

    @Test
    void deleteSchedule_removesEntity() {
        Schedule s = buildSchedule();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(scheduleRepository.findById(s.getId())).thenReturn(Optional.of(s));

        scheduleService.deleteSchedule(EMAIL, s.getId());

        verify(scheduleRepository).delete(s);
    }

    @Test
    void deleteSchedule_throwsNotFound_whenNotOwned() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.deleteSchedule(EMAIL, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- runDueSchedules ---

    @Test
    void runDueSchedules_executesWhenDayMatches() throws Exception {
        Schedule s = buildSchedule();
        // Include all 7 days so the schedule always matches regardless of test run day
        s.setDaysOfWeek("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        when(scheduleRepository.findByEnabledTrueAndHourAndMinute(anyInt(), anyInt())).thenReturn(List.of(s));
        when(scheduleRepository.findById(s.getId())).thenReturn(Optional.of(s));

        scheduleService.runDueSchedules();

        verify(deviceService).updateStateAsActor(any(), any(), any(), any());
    }

    @Test
    void runDueSchedules_skipsWhenDayNotMatch() {
        Schedule s = buildSchedule();
        // Set daysOfWeek to all days except today — schedule will never match
        String today = LocalDateTime.now().getDayOfWeek().name();
        String otherDays = Stream.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
                .filter(d -> !d.equals(today))
                .collect(Collectors.joining(","));
        s.setDaysOfWeek(otherDays);
        when(scheduleRepository.findByEnabledTrueAndHourAndMinute(anyInt(), anyInt())).thenReturn(List.of(s));

        scheduleService.runDueSchedules();

        verify(deviceService, never()).updateStateAsActor(any(), any(), any(), any());
    }

    @Test
    void runDueSchedules_doesNothingWhenNoSchedulesDue() {
        when(scheduleRepository.findByEnabledTrueAndHourAndMinute(anyInt(), anyInt())).thenReturn(List.of());

        scheduleService.runDueSchedules();

        verify(deviceService, never()).updateStateAsActor(any(), any(), any(), any());
    }

    // --- executeSchedule ---

    @Test
    void executeSchedule_appliesStateAndLogs() throws Exception {
        Schedule s = buildSchedule();
        when(scheduleRepository.findById(s.getId())).thenReturn(Optional.of(s));

        scheduleService.executeSchedule(s.getId());

        verify(deviceService).updateStateAsActor(any(), any(), any(), any());
    }

    @Test
    void executeSchedule_skipsWhenOrphan() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        scheduleService.executeSchedule(99L);

        verify(deviceService, never()).updateStateAsActor(any(), any(), any(), any());
    }

    @Test
    void executeSchedule_logsFailureOnException() throws Exception {
        Schedule s = buildSchedule();
        when(scheduleRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(deviceService.updateStateAsActor(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        scheduleService.executeSchedule(s.getId());

        verify(activityLogService).log(any(), any(), any(), any());
    }

    // --- helpers ---

    private Schedule buildSchedule() {
        Schedule s = new Schedule();
        ReflectionTestUtils.setField(s, "id", 1L);
        s.setName("Morning");
        s.setDevice(device);
        s.setDaysOfWeek("MONDAY,FRIDAY");
        s.setHour(7);
        s.setMinute(30);
        s.setActionPayload(VALID_PAYLOAD);
        s.setEnabled(true);
        return s;
    }

    private ScheduleRequest buildRequest() {
        ScheduleRequest req = new ScheduleRequest();
        req.setName("Morning");
        req.setDeviceId(device.getId());
        req.setDaysOfWeek(List.of("MONDAY", "FRIDAY"));
        req.setHour(7);
        req.setMinute(30);
        req.setActionPayload(VALID_PAYLOAD);
        req.setEnabled(true);
        return req;
    }
}
