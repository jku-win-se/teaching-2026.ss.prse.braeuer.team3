package at.jku.se.smarthome.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSocketConfig}.
 *
 * <p>Verifies that {@code registerWebSocketHandlers} wires up the handler, interceptor,
 * and allowed origin correctly without starting a Spring application context.</p>
 */
@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private DeviceWebSocketHandler handler;

    @Mock
    private JwtHandshakeInterceptor interceptor;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    private WebSocketConfig config;

    /**
     * Sets up the config and the fluent registration chain before each test.
     */
    @BeforeEach
    void setUp() {
        config = new WebSocketConfig(handler, interceptor);
        when(registry.addHandler(handler, "/ws/devices")).thenReturn(registration);
        when(registration.addInterceptors(interceptor)).thenReturn(registration);
        when(registration.setAllowedOrigins("http://localhost:4200")).thenReturn(registration);
    }

    /**
     * The handler must be registered at the {@code /ws/devices} path.
     */
    @Test
    void registerWebSocketHandlers_registersHandlerAtCorrectPath() {
        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/ws/devices");
    }

    /**
     * The JWT handshake interceptor must be added to the registration.
     */
    @Test
    void registerWebSocketHandlers_addsInterceptor() {
        config.registerWebSocketHandlers(registry);

        verify(registration).addInterceptors(interceptor);
    }

    /**
     * The allowed origin must be set to the Angular development server URL.
     */
    @Test
    void registerWebSocketHandlers_setsAllowedOrigin() {
        config.registerWebSocketHandlers(registry);

        verify(registration).setAllowedOrigins("http://localhost:4200");
    }
}
