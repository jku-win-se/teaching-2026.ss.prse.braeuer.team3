package at.jku.se.smarthome.dto;

import java.util.List;

/**
 * Response DTO returned by the schedule API endpoints.
 *
 * <p>Enriches the {@link at.jku.se.smarthome.domain.Schedule} entity with
 * denormalized device and room names for convenient client-side display.</p>
 *
 * <p>Implements FR-09: Zeitpläne konfigurieren.</p>
 */
public class ScheduleResponse {

    /** Primary key of the schedule. */
    private Long id;

    /** User-defined display name of the schedule. */
    private String name;

    /** Primary key of the target device. */
    private Long deviceId;

    /** Display name of the target device. */
    private String deviceName;

    /** Display name of the room the device belongs to. */
    private String roomName;

    /**
     * Days of week on which the schedule fires, as uppercase Java
     * {@link java.time.DayOfWeek} names, e.g. {@code ["MONDAY", "FRIDAY"]}.
     */
    private List<String> daysOfWeek;

    /** Hour of day at which the schedule fires (0–23). */
    private int hour;

    /** Minute of hour at which the schedule fires (0–59). */
    private int minute;

    /** JSON-serialized device state applied on execution. */
    private String actionPayload;

    /** Whether the schedule is currently active. */
    private boolean enabled;

    /**
     * Constructs a fully-populated {@code ScheduleResponse}.
     *
     * @param id            the schedule primary key
     * @param name          the schedule display name
     * @param deviceId      the target device primary key
     * @param deviceName    the target device name
     * @param roomName      the room name
     * @param daysOfWeek    the active days of week
     * @param hour          the fire hour (0–23)
     * @param minute        the fire minute (0–59)
     * @param actionPayload the JSON action payload
     * @param enabled       whether the schedule is active
     */
    public ScheduleResponse(Long id, String name, Long deviceId, String deviceName,
                            String roomName, List<String> daysOfWeek,
                            int hour, int minute, String actionPayload, boolean enabled) {
        this.id = id;
        this.name = name;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.roomName = roomName;
        this.daysOfWeek = daysOfWeek;
        this.hour = hour;
        this.minute = minute;
        this.actionPayload = actionPayload;
        this.enabled = enabled;
    }

    /**
     * Returns the schedule primary key.
     *
     * @return the id
     */
    public Long getId() { return id; }

    /**
     * Returns the schedule display name.
     *
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Returns the target device primary key.
     *
     * @return the device id
     */
    public Long getDeviceId() { return deviceId; }

    /**
     * Returns the target device display name.
     *
     * @return the device name
     */
    public String getDeviceName() { return deviceName; }

    /**
     * Returns the room name of the target device.
     *
     * @return the room name
     */
    public String getRoomName() { return roomName; }

    /**
     * Returns the days of week on which the schedule fires.
     *
     * @return list of uppercase day names
     */
    public List<String> getDaysOfWeek() { return daysOfWeek; }

    /**
     * Returns the hour of day at which the schedule fires.
     *
     * @return hour in range 0–23
     */
    public int getHour() { return hour; }

    /**
     * Returns the minute of hour at which the schedule fires.
     *
     * @return minute in range 0–59
     */
    public int getMinute() { return minute; }

    /**
     * Returns the JSON action payload applied on execution.
     *
     * @return the action payload JSON string
     */
    public String getActionPayload() { return actionPayload; }

    /**
     * Returns whether the schedule is currently active.
     *
     * @return {@code true} if the schedule fires at its configured time
     */
    public boolean isEnabled() { return enabled; }
}
