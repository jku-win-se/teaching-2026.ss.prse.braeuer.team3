package at.jku.se.smarthome.dto;

/**
 * Payload for real-time rule execution notifications sent over the WebSocket channel.
 *
 * <p>Instances are serialised to JSON and broadcast to the owning user's WebSocket
 * sessions via {@link at.jku.se.smarthome.websocket.DeviceWebSocketHandler}.
 * The {@code messageType} field is fixed to {@code "ruleNotification"} so the
 * frontend {@code RealtimeService} can route the message to the correct handler.</p>
 *
 * <p>Implements FR-US013-01 (success) and FR-US013-02 (failure).</p>
 */
public class RuleNotificationDto {

    private final String messageType = "ruleNotification";
    private final String ruleName;
    private final boolean success;
    private final String message;

    /**
     * Constructs a rule notification.
     *
     * @param ruleName the name of the rule that fired or failed
     * @param success  {@code true} if the rule executed successfully, {@code false} otherwise
     * @param message  human-readable action description (success) or error text (failure)
     */
    public RuleNotificationDto(String ruleName, boolean success, String message) {
        this.ruleName = ruleName;
        this.success = success;
        this.message = message;
    }

    /**
     * Returns the WebSocket message type discriminator.
     *
     * @return always {@code "ruleNotification"}
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Returns the name of the rule that fired or failed.
     *
     * @return rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Returns whether the rule executed successfully.
     *
     * @return {@code true} for success, {@code false} for failure
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns a human-readable description of the outcome.
     *
     * @return action description on success, or user-friendly error text on failure
     */
    public String getMessage() {
        return message;
    }
}
