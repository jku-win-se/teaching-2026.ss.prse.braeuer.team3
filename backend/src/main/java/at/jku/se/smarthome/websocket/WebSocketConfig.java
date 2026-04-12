package at.jku.se.smarthome.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the WebSocket endpoint for real-time device state updates (FR-07).
 *
 * <p>The endpoint {@code /ws/devices} accepts connections from the Angular frontend
 * ({@code http://localhost:4200}). Authentication is handled by {@link JwtHandshakeInterceptor}
 * before the WebSocket session is established.</p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeviceWebSocketHandler handler;
    private final JwtHandshakeInterceptor interceptor;

    /**
     * Constructs the configuration with the required WebSocket handler and interceptor.
     *
     * @param handler     the handler that manages device update broadcasts
     * @param interceptor the interceptor that authenticates the upgrade request
     */
    public WebSocketConfig(DeviceWebSocketHandler handler, JwtHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    /**
     * Registers the {@code /ws/devices} endpoint.
     *
     * @param registry the WebSocket handler registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/devices")
                .addInterceptors(interceptor)
                .setAllowedOrigins("http://localhost:4200");
    }
}
