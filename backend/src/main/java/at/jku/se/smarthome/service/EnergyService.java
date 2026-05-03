package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for estimating household energy consumption from existing devices.
 *
 * <p>US-016: Consumption is estimated from each device's type and current state.
 * The application does not yet persist historical metering samples, so weekly
 * values are projected from the current daily estimate.</p>
 *
 * <p>FR-16: CSV export via {@link #exportEnergyCsv(String)}.</p>
 */
@Service
public class EnergyService {

    private static final int SWITCH_WATTAGE = 60;
    private static final int DIMMER_WATTAGE = 12;
    private static final int THERMOSTAT_WATTAGE = 900;
    private static final int SENSOR_WATTAGE = 2;
    private static final int COVER_WATTAGE = 350;

    private static final double SWITCH_ACTIVE_HOURS = 6.0;
    private static final double DIMMER_ACTIVE_HOURS = 5.0;
    private static final double THERMOSTAT_ACTIVE_HOURS = 3.0;
    private static final double SENSOR_ACTIVE_HOURS = 24.0;
    private static final double COVER_ACTIVE_HOURS = 0.05;
    private static final double DAYS_PER_WEEK = 7.0;

    private final DeviceRepository deviceRepository;
    private final MemberService memberService;
    private final CsvExportService csvExportService;

    /**
     * Constructs an EnergyService with the required collaborators.
     *
     * @param deviceRepository repository for device lookups
     * @param memberService    service for resolving the effective home owner
     * @param csvExportService service for building CSV output (FR-16)
     */
    public EnergyService(DeviceRepository deviceRepository,
                         MemberService memberService,
                         CsvExportService csvExportService) {
        this.deviceRepository = deviceRepository;
        this.memberService = memberService;
        this.csvExportService = csvExportService;
    }

    /**
     * Returns estimated energy consumption for every device in the caller's
     * effective household.
     *
     * @param email authenticated user's email address
     * @return device-level energy responses ordered by room and device name
     */
    @Transactional(readOnly = true)
    public List<EnergyDeviceResponse> getDeviceEnergy(String email) {
        return getDeviceEnergyList(email);
    }

    /**
     * Exports energy usage data for the caller's household as a CSV string (FR-16).
     *
     * <p>Accessible to all authenticated users (Owner and Member alike),
     * consistent with the energy dashboard visibility.</p>
     *
     * @param email authenticated user's email address
     * @return RFC-4180 CSV content with header row; columns: Device, Room, Wattage (W), Today (kWh), Week (kWh)
     */
    @Transactional(readOnly = true)
    public String exportEnergyCsv(String email) {
        return csvExportService.buildEnergyCsv(getDeviceEnergyList(email));
    }

    private List<EnergyDeviceResponse> getDeviceEnergyList(String email) {
        User effectiveOwner = memberService.resolveEffectiveOwner(email);
        return deviceRepository.findAllByRoomUserId(effectiveOwner.getId())
                .stream()
                .sorted(Comparator
                        .comparing((Device device) -> device.getRoom().getName())
                        .thenComparing(Device::getName))
                .map(this::toEnergyResponse)
                .toList();
    }

    private EnergyDeviceResponse toEnergyResponse(Device device) {
        int wattage = estimateWattage(device);
        double todayKwh = estimateDailyKwh(device, wattage);
        return new EnergyDeviceResponse(
                device.getId(),
                device.getName(),
                device.getRoom().getName(),
                wattage,
                roundKwh(todayKwh),
                roundKwh(todayKwh * DAYS_PER_WEEK));
    }

    private int estimateWattage(Device device) {
        DeviceType type = device.getType();
        if (type == DeviceType.SWITCH) {
            return SWITCH_WATTAGE;
        }
        if (type == DeviceType.DIMMER) {
            return DIMMER_WATTAGE;
        }
        if (type == DeviceType.THERMOSTAT) {
            return THERMOSTAT_WATTAGE;
        }
        if (type == DeviceType.SENSOR) {
            return SENSOR_WATTAGE;
        }
        return COVER_WATTAGE;
    }

    private double estimateDailyKwh(Device device, int wattage) {
        double activeHours = estimateActiveHours(device);
        double powerFactor = estimatePowerFactor(device);
        return wattage * activeHours * powerFactor / 1000.0;
    }

    private double estimateActiveHours(Device device) {
        DeviceType type = device.getType();
        if (type == DeviceType.SENSOR) {
            return SENSOR_ACTIVE_HOURS;
        }
        if (!device.isStateOn()) {
            return 0.0;
        }
        if (type == DeviceType.SWITCH) {
            return SWITCH_ACTIVE_HOURS;
        }
        if (type == DeviceType.DIMMER) {
            return DIMMER_ACTIVE_HOURS;
        }
        if (type == DeviceType.THERMOSTAT) {
            return THERMOSTAT_ACTIVE_HOURS;
        }
        return COVER_ACTIVE_HOURS;
    }

    private double estimatePowerFactor(Device device) {
        if (device.getType() != DeviceType.DIMMER) {
            return 1.0;
        }
        return Math.max(0, Math.min(100, device.getBrightness())) / 100.0;
    }

    private double roundKwh(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
