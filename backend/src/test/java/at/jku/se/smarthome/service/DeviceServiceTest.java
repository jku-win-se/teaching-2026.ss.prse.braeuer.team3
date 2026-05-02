package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.RenameDeviceRequest;
import at.jku.se.smarthome.websocket.DeviceWebSocketHandler;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

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
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private MemberService memberService;

    private DeviceService deviceService;

    private User user;
    private Room room;

    @BeforeEach
    void setUp() {
        user = new User("Test User", "user@test.com", "hashed");
        room = new Room(user, "Living Room", "weekend");
        // Use a no-op DeviceWebSocketHandler subclass — avoids Mockito inline-mock
        // limitations on JVM versions that restrict byte-buddy instrumentation.
        DeviceWebSocketHandler noOpWs = new DeviceWebSocketHandler(new com.fasterxml.jackson.databind.ObjectMapper()) {
            @Override
            public void broadcast(String userEmail, DeviceResponse deviceResponse) {
                // no-op: WebSocket device broadcast is tested separately in DeviceWebSocketHandlerTest
            }

            @Override
            public void broadcastActivityLog(String userEmail, ActivityLogResponse activityLogResponse) {
                // no-op: WebSocket activity log broadcast is tested separately
            }
        };
        RuleService noOpRuleService = new RuleService(null, null, null, null, null, null) {
            @Override
            public void evaluateRulesForDevice(at.jku.se.smarthome.domain.Device device,
                                               at.jku.se.smarthome.dto.DeviceStateRequest request,
                                               boolean stateOnChanged) {
                // no-op: rule evaluation is tested separately in RuleServiceTest
            }
        };
        deviceService = new DeviceService(deviceRepository, roomRepository, userRepository, noOpWs, activityLogService,
                noOpRuleService, memberService);
    }

    // --- getDevices ---

    @Test
    void getDevices_returnsListForOwnedRoom() {
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        List<DeviceResponse> result = deviceService.getDevices("user@test.com", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Lamp");
        assertThat(result.get(0).getType()).isEqualTo(DeviceType.SWITCH);
    }

    @Test
    void getDevices_throwsNotFound_whenRoomBelongsToOtherUser() {
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDevices("user@test.com", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- addDevice ---

    @Test
    void addDevice_createsAndReturnsDevice() {
        DeviceRequest request = buildRequest("Lamp", DeviceType.SWITCH);
        Device saved = new Device(room, "Lamp", DeviceType.SWITCH);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.existsByRoomIdAndName(room.getId(), "Lamp")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenReturn(saved);

        DeviceResponse response = deviceService.addDevice("user@test.com", 1L, request);

        assertThat(response.getName()).isEqualTo("Lamp");
        assertThat(response.getType()).isEqualTo(DeviceType.SWITCH);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void addDevice_throwsConflict_whenNameAlreadyExists() {
        DeviceRequest request = buildRequest("Lamp", DeviceType.SWITCH);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.existsByRoomIdAndName(room.getId(), "Lamp")).thenReturn(true);

        assertThatThrownBy(() -> deviceService.addDevice("user@test.com", 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void addDevice_throwsNotFound_whenRoomNotOwned() {
        DeviceRequest request = buildRequest("Lamp", DeviceType.SWITCH);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.addDevice("user@test.com", 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void addDevice_memberCaller_throwsForbidden() {
        DeviceRequest request = buildRequest("Lamp", DeviceType.SWITCH);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the home owner can add devices."))
                .when(memberService).requireOwnerRole("user@test.com", "add devices");

        assertThatThrownBy(() -> deviceService.addDevice("user@test.com", 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // --- renameDevice ---

    @Test
    void renameDevice_updatesAndReturns() {
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        RenameDeviceRequest request = buildRenameRequest("Smart Lamp");

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.existsByRoomIdAndNameAndIdNot(room.getId(), "Smart Lamp", 10L)).thenReturn(false);
        when(deviceRepository.save(device)).thenReturn(device);

        DeviceResponse response = deviceService.renameDevice("user@test.com", 1L, 10L, request);

        assertThat(response.getName()).isEqualTo("Smart Lamp");
        verify(deviceRepository).save(device);
    }

    @Test
    void renameDevice_coOwner_updatesDeviceInSharedOwnerHome() {
        User owner = new User("Owner", "owner@gmail.com", "hashed");
        ReflectionTestUtils.setField(owner, "id", 42L);
        Room sharedRoom = new Room(owner, "Garden", "yard");
        ReflectionTestUtils.setField(sharedRoom, "id", 7L);
        Device device = new Device(sharedRoom, "Garten Switch", DeviceType.SWITCH);
        RenameDeviceRequest request = buildRenameRequest("Garden Switch");

        when(memberService.resolveEffectiveOwner("testowner@gmail.com")).thenReturn(owner);
        when(roomRepository.findByIdAndUserId(1L, owner.getId())).thenReturn(Optional.of(sharedRoom));
        when(deviceRepository.findByIdAndRoomId(10L, sharedRoom.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.existsByRoomIdAndNameAndIdNot(sharedRoom.getId(), "Garden Switch", 10L))
                .thenReturn(false);
        when(deviceRepository.save(device)).thenReturn(device);

        DeviceResponse response = deviceService.renameDevice("testowner@gmail.com", 1L, 10L, request);

        assertThat(response.getName()).isEqualTo("Garden Switch");
        verify(roomRepository).findByIdAndUserId(1L, 42L);
    }

    @Test
    void renameDevice_throwsConflict_whenNameTaken() {
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        RenameDeviceRequest request = buildRenameRequest("Thermostat");

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.existsByRoomIdAndNameAndIdNot(room.getId(), "Thermostat", 10L)).thenReturn(true);

        assertThatThrownBy(() -> deviceService.renameDevice("user@test.com", 1L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void renameDevice_throwsNotFound_whenDeviceNotInRoom() {
        RenameDeviceRequest request = buildRenameRequest("Smart Lamp");

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.renameDevice("user@test.com", 1L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- deleteDevice ---

    @Test
    void deleteDevice_removesDevice() {
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.of(device));

        deviceService.deleteDevice("user@test.com", 1L, 10L);

        verify(deviceRepository).delete(device);
    }

    @Test
    void deleteDevice_throwsNotFound_whenDeviceNotInRoom() {
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.deleteDevice("user@test.com", 1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private DeviceRequest buildRequest(String name, DeviceType type) {
        DeviceRequest req = new DeviceRequest();
        req.setName(name);
        req.setType(type);
        return req;
    }

    // --- updateState ---

    @Test
    void updateState_updatesStateOn() {
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        DeviceStateRequest request = new DeviceStateRequest();
        request.setStateOn(true);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        when(activityLogService.buildActionDescription(any(), any())).thenReturn("Turned on");
        when(activityLogService.log(any(), any(), any(), any())).thenReturn(
                new ActivityLogResponse(1L, Instant.now(), null, "Lamp", "Living Room", "Test User", "Turned on"));

        DeviceResponse response = deviceService.updateState("user@test.com", 1L, 10L, request);

        assertThat(response.isStateOn()).isTrue();
        verify(deviceRepository).save(device);
    }

    @Test
    void updateState_updatesBrightness() {
        Device device = new Device(room, "Dimmer", DeviceType.DIMMER);
        DeviceStateRequest request = new DeviceStateRequest();
        request.setBrightness(75);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        when(activityLogService.buildActionDescription(any(), any())).thenReturn("Brightness set to 75%");
        when(activityLogService.log(any(), any(), any(), any())).thenReturn(
                new ActivityLogResponse(1L, Instant.now(), null, "Dimmer", "Living Room", "Test User", "Brightness set to 75%"));

        DeviceResponse response = deviceService.updateState("user@test.com", 1L, 10L, request);

        assertThat(response.getBrightness()).isEqualTo(75);
    }

    @Test
    void updateState_memberCaller_logsAgainstOwnerWithMemberActor() {
        User memberUser = new User("Member", "member@test.com", "hashed");
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        DeviceStateRequest request = new DeviceStateRequest();
        request.setStateOn(true);

        when(memberService.resolveEffectiveOwner("member@test.com")).thenReturn(user);
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(memberUser));
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        when(activityLogService.buildActionDescription(any(), any())).thenReturn("Turned on");
        when(activityLogService.log(any(), any(), any(), any())).thenReturn(
                new ActivityLogResponse(1L, Instant.now(), null, "Lamp", "Living Room", "Member", "Turned on"));

        deviceService.updateState("member@test.com", 1L, 10L, request);

        verify(activityLogService).log(eq(device), eq(user), eq("Member"), eq("Turned on"));
    }

    @Test
    void updateState_throwsNotFound_whenDeviceNotInRoom() {
        DeviceStateRequest request = new DeviceStateRequest();
        request.setStateOn(true);

        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByIdAndRoomId(10L, room.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.updateState("user@test.com", 1L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private RenameDeviceRequest buildRenameRequest(String name) {
        RenameDeviceRequest req = new RenameDeviceRequest();
        req.setName(name);
        return req;
    }

    // --- Bugfix #62: type-aware null filtering in toResponse() ---

    @Test
    void toResponse_switch_onlyStateOnIsNonNull() {
        Device device = new Device(room, "Switch", DeviceType.SWITCH);
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        DeviceResponse response = deviceService.getDevices("user@test.com", 1L).get(0);

        assertThat(response.isStateOn()).isNotNull();
        assertThat(response.getBrightness()).isNull();
        assertThat(response.getTemperature()).isNull();
        assertThat(response.getSensorValue()).isNull();
        assertThat(response.getCoverPosition()).isNull();
    }

    @Test
    void toResponse_dimmer_onlyStateOnAndBrightnessAreNonNull() {
        Device device = new Device(room, "Dimmer", DeviceType.DIMMER);
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        DeviceResponse response = deviceService.getDevices("user@test.com", 1L).get(0);

        assertThat(response.isStateOn()).isNotNull();
        assertThat(response.getBrightness()).isNotNull();
        assertThat(response.getTemperature()).isNull();
        assertThat(response.getSensorValue()).isNull();
        assertThat(response.getCoverPosition()).isNull();
    }

    @Test
    void toResponse_thermostat_onlyStateOnAndTemperatureAreNonNull() {
        Device device = new Device(room, "Thermostat", DeviceType.THERMOSTAT);
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        DeviceResponse response = deviceService.getDevices("user@test.com", 1L).get(0);

        assertThat(response.isStateOn()).isNotNull();
        assertThat(response.getBrightness()).isNull();
        assertThat(response.getTemperature()).isNotNull();
        assertThat(response.getSensorValue()).isNull();
        assertThat(response.getCoverPosition()).isNull();
    }

    @Test
    void toResponse_sensor_onlyTemperatureAndSensorValueAreNonNull() {
        Device device = new Device(room, "Sensor", DeviceType.SENSOR);
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        DeviceResponse response = deviceService.getDevices("user@test.com", 1L).get(0);

        assertThat(response.isStateOn()).isNull();
        assertThat(response.getBrightness()).isNull();
        assertThat(response.getTemperature()).isNotNull();
        assertThat(response.getSensorValue()).isNotNull();
        assertThat(response.getCoverPosition()).isNull();
    }

    @Test
    void toResponse_cover_onlyStateOnAndCoverPositionAreNonNull() {
        Device device = new Device(room, "Cover", DeviceType.COVER);
        when(memberService.resolveEffectiveOwner("user@test.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        DeviceResponse response = deviceService.getDevices("user@test.com", 1L).get(0);

        assertThat(response.isStateOn()).isNotNull();
        assertThat(response.getBrightness()).isNull();
        assertThat(response.getTemperature()).isNull();
        assertThat(response.getSensorValue()).isNull();
        assertThat(response.getCoverPosition()).isNotNull();
    }
}
