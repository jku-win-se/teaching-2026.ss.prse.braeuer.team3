package at.jku.se.smarthome.dto;

import java.util.List;

/**
 * Request body for a day simulation run (US-020).
 *
 * <p>The caller specifies which day of the week to simulate and optional
 * per-device starting states. The backend evaluates all automation rules
 * for that day purely in-memory without touching the live system.</p>
 */
public class SimulationRequest {

    /**
     * The day of the week to simulate, as an uppercase Java {@link java.time.DayOfWeek} name
     * (e.g. {@code "MONDAY"}, {@code "SATURDAY"}).
     */
    private String dayOfWeek;

    /**
     * Optional per-device starting states. Devices not listed here start with
     * their current live state from the database.
     */
    private List<DeviceStartCondition> startConditions;

    /** Default no-arg constructor required for JSON deserialization. */
    public SimulationRequest() {
    }

    /**
     * Returns the day of the week to simulate.
     *
     * @return uppercase day name, e.g. {@code "MONDAY"}
     */
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Sets the day of the week to simulate.
     *
     * @param dayOfWeek uppercase day name, e.g. {@code "MONDAY"}
     */
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * Returns the list of per-device start condition overrides.
     *
     * @return list of start conditions (may be {@code null} or empty)
     */
    public List<DeviceStartCondition> getStartConditions() {
        return startConditions;
    }

    /**
     * Sets the list of per-device start condition overrides.
     *
     * @param startConditions per-device starting states
     */
    public void setStartConditions(List<DeviceStartCondition> startConditions) {
        this.startConditions = startConditions;
    }
}
