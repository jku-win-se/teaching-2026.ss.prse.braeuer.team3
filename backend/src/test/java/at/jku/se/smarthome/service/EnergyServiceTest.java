package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MemberService memberService;

    private EnergyService energyService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Alice", "alice@example.com", "hashed");
        ReflectionTestUtils.setField(user, "id", 42L);
        energyService = new EnergyService(deviceRepository, memberService, new CsvExportService());
    }

    @Test
    void getDeviceEnergy_returnsEstimatedConsumptionForEffectiveOwnerDevices() {
        Room livingRoom = new Room(user, "Living Room", "weekend");
        Room kitchen = new Room(user, "Kitchen", "kitchen");

        Device dimmer = new Device(livingRoom, "Ceiling Light", DeviceType.DIMMER);
        ReflectionTestUtils.setField(dimmer, "id", 1L);
        dimmer.setStateOn(true);
        dimmer.setBrightness(50);

        Device sensor = new Device(kitchen, "Temperature Sensor", DeviceType.SENSOR);
        ReflectionTestUtils.setField(sensor, "id", 2L);

        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(deviceRepository.findAllByRoomUserId(42L)).thenReturn(List.of(sensor, dimmer));

        List<EnergyDeviceResponse> result = energyService.getDeviceEnergy("alice@example.com");

        assertThat(result).extracting(EnergyDeviceResponse::getDeviceName)
                .containsExactly("Temperature Sensor", "Ceiling Light");
        assertThat(result.get(0).getRoom()).isEqualTo("Kitchen");
        assertThat(result.get(0).getWattage()).isEqualTo(2);
        assertThat(result.get(0).getTodayKwh()).isEqualTo(0.05);
        assertThat(result.get(0).getWeekKwh()).isEqualTo(0.34);
        assertThat(result.get(1).getTodayKwh()).isEqualTo(0.03);
        assertThat(result.get(1).getWeekKwh()).isEqualTo(0.21);
    }

    @Test
    void getDeviceEnergy_returnsZeroConsumptionForInactiveSwitch() {
        Room room = new Room(user, "Office", "desk");
        Device switchDevice = new Device(room, "Desk Plug", DeviceType.SWITCH);
        ReflectionTestUtils.setField(switchDevice, "id", 7L);
        switchDevice.setStateOn(false);

        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(deviceRepository.findAllByRoomUserId(42L)).thenReturn(List.of(switchDevice));

        List<EnergyDeviceResponse> result = energyService.getDeviceEnergy("alice@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceId()).isEqualTo(7L);
        assertThat(result.get(0).getTodayKwh()).isZero();
        assertThat(result.get(0).getWeekKwh()).isZero();
    }
}
