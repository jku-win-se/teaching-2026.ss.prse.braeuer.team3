package at.jku.se.smarthome.websocket;

import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.dto.DeviceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeviceWebSocketHandler}.
 *
 * <p>{@link WebSocketSession} is an interface — Mockito uses a JDK dynamic proxy
 * (no byte-buddy instrumentation), so these tests run cleanly on Java 25.</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceWebSocketHandlerTest {

    @Mock
    private WebSocketSession session;

    @Mock
    private WebSocketSession session2;

    private DeviceWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeviceWebSocketHandler(new ObjectMapper());
    }

    // --- afterConnectionEstablished ---

    @Test
    void afterConnectionEstablished_storesSession() throws Exception {
        stubEmail(session, "user@test.com");

        handler.afterConnectionEstablished(session);

        assertThat(getSessionMap()).containsKey("user@test.com");
        assertThat(getSessionMap().get("user@test.com")).contains(session);
    }

    @Test
    void afterConnectionEstablished_noEmail_doesNotStore() throws Exception {
        when(session.getAttributes()).thenReturn(new HashMap<>());

        handler.afterConnectionEstablished(session);

        assertThat(getSessionMap()).isEmpty();
    }

    @Test
    void afterConnectionEstablished_multipleSessionsSameUser_allStored() throws Exception {
        stubEmail(session, "user@test.com");
        stubEmail(session2, "user@test.com");

        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(session2);

        assertThat(getSessionMap().get("user@test.com")).hasSize(2);
    }

    // --- afterConnectionClosed ---

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        stubEmail(session, "user@test.com");
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(getSessionMap()).doesNotContainKey("user@test.com");
    }

    // --- broadcast ---

    @Test
    void broadcast_noSessions_doesNotThrow() {
        assertThatCode(() -> handler.broadcast("nobody@test.com", buildResponse()))
                .doesNotThrowAnyException();
    }

    @Test
    void broadcast_singleOpenSession_sendsMessage() throws Exception {
        stubEmail(session, "user@test.com");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.broadcast("user@test.com", buildResponse());

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcast_multipleOpenSessions_allReceiveMessage() throws Exception {
        stubEmail(session, "user@test.com");
        stubEmail(session2, "user@test.com");
        when(session.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(session2);

        handler.broadcast("user@test.com", buildResponse());

        verify(session).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcast_closedSession_skippedAndRemoved() throws Exception {
        stubEmail(session, "user@test.com");
        when(session.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(session);

        handler.broadcast("user@test.com", buildResponse());

        verify(session, never()).sendMessage(any());
        assertThat(getSessionMap()).doesNotContainKey("user@test.com");
    }

    @Test
    void broadcast_failingSession_removedOtherSessionStillReceives() throws Exception {
        stubEmail(session, "user@test.com");
        stubEmail(session2, "user@test.com");
        when(session.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        doThrow(new IOException("simulated failure")).when(session).sendMessage(any());
        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(session2);

        handler.broadcast("user@test.com", buildResponse());

        // Failing session removed (NFR-05)
        assertThat(getSessionMap().get("user@test.com")).doesNotContain(session);
        // Healthy session still received the message
        verify(session2).sendMessage(any(TextMessage.class));
    }

    // --- removeSession ---

    @Test
    void removeSession_unknownEmail_doesNotThrow() {
        when(session.getAttributes()).thenReturn(new HashMap<>());
        assertThatCode(() -> handler.removeSession(session)).doesNotThrowAnyException();
    }

    // --- helpers ---

    private DeviceResponse buildResponse() {
        return new DeviceResponse(1L, "Lamp", DeviceType.SWITCH, true, 50, 21.0, 0.0, 0);
    }

    private void stubEmail(WebSocketSession s, String email) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(JwtHandshakeInterceptor.USER_EMAIL_ATTR, email);
        when(s.getAttributes()).thenReturn(attrs);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> getSessionMap()
            throws Exception {
        Field field = DeviceWebSocketHandler.class.getDeclaredField("sessionMap");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>>) field.get(handler);
    }
}
