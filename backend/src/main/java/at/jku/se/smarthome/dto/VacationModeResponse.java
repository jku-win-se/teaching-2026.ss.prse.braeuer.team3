package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.VacationModeAction;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * Response DTO returned by the vacation mode API endpoints.
 *
 * <p>Includes the denormalized schedule name for convenient client-side display
 * and a computed {@code active} field indicating whether the vacation window
 * is currently in effect.</p>
 *
 * <p>Implements FR-21: Urlaubsmodus.</p>
 */
public class VacationModeResponse {

    /** Primary key of the vacation mode. */
    private Long id;

    /** User-defined display name. */
    private String name;

    /** Primary key of the assigned schedule. */
    private Long scheduleId;

    /** Display name of the assigned schedule. */
    private String scheduleName;

    /** First day of the vacation period (inclusive). */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** Last day of the vacation period (inclusive). */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** Whether this vacation mode has been permanently deactivated. */
    private boolean deactivated;

    /**
     * Whether the vacation mode is currently active (action has been applied and not yet restored).
     * {@code true} when {@code originalEnabled} has been captured and the rule is not deactivated.
     */
    private boolean active;

    /** The action applied to the schedule during the vacation window. */
    private VacationModeAction action;

    /**
     * Constructs a fully-populated {@code VacationModeResponse}.
     *
     * @param id           the vacation mode primary key
     * @param name         the display name
     * @param scheduleId   the assigned schedule primary key
     * @param scheduleName the assigned schedule display name
     * @param startDate    the vacation start date (inclusive)
     * @param endDate      the vacation end date (inclusive)
     * @param deactivated  whether permanently deactivated
     * @param active       whether currently active (action applied, not yet restored)
     * @param action       the action applied during the vacation window
     */
    public VacationModeResponse(Long id, String name, Long scheduleId, String scheduleName,
                                LocalDate startDate, LocalDate endDate,
                                boolean deactivated, boolean active, VacationModeAction action) {
        this.id = id;
        this.name = name;
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.deactivated = deactivated;
        this.active = active;
        this.action = action;
    }

    /**
     * Returns the vacation mode primary key.
     *
     * @return the id
     */
    public Long getId() { return id; }

    /**
     * Returns the display name.
     *
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Returns the assigned schedule primary key.
     *
     * @return the schedule id
     */
    public Long getScheduleId() { return scheduleId; }

    /**
     * Returns the assigned schedule display name.
     *
     * @return the schedule name
     */
    public String getScheduleName() { return scheduleName; }

    /**
     * Returns the vacation start date (inclusive).
     *
     * @return the start date
     */
    public LocalDate getStartDate() { return startDate; }

    /**
     * Returns the vacation end date (inclusive).
     *
     * @return the end date
     */
    public LocalDate getEndDate() { return endDate; }

    /**
     * Returns whether this vacation mode has been permanently deactivated.
     *
     * @return {@code true} if permanently deactivated
     */
    public boolean isDeactivated() { return deactivated; }

    /**
     * Returns whether the vacation mode is currently active.
     *
     * @return {@code true} when the action has been applied and the rule is not deactivated
     */
    public boolean isActive() { return active; }

    /**
     * Returns the action applied to the schedule during the vacation window.
     *
     * @return the action (ENABLE or DISABLE)
     */
    public VacationModeAction getAction() { return action; }
}
