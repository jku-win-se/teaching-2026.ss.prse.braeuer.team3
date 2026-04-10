package at.jku.se.smarthome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for renaming an existing virtual device.
 *
 * <p>FR-05: Gerät umbenennen.</p>
 */
public class RenameDeviceRequest {

    /** The new device name. Must not be blank and at most 50 characters. */
    @NotBlank(message = "Device name is required.")
    @Size(max = 50, message = "Device name must not exceed 50 characters.")
    private String name;

    /**
     * Returns the new device name.
     *
     * @return the device name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the new device name.
     *
     * @param name the device name
     */
    public void setName(String name) {
        this.name = name;
    }
}
