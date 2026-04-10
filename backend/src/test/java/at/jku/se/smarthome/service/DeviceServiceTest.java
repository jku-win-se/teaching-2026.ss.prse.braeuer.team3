package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceRequest;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private DeviceService deviceService;

    private User user;
    private Room room;

    @BeforeEach
    void setUp() {
        user = new User("Test User", "user@test.com", "hashed");
        room = new Room(user, "Living Room", "weekend");
    }

    // --- getDevices ---

    @Test
    void getDevices_returnsListForOwnedRoom() {
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByRoomIdOrderByCreatedAtAsc(room.getId())).thenReturn(List.of(device));

        List<DeviceResponse> result = deviceService.getDevices("user@test.com", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Lamp");
        assertThat(result.get(0).getType()).isEqualTo(DeviceType.SWITCH);
    }

    @Test
    void getDevices_throwsNotFound_whenRoomBelongsToOtherUser() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.addDevice("user@test.com", 1L, request))
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
}
