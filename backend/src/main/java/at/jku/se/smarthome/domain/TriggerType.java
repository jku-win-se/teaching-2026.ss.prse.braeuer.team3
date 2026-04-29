package at.jku.se.smarthome.domain;

/**
 * Enumeration of supported rule trigger types for FR-10 / US-012.
 *
 * <p>Determines how a {@link Rule} is activated:
 * a sensor value crossing a threshold, a device state change, or a scheduled time.</p>
 */
public enum TriggerType {

    /**
     * Rule fires when a sensor's numeric value satisfies a comparison operator
     * against a configured threshold on every device state update.
     */
    THRESHOLD,

    /**
     * Rule fires when a device's {@code stateOn} field changes in a state update.
     */
    EVENT,

    /**
     * Rule fires at a configured hour and minute on selected days of the week.
     * Evaluated once per minute by {@link at.jku.se.smarthome.service.RuleScheduler}.
     */
    TIME
}
