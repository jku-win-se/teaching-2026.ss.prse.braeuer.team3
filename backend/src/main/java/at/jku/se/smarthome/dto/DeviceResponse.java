package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.DeviceType;

/**
 * Response DTO representing a device returned by the API.
 *
 * <p>FR-04: add virtual smart devices.
 * FR-06: includes persisted device state fields.</p>
 *
 * <p>State fields use wrapper types so that inapplicable fields for a given
 * device type are returned as {@code null} rather than a misleading default value.
 * For example, a {@code SWITCH} device will have {@code null} for brightness,
 * temperature, sensorValue, and coverPosition.</p>
 */
public class DeviceResponse {

    /** The device's primary key. */
    private Long id;

    /** The human-readable device name. */
    private String name;

    /** The device type, serialized as a lowercase string. */
    private DeviceType type;

    /** Whether the device is switched on, or {@code null} if not applicable for this type. */
    private Boolean stateOn;

    /** Brightness level (0–100) for dimmer devices, or {@code null} if not applicable. */
    private Integer brightness;

    /** Thermostat target temperature in degrees Celsius, or {@code null} if not applicable. */
    private Double temperature;

    /** Current sensor reading, or {@code null} if not applicable. */
    private Double sensorValue;

    /** Cover position (0 = closed, 100 = open), or {@code null} if not applicable. */
    private Integer coverPosition;

    /**
     * Creates a DeviceResponse without state (defaults to off/50/21/0/0).
     * Used by FR-04 / FR-05 where state is not relevant.
     *
     * @param id   the device id
     * @param name the device name
     * @param type the device type
     */
    public DeviceResponse(Long id, String name, DeviceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Creates a DeviceResponse with type-aware state.
     * Used by FR-06 endpoints that return persisted state.
     * Fields that are not applicable for the device type are passed as {@code null}.
     *
     * @param id            the device id
     * @param name          the device name
     * @param type          the device type
     * @param stateOn       whether the device is on, or {@code null} if not applicable
     * @param brightness    brightness level (0–100), or {@code null} if not applicable
     * @param temperature   thermostat target temperature, or {@code null} if not applicable
     * @param sensorValue   current sensor reading, or {@code null} if not applicable
     * @param coverPosition cover position (0 or 100), or {@code null} if not applicable
     */
    public DeviceResponse(Long id, String name, DeviceType type,
                          Boolean stateOn, Integer brightness, Double temperature,
                          Double sensorValue, Integer coverPosition) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.stateOn = stateOn;
        this.brightness = brightness;
        this.temperature = temperature;
        this.sensorValue = sensorValue;
        this.coverPosition = coverPosition;
    }

    /**
     * Returns the device id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the device name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the device type.
     *
     * @return the type
     */
    public DeviceType getType() {
        return type;
    }

    /**
     * Returns whether the device is switched on, or {@code null} if not applicable for this type.
     *
     * @return {@code true} if on, {@code false} if off, {@code null} if not applicable
     */
    public Boolean isStateOn() {
        return stateOn;
    }

    /**
     * Returns the brightness level, or {@code null} if not applicable for this type.
     *
     * @return brightness (0–100), or {@code null}
     */
    public Integer getBrightness() {
        return brightness;
    }

    /**
     * Returns the thermostat target temperature, or {@code null} if not applicable for this type.
     *
     * @return temperature in degrees Celsius, or {@code null}
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * Returns the current sensor value, or {@code null} if not applicable for this type.
     *
     * @return sensor reading, or {@code null}
     */
    public Double getSensorValue() {
        return sensorValue;
    }

    /**
     * Returns the cover position, or {@code null} if not applicable for this type.
     *
     * @return 0 for closed, 100 for open, or {@code null}
     */
    public Integer getCoverPosition() {
        return coverPosition;
    }
}
