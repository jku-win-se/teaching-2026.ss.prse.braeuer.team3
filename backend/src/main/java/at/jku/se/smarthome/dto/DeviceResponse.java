package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.DeviceType;

/**
 * Response DTO representing a device returned by the API.
 *
 * <p>FR-04: add virtual smart devices.
 * FR-06: includes persisted device state fields.</p>
 */
public class DeviceResponse {

    /** The device's primary key. */
    private Long id;

    /** The human-readable device name. */
    private String name;

    /** The device type, serialized as a lowercase string. */
    private DeviceType type;

    /** Whether the device is switched on. */
    private boolean stateOn;

    /** Brightness level (0–100), used by dimmer devices. */
    private int brightness;

    /** Thermostat target temperature in degrees Celsius. */
    private double temperature;

    /** Current sensor reading. */
    private double sensorValue;

    /** Cover position: 0 = closed, 100 = open. */
    private int coverPosition;

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
     * Creates a DeviceResponse with full state.
     * Used by FR-06 endpoints that return persisted state.
     *
     * @param id            the device id
     * @param name          the device name
     * @param type          the device type
     * @param stateOn       whether the device is on
     * @param brightness    brightness level (0–100)
     * @param temperature   thermostat target temperature
     * @param sensorValue   current sensor reading
     * @param coverPosition cover position (0 or 100)
     */
    public DeviceResponse(Long id, String name, DeviceType type,
                          boolean stateOn, int brightness, double temperature,
                          double sensorValue, int coverPosition) {
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
     * Returns whether the device is switched on.
     *
     * @return {@code true} if on
     */
    public boolean isStateOn() {
        return stateOn;
    }

    /**
     * Returns the brightness level.
     *
     * @return brightness (0–100)
     */
    public int getBrightness() {
        return brightness;
    }

    /**
     * Returns the thermostat target temperature.
     *
     * @return temperature in degrees Celsius
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns the current sensor value.
     *
     * @return sensor reading
     */
    public double getSensorValue() {
        return sensorValue;
    }

    /**
     * Returns the cover position.
     *
     * @return 0 for closed, 100 for open
     */
    public int getCoverPosition() {
        return coverPosition;
    }
}
