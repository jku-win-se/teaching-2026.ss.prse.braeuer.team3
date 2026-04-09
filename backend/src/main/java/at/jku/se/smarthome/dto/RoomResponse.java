package at.jku.se.smarthome.dto;

/**
 * Response DTO representing a room returned by the API (US-004).
 */
public class RoomResponse {

    /** The room's primary key. */
    private Long id;

    /** The human-readable room name. */
    private String name;

    /** The Material Design icon identifier. */
    private String icon;

    /**
     * Creates a new RoomResponse.
     *
     * @param id   the room id
     * @param name the room name
     * @param icon the room icon
     */
    public RoomResponse(Long id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    /**
     * Returns the room id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the room name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the icon identifier.
     *
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }
}
