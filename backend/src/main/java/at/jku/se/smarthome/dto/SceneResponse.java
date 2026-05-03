package at.jku.se.smarthome.dto;

import java.util.List;

/**
 * Response DTO returned by the Scenes API for every read, create, and update operation.
 *
 * <p>Implements US-018: Szene mit Name und mehreren Gerätezuständen erstellbar.</p>
 */
public class SceneResponse {

    /** Primary key of the scene. */
    private Long id;

    /** Display name of the scene. */
    private String name;

    /** Material icon identifier. */
    private String icon;

    /** Device-action entries belonging to this scene. */
    private List<SceneEntryResponse> entries;

    /**
     * Constructs a SceneResponse with all fields.
     *
     * @param id      the scene primary key
     * @param name    the display name
     * @param icon    the Material icon identifier
     * @param entries the device-action entries
     */
    public SceneResponse(Long id, String name, String icon, List<SceneEntryResponse> entries) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.entries = entries;
    }

    /**
     * Returns the scene id.
     *
     * @return primary key
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the scene display name.
     *
     * @return name string
     */
    public String getName() {
        return name;
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
     * Returns the device-action entries of this scene.
     *
     * @return list of entry responses
     */
    public List<SceneEntryResponse> getEntries() {
        return entries;
    }
}
