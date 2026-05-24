package at.jku.se.smarthome.dto;

/**
 * Represents the initial state of a single device at the start of a day simulation (US-020).
 *
 * <p>All fields are optional overrides. Any field left at its default value means
 * "use the device's current live state as starting point".</p>
 */
public class DeviceStartCondition {

    /** The primary key of the device whose starting state is being overridden. */
    private Long deviceId;

    /** Whether the device is switched on at simulation start. */
    private boolean stateOn;

    /** Brightness level (0–100) for dimmer devices at simulation start. */
    private int brightness = 50;

    /** Thermostat target temperature at simulation start. */
    private double temperature = 21.0;

    /** Sensor reading value at simulation start. */
    private double sensorValue = 0.0;

    /** Cover position (0 = closed, 100 = open) at simulation start. */
    private int coverPosition = 0;

    /** Default no-arg constructor required for JSON deserialization. */
    public DeviceStartCondition() {
    }

    /**
     * Returns the device ID this start condition applies to.
     *
     * @return the device primary key
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the device ID this start condition applies to.
     *
     * @param deviceId the device primary key
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns whether the device is switched on at simulation start.
     *
     * @return {@code true} if the device starts on
     */
    public boolean isStateOn() {
        return stateOn;
    }

    /**
     * Sets whether the device is switched on at simulation start.
     *
     * @param stateOn {@code true} to start on
     */
    public void setStateOn(boolean stateOn) {
        this.stateOn = stateOn;
    }

    /**
     * Returns the brightness level at simulation start.
     *
     * @return brightness (0–100)
     */
    public int getBrightness() {
        return brightness;
    }

    /**
     * Sets the brightness level at simulation start.
     *
     * @param brightness brightness (0–100)
     */
    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    /**
     * Returns the thermostat temperature at simulation start.
     *
     * @return temperature in degrees Celsius
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Sets the thermostat temperature at simulation start.
     *
     * @param temperature temperature in degrees Celsius
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * Returns the sensor value at simulation start.
     *
     * @return sensor reading
     */
    public double getSensorValue() {
        return sensorValue;
    }

    /**
     * Sets the sensor value at simulation start.
     *
     * @param sensorValue sensor reading
     */
    public void setSensorValue(double sensorValue) {
        this.sensorValue = sensorValue;
    }

    /**
     * Returns the cover position at simulation start.
     *
     * @return cover position (0 = closed, 100 = open)
     */
    public int getCoverPosition() {
        return coverPosition;
    }

}
