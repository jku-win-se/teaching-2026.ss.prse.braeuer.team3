package at.jku.se.smarthome.dto;

/**
 * Request DTO for a single device-action pair within a scene create/update request.
 *
 * <p>Implements US-018: Szene mit mehreren Gerätezuständen erstellbar.</p>
 */
public class SceneEntryRequest {

    /** Primary key of the target device. */
    private Long deviceId;

    /**
     * Action to apply to the device on scene activation.
     * Allowed values: {@code "true"}, {@code "false"}, {@code "open"}, {@code "close"}.
     */
    private String actionValue;

    /** Default constructor for Jackson deserialization. */
    public SceneEntryRequest() {
    }

    /**
     * Returns the target device id.
     *
     * @return device primary key
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the target device id.
     *
     * @param deviceId device primary key
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns the action value string.
     *
     * @return action string
     */
    public String getActionValue() {
        return actionValue;
    }

    /**
     * Sets the action value string.
     *
     * @param actionValue action string
     */
    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
}
