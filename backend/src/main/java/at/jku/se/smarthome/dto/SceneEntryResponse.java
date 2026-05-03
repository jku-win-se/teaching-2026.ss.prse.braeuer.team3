package at.jku.se.smarthome.dto;

/**
 * Response DTO for a single device-action pair within a scene.
 *
 * <p>Returned as part of {@link SceneResponse} for every scene read operation.</p>
 */
public class SceneEntryResponse {

    /** Primary key of the target device. */
    private Long deviceId;

    /** Display name of the target device. */
    private String deviceName;

    /**
     * Action applied to the device on scene activation.
     * Values: {@code "true"}, {@code "false"}, {@code "open"}, {@code "close"}.
     */
    private String actionValue;

    /**
     * Constructs a SceneEntryResponse with all fields.
     *
     * @param deviceId    the device primary key
     * @param deviceName  the device display name
     * @param actionValue the action string
     */
    public SceneEntryResponse(Long deviceId, String deviceName, String actionValue) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.actionValue = actionValue;
    }

    /**
     * Returns the device id.
     *
     * @return device primary key
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the device display name.
     *
     * @return device name
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Returns the action value string.
     *
     * @return action string
     */
    public String getActionValue() {
        return actionValue;
    }
}
