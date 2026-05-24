package at.jku.se.smarthome.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for saving MQTT broker configuration (US-019).
 *
 * <p>Both fields are required. The system never connects to the supplied
 * broker URL — the connection is fully simulated in-memory.</p>
 */
public class MqttConfigRequest {

    @NotBlank(message = "brokerUrl must not be blank")
    private String brokerUrl;

    @NotBlank(message = "topic must not be blank")
    private String topic;

    /** Required by Jackson. */
    public MqttConfigRequest() {
    }

    /**
     * Creates a request with broker URL and topic.
     *
     * @param brokerUrl the simulated broker URL
     * @param topic     the base MQTT topic
     */
    public MqttConfigRequest(String brokerUrl, String topic) {
        this.brokerUrl = brokerUrl;
        this.topic = topic;
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
     * @param brokerUrl the new broker URL
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
     * Sets the base MQTT topic.
     *
     * @param topic the new topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }
}
