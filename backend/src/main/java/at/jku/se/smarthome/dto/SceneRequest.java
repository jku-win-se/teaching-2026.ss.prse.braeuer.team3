package at.jku.se.smarthome.dto;

import java.util.List;

/**
 * Request DTO for creating or updating a scene (US-018).
 *
 * <p>The {@code entries} list must contain at least one device-action pair.
 * Each device must be owned by the authenticated user.</p>
 */
public class SceneRequest {

    /** User-defined display name for the scene (max 100 characters). */
    private String name;

    /** Material icon identifier displayed in the UI (max 50 characters). */
    private String icon;

    /** Device-action pairs to apply when the scene is activated. */
    private List<SceneEntryRequest> entries;

    /** Default constructor for Jackson deserialization. */
    public SceneRequest() {
    }

    /**
     * Returns the scene name.
     *
     * @return name string
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the scene name.
     *
     * @param name name string (max 100 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the Material icon identifier.
     *
     * @return icon string
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Sets the Material icon identifier.
     *
     * @param icon icon string (max 50 characters)
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Returns the list of device-action entry requests.
     *
     * @return entry request list
     */
    public List<SceneEntryRequest> getEntries() {
        return entries;
    }

    /**
     * Sets the list of device-action entry requests.
     *
     * @param entries entry request list
     */
    public void setEntries(List<SceneEntryRequest> entries) {
        this.entries = entries;
    }
}
