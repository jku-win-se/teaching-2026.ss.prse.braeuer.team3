package at.jku.se.smarthome.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of supported virtual smart device types.
 *
 * <p>Serialized as lowercase strings (e.g. {@code "switch"}) to match the
 * Angular frontend's {@code DeviceType} union type without any conversion.</p>
 *
 * <p>FR-04: switch, dimmer, thermostat, sensor, cover/blind.</p>
 */
public enum DeviceType {

    /** Simple on/off switch. */
    SWITCH,

    /** Dimmable light (0–100 % brightness). */
    DIMMER,

    /** Temperature controller. */
    THERMOSTAT,

    /** Value sensor (temperature, humidity, etc.). */
    SENSOR,

    /** Cover or blind (0 = closed, 100 = open). */
    COVER;

    /**
     * Returns the lowercase JSON representation of this type.
     *
     * @return lowercase name, e.g. {@code "switch"}
     */
    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    /**
     * Deserializes a string into a {@code DeviceType}.
     *
     * <p>Called by Jackson during request body deserialization, before Bean
     * Validation runs. A {@code null} value is returned as-is so that
     * {@code @NotNull} on the DTO field can produce a proper validation error.</p>
     *
     * @param value the type string from JSON (case-insensitive)
     * @return the matching enum constant, or {@code null} if value is null
     * @throws IllegalArgumentException if value is non-null but not a known type
     */
    @JsonCreator
    public static DeviceType fromJson(String value) {
        if (value == null) {
            return null;
        }
        return valueOf(value.toUpperCase());
    }
}
