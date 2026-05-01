package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.ActivityLog;
import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.repository.ActivityLogRepository;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private MemberService memberService;

    private ActivityLogService activityLogService;

    private User user;
    private Room room;
    private Device device;

    @BeforeEach
    void setUp() {
        activityLogService = new ActivityLogService(activityLogRepository, userRepository, deviceRepository,
                memberService);
        user = new User("Test User", "user@test.com", "hashed");
        room = new Room(user, "Living Room", "weekend");
        device = new Device(room, "Lamp", DeviceType.SWITCH);
    }

    // --- log ---

    @Test
    void log_createsEntry() {
        ActivityLog saved = new ActivityLog(Instant.now(), device, user, "Test User", "Turned on");
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(saved);

        ActivityLogResponse response = activityLogService.log(device, user, "Test User", "Turned on");

        assertThat(response.getActorName()).isEqualTo("Test User");
        assertThat(response.getAction()).isEqualTo("Turned on");
        assertThat(response.getDeviceName()).isEqualTo("Lamp");
        assertThat(response.getRoomName()).isEqualTo("Living Room");
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    // --- getLogs ---

    @Test
    void getLogs_noFilter_returnsAll() {
        ActivityLog entry = new ActivityLog(Instant.now(), device, user, "Test User", "Turned on");
        Page<ActivityLog> page = new PageImpl<>(List.of(entry));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(activityLogRepository.findByUser(eq(user), any(Pageable.class))).thenReturn(page);

        Page<ActivityLogResponse> result = activityLogService.getLogs("user@test.com", 0, 20, null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("Turned on");
    }

    @Test
    void getLogs_withDateRange_filtersCorrectly() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        ActivityLog entry = new ActivityLog(Instant.now().minusSeconds(1800), device, user, "Test User", "Turned on");
        Page<ActivityLog> page = new PageImpl<>(List.of(entry));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(activityLogRepository.findByUserAndTimestampBetween(eq(user), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        Page<ActivityLogResponse> result = activityLogService.getLogs("user@test.com", 0, 20, from, to, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(activityLogRepository).findByUserAndTimestampBetween(eq(user), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    @Test
    void getLogs_withDeviceId_filtersCorrectly() {
        ActivityLog entry = new ActivityLog(Instant.now(), device, user, "Test User", "Turned on");
        Page<ActivityLog> page = new PageImpl<>(List.of(entry));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(activityLogRepository.findByUserAndDevice(eq(user), eq(device), any(Pageable.class)))
                .thenReturn(page);

        Page<ActivityLogResponse> result = activityLogService.getLogs("user@test.com", 0, 20, null, null, 10L);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(activityLogRepository).findByUserAndDevice(eq(user), eq(device), any(Pageable.class));
    }

    @Test
    void getLogs_withDeviceIdAndDateRange_filtersCorrectly() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        ActivityLog entry = new ActivityLog(Instant.now().minusSeconds(1800), device, user, "Test User", "Turned on");
        Page<ActivityLog> page = new PageImpl<>(List.of(entry));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(activityLogRepository.findByUserAndTimestampBetweenAndDevice(
                eq(user), any(Instant.class), any(Instant.class), eq(device), any(Pageable.class)))
                .thenReturn(page);

        Page<ActivityLogResponse> result = activityLogService.getLogs("user@test.com", 0, 20, from, to, 10L);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(activityLogRepository).findByUserAndTimestampBetweenAndDevice(
                eq(user), any(Instant.class), any(Instant.class), eq(device), any(Pageable.class));
    }

    @Test
    void getLogs_memberCaller_throwsForbidden() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Owner role required."))
                .when(memberService).requireOwnerRole("user@test.com");

        assertThatThrownBy(() -> activityLogService.getLogs("user@test.com", 0, 20, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // --- deleteLog ---

    @Test
    void deleteLog_removesEntry() {
        ActivityLog entry = new ActivityLog(Instant.now(), device, user, "Test User", "Turned on");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(activityLogRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(entry));

        activityLogService.deleteLog("user@test.com", 1L);

        verify(activityLogRepository).delete(entry);
    }

    @Test
    void deleteLog_throwsNotFound_whenEntryNotOwned() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(activityLogRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityLogService.deleteLog("user@test.com", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- buildActionDescription ---

    @Test
    void buildActionDescription_switch_turnedOn() {
        DeviceStateRequest req = new DeviceStateRequest();
        req.setStateOn(true);
        assertThat(activityLogService.buildActionDescription(device, req)).isEqualTo("Turned on");
    }

    @Test
    void buildActionDescription_switch_turnedOff() {
        DeviceStateRequest req = new DeviceStateRequest();
        req.setStateOn(false);
        assertThat(activityLogService.buildActionDescription(device, req)).isEqualTo("Turned off");
    }

    @Test
    void buildActionDescription_dimmer_brightness() {
        Device dimmer = new Device(room, "Dimmer", DeviceType.DIMMER);
        DeviceStateRequest req = new DeviceStateRequest();
        req.setBrightness(80);
        assertThat(activityLogService.buildActionDescription(dimmer, req)).isEqualTo("Brightness set to 80%");
    }

    @Test
    void buildActionDescription_thermostat_temperature() {
        Device thermostat = new Device(room, "Thermostat", DeviceType.THERMOSTAT);
        DeviceStateRequest req = new DeviceStateRequest();
        req.setTemperature(22.5);
        assertThat(activityLogService.buildActionDescription(thermostat, req)).isEqualTo("Temperature set to 22.5°C");
    }

    @Test
    void buildActionDescription_cover_closed() {
        Device cover = new Device(room, "Cover", DeviceType.COVER);
        DeviceStateRequest req = new DeviceStateRequest();
        req.setCoverPosition(0);
        assertThat(activityLogService.buildActionDescription(cover, req)).isEqualTo("Cover closed");
    }

    @Test
    void buildActionDescription_cover_opened() {
        Device cover = new Device(room, "Cover", DeviceType.COVER);
        DeviceStateRequest req = new DeviceStateRequest();
        req.setCoverPosition(100);
        assertThat(activityLogService.buildActionDescription(cover, req)).isEqualTo("Cover opened");
    }

    @Test
    void buildActionDescription_sensor_value() {
        Device sensor = new Device(room, "Sensor", DeviceType.SENSOR);
        DeviceStateRequest req = new DeviceStateRequest();
        req.setSensorValue(23.1);
        assertThat(activityLogService.buildActionDescription(sensor, req)).isEqualTo("Sensor value set to 23.1");
    }
}
