package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.TriggerOperator;
import at.jku.se.smarthome.domain.TriggerType;

/**
 * Request body for creating or replacing an IF-THEN rule (FR-10).
 *
 * <p>Used for {@code POST /api/rules} and {@code PUT /api/rules/{id}}.
 * Fields {@code triggerOperator} and {@code triggerThresholdValue} are only
 * required when {@code triggerType} is {@code THRESHOLD}.</p>
 */
public class RuleRequest {

    /** Display name for the rule (required, max 100 characters). */
    private String name;

    /** Whether the rule is active; defaults to {@code true} if omitted. */
    private Boolean enabled;

    /** The type of trigger: {@code THRESHOLD} or {@code EVENT}. */
    private TriggerType triggerType;

    /** Primary key of the device whose state change triggers this rule. */
    private Long triggerDeviceId;

    /**
     * Comparison operator for THRESHOLD rules ({@code GT} or {@code LT}).
     * Must be {@code null} for EVENT rules.
     */
    private TriggerOperator triggerOperator;

    /**
     * Numeric threshold value for THRESHOLD rules.
     * Must be {@code null} for EVENT rules.
     */
    private Double triggerThresholdValue;

    /** Primary key of the device to control when the rule fires. */
    private Long actionDeviceId;

    /**
     * Action to apply to the action device when the rule fires.
     * {@code "true"} or {@code "false"} for Switch;
     * {@code "open"} or {@code "close"} for Shutter.
     */
    private String actionValue;

    /**
     * Returns the display name of the rule.
     *
     * @return the rule name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the rule.
     *
     * @param name the rule name (max 100 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns whether the rule should be enabled on creation/update.
     *
     * @return {@code true} if enabled, {@code false} if disabled, or {@code null} to use default
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets whether the rule should be enabled.
     *
     * @param enabled enabled flag; {@code null} causes the service to default to {@code true}
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the trigger type.
     *
     * @return THRESHOLD or EVENT
     */
    public TriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * Sets the trigger type.
     *
     * @param triggerType THRESHOLD or EVENT
     */
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * Returns the id of the trigger device.
     *
     * @return trigger device id
     */
    public Long getTriggerDeviceId() {
        return triggerDeviceId;
    }

    /**
     * Sets the id of the trigger device.
     *
     * @param triggerDeviceId primary key of the trigger device
     */
    public void setTriggerDeviceId(Long triggerDeviceId) {
        this.triggerDeviceId = triggerDeviceId;
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
     * Sets the comparison operator for THRESHOLD rules.
     *
     * @param triggerOperator GT or LT; {@code null} for EVENT rules
     */
    public void setTriggerOperator(TriggerOperator triggerOperator) {
        this.triggerOperator = triggerOperator;
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
     * Sets the numeric threshold for THRESHOLD rules.
     *
     * @param triggerThresholdValue the threshold; {@code null} for EVENT rules
     */
    public void setTriggerThresholdValue(Double triggerThresholdValue) {
        this.triggerThresholdValue = triggerThresholdValue;
    }

    /**
     * Returns the id of the action device.
     *
     * @return action device id
     */
    public Long getActionDeviceId() {
        return actionDeviceId;
    }

    /**
     * Sets the id of the action device.
     *
     * @param actionDeviceId primary key of the action device
     */
    public void setActionDeviceId(Long actionDeviceId) {
        this.actionDeviceId = actionDeviceId;
    }

    /**
     * Returns the action value string.
     *
     * @return {@code "true"}/{@code "false"} for Switch; {@code "open"}/{@code "close"} for Shutter
     */
    public String getActionValue() {
        return actionValue;
    }

    /**
     * Sets the action value string.
     *
     * @param actionValue {@code "true"}/{@code "false"} for Switch; {@code "open"}/{@code "close"} for Shutter
     */
    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
}
