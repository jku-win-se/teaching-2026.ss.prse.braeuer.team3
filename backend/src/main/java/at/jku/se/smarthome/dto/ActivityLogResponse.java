package at.jku.se.smarthome.dto;

import java.time.Instant;

/**
 * Response DTO for an activity log entry returned by the REST API.
 *
 * <p>Carries all the fields needed by the frontend to display a single row
 * in the activity log table, including device and room names resolved from
 * the entity graph at query time.</p>
 *
 * <p>Implements FR-08: Aktivitätsprotokoll.</p>
 */
public class ActivityLogResponse {

    /** The log entry's primary key. */
    private Long id;

    /** When the action occurred. */
    private Instant timestamp;

    /** The primary key of the affected device. */
    private Long deviceId;

    /** The human-readable name of the affected device. */
    private String deviceName;

    /** The name of the room the affected device belongs to. */
    private String roomName;

    /** The display name of the actor who performed the action. */
    private String actorName;

    /** A human-readable description of the action performed. */
    private String action;

    /**
     * Creates an ActivityLogResponse with all fields populated.
     *
     * @param id         the log entry id
     * @param timestamp  when the action occurred
     * @param deviceId   the affected device id
     * @param deviceName the affected device name
     * @param roomName   the room the device belongs to
     * @param actorName  the actor's display name
     * @param action     the human-readable action description
     */
    public ActivityLogResponse(Long id, Instant timestamp, Long deviceId,
                               String deviceName, String roomName,
                               String actorName, String action) {
        this.id = id;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.roomName = roomName;
        this.actorName = actorName;
        this.action = action;
    }

    /**
     * Returns the log entry id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the timestamp when the action occurred.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the primary key of the affected device.
     *
     * @return the device id
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the name of the affected device.
     *
     * @return the device name
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Returns the name of the room the affected device belongs to.
     *
     * @return the room name
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Returns the display name of the actor who performed the action.
     *
     * @return the actor name
     */
    public String getActorName() {
        return actorName;
    }

    /**
     * Returns the human-readable description of the action performed.
     *
     * @return the action description
     */
    public String getAction() {
        return action;
    }
}
