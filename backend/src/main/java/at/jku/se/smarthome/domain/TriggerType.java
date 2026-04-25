package at.jku.se.smarthome.domain;

/**
 * Enumeration of supported rule trigger types for FR-10.
 *
 * <p>Determines how a {@link Rule} is activated:
 * a sensor value crossing a threshold, or a device being turned on or off.</p>
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
    EVENT
}
