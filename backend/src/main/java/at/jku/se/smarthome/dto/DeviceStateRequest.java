package at.jku.se.smarthome.dto;

/**
 * Request DTO for partially updating the runtime state of a virtual device.
 *
 * <p>All fields are optional. A {@code null} value means the field is not changed.
 * FR-06: Gerät manuell steuern.</p>
 */
public class DeviceStateRequest {

    /** New on/off state. {@code null} = no change. */
    private Boolean stateOn;

    /** New brightness level (0–100). {@code null} = no change. */
    private Integer brightness;

    /** New thermostat target temperature. {@code null} = no change. */
    private Double temperature;

    /** New sensor value. {@code null} = no change. */
    private Double sensorValue;

    /** New cover position (0 = closed, 100 = open). {@code null} = no change. */
    private Integer coverPosition;

    /**
     * Returns the on/off state to apply, or {@code null} if unchanged.
     *
     * @return the new on/off state, or {@code null}
     */
    public Boolean getStateOn() {
        return stateOn;
    }

    /**
     * Sets the on/off state.
     *
     * @param stateOn the new state
     */
    public void setStateOn(Boolean stateOn) {
        this.stateOn = stateOn;
    }

    /**
     * Returns the brightness to apply, or {@code null} if unchanged.
     *
     * @return brightness (0–100), or {@code null}
     */
    public Integer getBrightness() {
        return brightness;
    }

    /**
     * Sets the brightness.
     *
     * @param brightness brightness percentage (0–100)
     */
    public void setBrightness(Integer brightness) {
        this.brightness = brightness;
    }

    /**
     * Returns the temperature to apply, or {@code null} if unchanged.
     *
     * @return temperature in degrees Celsius, or {@code null}
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * Sets the temperature.
     *
     * @param temperature temperature in degrees Celsius
     */
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * Returns the sensor value to apply, or {@code null} if unchanged.
     *
     * @return sensor reading, or {@code null}
     */
    public Double getSensorValue() {
        return sensorValue;
    }

    /**
     * Sets the sensor value.
     *
     * @param sensorValue the new sensor reading
     */
    public void setSensorValue(Double sensorValue) {
        this.sensorValue = sensorValue;
    }

    /**
     * Returns the cover position to apply, or {@code null} if unchanged.
     *
     * @return cover position (0 or 100), or {@code null}
     */
    public Integer getCoverPosition() {
        return coverPosition;
    }

    /**
     * Sets the cover position.
     *
     * @param coverPosition 0 for closed, 100 for open
     */
    public void setCoverPosition(Integer coverPosition) {
        this.coverPosition = coverPosition;
    }
}
