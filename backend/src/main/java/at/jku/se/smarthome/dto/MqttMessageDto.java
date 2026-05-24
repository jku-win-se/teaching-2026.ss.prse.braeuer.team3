package at.jku.se.smarthome.dto;

/**
 * Represents a single simulated MQTT message in the in-memory message log (US-019).
 *
 * <p>Messages are never written to the database — they exist only in the
 * {@link at.jku.se.smarthome.service.MqttSimulatorService} in-memory queue.</p>
 */
public class MqttMessageDto {

    private String timestamp;
    private String direction;
    private String topic;
    private String payload;

    /** Required by Jackson. */
    public MqttMessageDto() {
    }

    /**
     * Creates a new simulated MQTT message.
     *
     * @param timestamp ISO-8601 timestamp string of the simulated event
     * @param direction {@code "PUBLISH"} or {@code "RECEIVE"}
     * @param topic     the full MQTT topic used in the message
     * @param payload   the message payload (JSON string)
     */
    public MqttMessageDto(String timestamp, String direction, String topic, String payload) {
        this.timestamp = timestamp;
        this.direction = direction;
        this.topic = topic;
        this.payload = payload;
    }

    /**
     * Returns the timestamp.
     *
     * @return the timestamp string
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp the timestamp string
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the message direction.
     *
     * @return {@code "PUBLISH"} or {@code "RECEIVE"}
     */
    public String getDirection() {
        return direction;
    }

    /**
     * Sets the direction.
     *
     * @param direction the direction
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * Returns the full MQTT topic.
     *
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the topic.
     *
     * @param topic the topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Returns the message payload.
     *
     * @return the payload JSON string
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the payload.
     *
     * @param payload the payload
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }
}
