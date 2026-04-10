package at.jku.se.smarthome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or renaming a room (US-004).
 */
public class RoomRequest {

    /** The name of the room (max 30 characters). */
    @NotBlank(message = "Room name is required")
    @Size(max = 30, message = "Room name must not exceed 30 characters")
    private String name;

    /** The Material Design icon identifier for the room. Defaults to "weekend". */
    private String icon = "weekend";

    /**
     * Returns the room name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the room name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the icon identifier.
     *
     * @return the icon name
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Sets the icon identifier.
     *
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }
}
