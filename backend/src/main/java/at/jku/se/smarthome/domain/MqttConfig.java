package at.jku.se.smarthome.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Persisted MQTT broker configuration for a single user (US-019).
 *
 * <p>Each user has at most one {@code MqttConfig} row. The {@code connected}
 * flag records whether the simulated connection is currently active; the flag is
 * reset to {@code false} on application restart because the in-memory simulator
 * state is lost.</p>
 */
@Entity
@Table(name = "mqtt_configs")
public class MqttConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "broker_url", nullable = false)
    private String brokerUrl = "";

    @Column(name = "topic", nullable = false)
    private String topic = "smarthome";

    @Column(name = "connected", nullable = false)
    private boolean connected = false;

    /** Required by JPA. */
    protected MqttConfig() {
    }

    /**
     * Creates a new {@code MqttConfig} for the given user.
     *
     * @param user      the owner of this configuration
     * @param brokerUrl the simulated MQTT broker URL (e.g. {@code mqtt://localhost:1883})
     * @param topic     the base MQTT topic (e.g. {@code smarthome})
     */
    public MqttConfig(User user, String brokerUrl, String topic) {
        this.user = user;
        this.brokerUrl = brokerUrl;
        this.topic = topic;
    }

    /**
     * Returns the primary key.
     *
     * @return the config id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the user who owns this configuration.
     *
     * @return the owning {@link User}
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the simulated broker URL.
     *
     * @return the broker URL string
     */
    public String getBrokerUrl() {
        return brokerUrl;
    }

    /**
     * Sets the simulated broker URL.
     *
     * @param brokerUrl the new broker URL
     */
    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    /**
     * Returns the base MQTT topic.
     *
     * @return the topic string
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
     * @param connected {@code true} to mark as connected
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
