package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a virtual device to a room (FR-04).
 */
public class DeviceRequest {

    /** The device name (required, max 50 characters). */
    @NotBlank(message = "Device name is required")
    @Size(max = 50, message = "Device name must not exceed 50 characters")
    private String name;

    /** The device type (required). */
    @NotNull(message = "Device type is required")
    private DeviceType type;

    /**
     * Returns the device name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the device name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the device type.
     *
     * @return the type
     */
    public DeviceType getType() {
        return type;
    }

    /**
     * Sets the device type.
     *
     * @param type the type to set
     */
    public void setType(DeviceType type) {
        this.type = type;
    }
}
