package at.jku.se.smarthome.domain;

/**
 * Action applied to a schedule by a {@link VacationMode} rule.
 *
 * <p>A vacation mode rule either enables or disables the referenced schedule
 * for the duration of the vacation window, then restores the original state
 * when the window expires or the rule is manually deactivated.</p>
 *
 * <p>Implements FR-21: Urlaubsmodus.</p>
 */
public enum VacationModeAction {

    /** Enable the associated schedule during the vacation window. */
    ENABLE,

    /** Disable the associated schedule during the vacation window. */
    DISABLE
}
