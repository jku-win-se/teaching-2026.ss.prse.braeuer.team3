package at.jku.se.smarthome.websocket;

import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.dto.RuleNotificationDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket handler that manages connected client sessions and broadcasts real-time
 * device state updates (FR-07).
 *
 * <p>Sessions are grouped per authenticated user e-mail, allowing a single user to have
 * multiple active connections (e.g. multiple browser tabs). The user e-mail is read from
 * the session attribute set by {@link JwtHandshakeInterceptor}.</p>
 *
 * <p>Thread safety: {@link ConcurrentHashMap} with {@link CopyOnWriteArrayList} values
 * ensures safe concurrent access. Individual {@code sendMessage} calls are synchronised
 * on the session object because the Tomcat WebSocket implementation is not thread-safe
 * for concurrent writes to the same session.</p>
 */
@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> sessionMap =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    /**
     * Constructs the handler with the Jackson object mapper used for JSON serialisation.
     *
     * @param objectMapper the mapper used to serialise {@link DeviceResponse} to JSON
     */
    public DeviceWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Registers a newly connected session under the authenticated user's e-mail.
     *
     * @param session the newly established WebSocket session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = getUserEmail(session);
        if (email != null) {
            sessionMap.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(session);
        }
    }

    /**
     * Removes a session from the registry when the connection is closed normally.
     *
     * @param session     the closed session
     * @param closeStatus the close status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        removeSession(session);
    }

    /**
     * Removes a session from the registry when a transport error occurs.
     *
     * @param session   the affected session
     * @param exception the transport error
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        removeSession(session);
    }

    /**
     * Broadcasts a device state update to all open sessions belonging to the given user.
     *
     * <p>If sending to a single session fails (NFR-05), that session is removed from the
     * registry and broadcasting continues to the remaining sessions.</p>
     *
     * @param userEmail      the e-mail of the user whose sessions should receive the update
     * @param deviceResponse the updated device state to broadcast
     */
    public void broadcast(String userEmail, DeviceResponse deviceResponse) {
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionMap.get(userEmail);
        if (sessions == null) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(deviceResponse);
        } catch (JsonProcessingException serializationException) {
            throw new IllegalStateException("Failed to serialise DeviceResponse for broadcast",
                    serializationException);
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException sendException) {
                removeSession(session);
            }
        }
    }

    /**
     * Broadcasts a new activity log entry to all open sessions belonging to the given user.
     *
     * <p>The message is sent as a JSON object with an additional {@code messageType}
     * field set to {@code "activityLog"} so the frontend can distinguish it from
     * device state update messages (FR-08).</p>
     *
     * <p>If sending to a single session fails, that session is removed from the
     * registry and broadcasting continues to the remaining sessions.</p>
     *
     * @param userEmail           the e-mail of the user whose sessions should receive the entry
     * @param activityLogResponse the log entry to broadcast
     */
    public void broadcastActivityLog(String userEmail, ActivityLogResponse activityLogResponse) {
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionMap.get(userEmail);
        if (sessions == null) {
            return;
        }
        String payload;
        try {
            ObjectNode node = objectMapper.valueToTree(activityLogResponse);
            node.put("messageType", "activityLog");
            payload = objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException serializationException) {
            throw new IllegalStateException("Failed to serialise ActivityLogResponse for broadcast",
                    serializationException);
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException sendException) {
                removeSession(session);
            }
        }
    }

    /**
     * Broadcasts a rule execution notification to all open sessions belonging to the given user.
     *
     * <p>The {@link RuleNotificationDto} is serialised directly to JSON; its
     * {@code messageType} field ({@code "ruleNotification"}) allows the frontend to
     * distinguish this message from device state updates and activity log entries (FR-US013-06).</p>
     *
     * <p>If sending to a single session fails, that session is removed from the
     * registry and broadcasting continues to the remaining sessions.</p>
     *
     * @param userEmail the e-mail of the user whose sessions should receive the notification
     * @param dto       the rule notification payload to broadcast
     */
    public void broadcastRuleNotification(String userEmail, RuleNotificationDto dto) {
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionMap.get(userEmail);
        if (sessions == null) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException serializationException) {
            throw new IllegalStateException("Failed to serialise RuleNotificationDto for broadcast",
                    serializationException);
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException sendException) {
                removeSession(session);
            }
        }
    }

    /**
     * Broadcasts a scene-list change notification to all open sessions belonging to the given user.
     *
     * <p>Sends {@code { "messageType": "sceneUpdate" }} so every open tab owned by the
     * same user can refresh its scene list in real time (US-018).</p>
     *
     * <p>If sending to a single session fails, that session is removed from the
     * registry and broadcasting continues to the remaining sessions.</p>
     *
     * @param userEmail the e-mail of the user whose sessions should receive the notification
     */
    public void broadcastSceneUpdate(String userEmail) {
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionMap.get(userEmail);
        if (sessions == null) {
            return;
        }
        String payload;
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("messageType", "sceneUpdate");
            payload = objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException serializationException) {
            throw new IllegalStateException("Failed to serialise scene update message for broadcast",
                    serializationException);
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException sendException) {
                removeSession(session);
            }
        }
    }

    /**
     * Removes the given session from the registry. If the user's session list becomes
     * empty the map entry is also removed.
     *
     * @param session the session to remove
     */
    void removeSession(WebSocketSession session) {
        String email = getUserEmail(session);
        if (email == null) {
            return;
        }
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionMap.get(email);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionMap.remove(email, sessions);
            }
        }
    }

    private String getUserEmail(WebSocketSession session) {
        return (String) session.getAttributes().get(JwtHandshakeInterceptor.USER_EMAIL_ATTR);
    }
}
