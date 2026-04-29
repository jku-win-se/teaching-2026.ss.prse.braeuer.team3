package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.TriggerOperator;
import at.jku.se.smarthome.domain.TriggerType;

/**
 * Response body returned for IF-THEN rule operations (FR-10).
 *
 * <p>Used for all {@code GET}, {@code POST}, {@code PUT}, and {@code PATCH} responses
 * on {@code /api/rules}. Immutable — constructed once from a {@link at.jku.se.smarthome.domain.Rule}
 * entity and never modified.</p>
 */
public class RuleResponse {

    private final Long id;
    private final String name;
    private final boolean enabled;
    private final TriggerType triggerType;
    private final Long triggerDeviceId;
    private final String triggerDeviceName;
    private final TriggerOperator triggerOperator;
    private final Double triggerThresholdValue;
    private final Integer triggerHour;
    private final Integer triggerMinute;
    private final String triggerDaysOfWeek;
    private final Long actionDeviceId;
    private final String actionDeviceName;
    private final String actionValue;

    /**
     * Constructs a RuleResponse with all fields.
     *
     * @param id                    the rule's primary key
     * @param name                  the rule's display name
     * @param enabled               whether the rule is active
     * @param triggerType           THRESHOLD, EVENT, or TIME
     * @param triggerDeviceId       primary key of the trigger device; {@code null} for TIME rules
     * @param triggerDeviceName     display name of the trigger device; {@code null} for TIME rules
     * @param triggerOperator       GT or LT for THRESHOLD rules; {@code null} otherwise
     * @param triggerThresholdValue numeric threshold for THRESHOLD rules; {@code null} otherwise
     * @param triggerHour           hour (0–23) for TIME rules; {@code null} otherwise
     * @param triggerMinute         minute (0–59) for TIME rules; {@code null} otherwise
     * @param triggerDaysOfWeek     comma-separated days for TIME rules; {@code null} otherwise
     * @param actionDeviceId        primary key of the action device
     * @param actionDeviceName      display name of the action device
     * @param actionValue           action string applied to the action device on fire
     */
    public RuleResponse(Long id, String name, boolean enabled,
                        TriggerType triggerType, Long triggerDeviceId, String triggerDeviceName,
                        TriggerOperator triggerOperator, Double triggerThresholdValue,
                        Integer triggerHour, Integer triggerMinute, String triggerDaysOfWeek,
                        Long actionDeviceId, String actionDeviceName, String actionValue) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.triggerType = triggerType;
        this.triggerDeviceId = triggerDeviceId;
        this.triggerDeviceName = triggerDeviceName;
        this.triggerOperator = triggerOperator;
        this.triggerThresholdValue = triggerThresholdValue;
        this.triggerHour = triggerHour;
        this.triggerMinute = triggerMinute;
        this.triggerDaysOfWeek = triggerDaysOfWeek;
        this.actionDeviceId = actionDeviceId;
        this.actionDeviceName = actionDeviceName;
        this.actionValue = actionValue;
    }

    /**
     * Returns the rule's primary key.
     *
     * @return the rule id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the rule's display name.
     *
     * @return the rule name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the rule is currently enabled.
     *
     * @return {@code true} if the rule is active
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the trigger type of this rule.
     *
     * @return THRESHOLD or EVENT
     */
    public TriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * Returns the primary key of the trigger device.
     *
     * @return trigger device id
     */
    public Long getTriggerDeviceId() {
        return triggerDeviceId;
    }

    /**
     * Returns the display name of the trigger device.
     *
     * @return trigger device name
     */
    public String getTriggerDeviceName() {
        return triggerDeviceName;
    }

    /**
     * Returns the comparison operator for THRESHOLD rules.
     *
     * @return GT or LT, or {@code null} for EVENT rules
     */
    public TriggerOperator getTriggerOperator() {
        return triggerOperator;
    }

    /**
     * Returns the numeric threshold for THRESHOLD rules.
     *
     * @return threshold value, or {@code null} for EVENT rules
     */
    public Double getTriggerThresholdValue() {
        return triggerThresholdValue;
    }

    /**
     * Returns the primary key of the action device.
     *
     * @return action device id
     */
    public Long getActionDeviceId() {
        return actionDeviceId;
    }

    /**
     * Returns the display name of the action device.
     *
     * @return action device name
     */
    public String getActionDeviceName() {
        return actionDeviceName;
    }

    /**
     * Returns the action value applied to the action device when this rule fires.
     *
     * @return {@code "true"}/{@code "false"} for Switch; {@code "open"}/{@code "close"} for Shutter
     */
    public String getActionValue() {
        return actionValue;
    }

    /**
     * Returns the trigger hour for TIME rules.
     *
     * @return hour (0–23), or {@code null} for non-TIME rules
     */
    public Integer getTriggerHour() {
        return triggerHour;
    }

    /**
     * Returns the trigger minute for TIME rules.
     *
     * @return minute (0–59), or {@code null} for non-TIME rules
     */
    public Integer getTriggerMinute() {
        return triggerMinute;
    }

    /**
     * Returns the comma-separated day-of-week string for TIME rules.
     *
     * @return e.g. {@code "MONDAY,FRIDAY"}, or {@code null} for non-TIME rules
     */
    public String getTriggerDaysOfWeek() {
        return triggerDaysOfWeek;
    }
}
