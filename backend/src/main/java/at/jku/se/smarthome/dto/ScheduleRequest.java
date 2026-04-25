package at.jku.se.smarthome.dto;

import java.util.List;

/**
 * Request DTO for creating or updating a device schedule.
 *
 * <p>Used by {@link at.jku.se.smarthome.controller.ScheduleController} for
 * POST (create) and PUT (update) operations. All fields are required.</p>
 *
 * <p>Implements FR-09: Zeitpläne konfigurieren.</p>
 */
public class ScheduleRequest {

    /** User-defined display name for the schedule (max 100 characters). */
    private String name;

    /** Primary key of the target device. */
    private Long deviceId;

    /**
     * Days of week on which the schedule fires.
     * Values must be uppercase Java {@link java.time.DayOfWeek} names,
     * e.g. {@code ["MONDAY", "FRIDAY"]}.
     */
    private List<String> daysOfWeek;

    /** Hour of day to fire (0–23, server timezone). */
    private int hour;

    /** Minute of hour to fire (0–59). */
    private int minute;

    /**
     * JSON-serialized {@link DeviceStateRequest} describing the state to apply.
     * At least one field must be non-null.
     */
    private String actionPayload;

    /** Whether the schedule should be active immediately. Defaults to {@code true}. */
    private boolean enabled = true;

    /**
     * Returns the schedule name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the schedule name.
     *
     * @param name the display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the target device ID.
     *
     * @return the device primary key
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the target device ID.
     *
     * @param deviceId the device primary key
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns the days of week on which the schedule fires.
     *
     * @return list of uppercase day names
     */
    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    /**
     * Sets the days of week.
     *
     * @param daysOfWeek list of uppercase Java DayOfWeek names
     */
    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    // CPD-OFF — getters/setters for hour, minute, actionPayload and enabled are structurally
    // identical to Schedule.java by design (DTO mirrors entity fields); not a real duplication.

    /**
     * Returns the hour of day (0–23).
     *
     * @return hour
     */
    public int getHour() {
        return hour;
    }

    /**
     * Sets the hour of day.
     *
     * @param hour value in range 0–23
     */
    public void setHour(int hour) {
        this.hour = hour;
    }

    /**
     * Returns the minute of hour (0–59).
     *
     * @return minute
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Sets the minute of hour.
     *
     * @param minute value in range 0–59
     */
    public void setMinute(int minute) {
        this.minute = minute;
    }

    /**
     * Returns the JSON-serialized action payload.
     *
     * @return action payload JSON string
     */
    public String getActionPayload() {
        return actionPayload;
    }

    /**
     * Sets the JSON-serialized action payload.
     *
     * @param actionPayload JSON string representing a {@link DeviceStateRequest}
     */
    public void setActionPayload(String actionPayload) {
        this.actionPayload = actionPayload;
    }

    /**
     * Returns whether the schedule is enabled.
     *
     * @return {@code true} if the schedule should fire
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled flag.
     *
     * @param enabled {@code true} to activate the schedule
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    // CPD-ON
}
