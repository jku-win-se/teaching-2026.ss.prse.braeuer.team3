package at.jku.se.smarthome.dto;

/**
 * Represents a single device state change produced during a day simulation (US-020).
 *
 * <p>Events are ordered chronologically by {@link #hour} and {@link #minute}.
 * Each event records which rule caused the change and what the resulting state is.</p>
 */
public class SimulationEvent {

    /** Simulated hour of day (0–23) at which this event occurs. */
    private int hour;

    /** Simulated minute of hour (0–59) at which this event occurs. */
    private int minute;

    /** Primary key of the device whose state changed. */
    private Long deviceId;

    /** Human-readable name of the affected device. */
    private String deviceName;

    /** Name of the room the device belongs to. */
    private String roomName;

    /**
     * The action applied to the device by the rule.
     * {@code "true"}/{@code "false"} for Switch; {@code "open"}/{@code "close"} for Cover.
     */
    private String actionValue;

    /** Display name of the rule that triggered this change. */
    private String ruleName;

    /** Primary key of the rule that triggered this change. */
    private Long ruleId;

    /**
     * Constructs a fully initialised simulation event.
     *
     * @param hour       simulated hour (0–23)
     * @param minute     simulated minute (0–59)
     * @param deviceId   primary key of the affected device
     * @param deviceName display name of the affected device
     * @param roomName   name of the room the device belongs to
     * @param actionValue action applied ({@code "true"}/{@code "false"}/{@code "open"}/{@code "close"})
     * @param ruleName   display name of the triggering rule
     * @param ruleId     primary key of the triggering rule
     */
    public SimulationEvent(int hour, int minute, Long deviceId, String deviceName,
                           String roomName, String actionValue, String ruleName, Long ruleId) {
        this.hour = hour;
        this.minute = minute;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.roomName = roomName;
        this.actionValue = actionValue;
        this.ruleName = ruleName;
        this.ruleId = ruleId;
    }

    /**
     * Returns the simulated hour.
     *
     * @return hour (0–23)
     */
    public int getHour() {
        return hour;
    }

    /**
     * Sets the simulated hour.
     *
     * @param hour hour (0–23)
     */
    public void setHour(int hour) {
        this.hour = hour;
    }

    /**
     * Returns the simulated minute.
     *
     * @return minute (0–59)
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Sets the simulated minute.
     *
     * @param minute minute (0–59)
     */
    public void setMinute(int minute) {
        this.minute = minute;
    }

    /**
     * Returns the primary key of the affected device.
     *
     * @return device id
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the primary key of the affected device.
     *
     * @param deviceId device id
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns the display name of the affected device.
     *
     * @return device name
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Sets the display name of the affected device.
     *
     * @param deviceName device name
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * Returns the action value applied to the device.
     *
     * @return {@code "true"}, {@code "false"}, {@code "open"}, or {@code "close"}
     */
    public String getActionValue() {
        return actionValue;
    }

    /**
     * Sets the action value applied to the device.
     *
     * @param actionValue the action string
     */
    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
}
