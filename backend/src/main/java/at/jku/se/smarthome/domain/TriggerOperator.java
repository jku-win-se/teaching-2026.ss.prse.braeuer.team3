package at.jku.se.smarthome.domain;

/**
 * Comparison operator used in {@link TriggerType#THRESHOLD} rules.
 *
 * <p>Defines whether the sensor value must be greater than or less than
 * the configured threshold for the rule to fire.</p>
 */
public enum TriggerOperator {

    /** Rule fires when the sensor value is strictly greater than the threshold. */
    GT,

    /** Rule fires when the sensor value is strictly less than the threshold. */
    LT
}
