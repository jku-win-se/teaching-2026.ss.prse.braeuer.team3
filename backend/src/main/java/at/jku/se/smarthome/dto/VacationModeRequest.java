package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.VacationModeAction;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * Request DTO for creating a vacation mode.
 *
 * <p>Dates are expected in ISO 8601 format ({@code yyyy-MM-dd}).</p>
 *
 * <p>Implements FR-21: Urlaubsmodus.</p>
 */
public class VacationModeRequest {

    /** User-defined display name for the vacation mode (max 100 characters). */
    private String name;

    /**
     * The action to apply to the schedule during the vacation window.
     * Must be {@code ENABLE} or {@code DISABLE}.
     */
    private VacationModeAction action;

    /** Primary key of the schedule to assign to this vacation mode. */
    private Long scheduleId;

    /** First day of the vacation period (inclusive), in {@code yyyy-MM-dd} format. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** Last day of the vacation period (inclusive), in {@code yyyy-MM-dd} format. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Returns the display name for the vacation mode.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name for the vacation mode.
     *
     * @param name the name (max 100 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the primary key of the schedule to assign.
     *
     * @return the schedule id
     */
    public Long getScheduleId() {
        return scheduleId;
    }

    /**
     * Sets the primary key of the schedule to assign.
     *
     * @param scheduleId the schedule id
     */
    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    /**
     * Returns the start date of the vacation period.
     *
     * @return the start date (inclusive)
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date of the vacation period.
     *
     * @param startDate the start date (inclusive)
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the end date of the vacation period.
     *
     * @return the end date (inclusive)
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date of the vacation period.
     *
     * @param endDate the end date (inclusive, must be &gt;= startDate)
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns the action to apply during the vacation window.
     *
     * @return the action (ENABLE or DISABLE)
     */
    public VacationModeAction getAction() {
        return action;
    }

    /**
     * Sets the action to apply during the vacation window.
     *
     * @param action the action (ENABLE or DISABLE); must not be null
     */
    public void setAction(VacationModeAction action) {
        this.action = action;
    }
}
