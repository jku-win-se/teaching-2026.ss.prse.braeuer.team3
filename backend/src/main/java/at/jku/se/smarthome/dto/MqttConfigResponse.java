package at.jku.se.smarthome.dto;

/**
 * Response DTO containing the current MQTT configuration and connection status (US-019).
 */
public class MqttConfigResponse {

    private String brokerUrl;
    private String topic;
    private boolean connected;

    /** Required by Jackson. */
    public MqttConfigResponse() {
    }

    /**
     * Creates a response with all fields set.
     *
     * @param brokerUrl the simulated broker URL
     * @param topic     the base MQTT topic
     * @param connected {@code true} if the simulated connection is active
     */
    public MqttConfigResponse(String brokerUrl, String topic, boolean connected) {
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.connected = connected;
    }

    /**
     * Returns the broker URL.
     *
     * @return the broker URL
     */
    public String getBrokerUrl() {
        return brokerUrl;
    }

    /**
     * Sets the broker URL.
     *
     * @param brokerUrl the broker URL
     */
    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    /**
     * Returns the base MQTT topic.
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
     * Returns whether the simulated connection is currently active.
     *
     * @return {@code true} if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Sets the connected flag.
     *
     * @param connected the connected flag
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
