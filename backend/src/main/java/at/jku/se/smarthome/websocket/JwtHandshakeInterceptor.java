package at.jku.se.smarthome.websocket;

import at.jku.se.smarthome.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor that authenticates the upgrade request via a JWT
 * supplied as a {@code ?token=} query parameter (FR-07).
 *
 * <p>The browser's native {@code WebSocket} constructor passes query parameters on the
 * initial HTTP upgrade request, which this interceptor validates before the connection
 * is established. Rejected handshakes return HTTP 401 and no WebSocket session is created.</p>
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Session attribute key under which the authenticated user's e-mail is stored. */
    static final String USER_EMAIL_ATTR = "userEmail";

    private final JwtUtil jwtUtil;

    /**
     * Constructs the interceptor with the JWT utility.
     *
     * @param jwtUtil the utility used to validate tokens and extract the user e-mail
     */
    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Validates the {@code ?token=} query parameter before the WebSocket handshake completes.
     *
     * <p>On success, the authenticated user's e-mail is stored in {@code attributes} under
     * {@link #USER_EMAIL_ATTR} so that {@link DeviceWebSocketHandler} can retrieve it.</p>
     *
     * @param request    the HTTP upgrade request
     * @param response   the HTTP upgrade response
     * @param wsHandler  the target WebSocket handler
     * @param attributes mutable session attribute map populated for the new session
     * @return {@code true} to proceed with the handshake; {@code false} to reject it
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = httpRequest.getParameter("token");
            if (token != null && jwtUtil.isValid(token)) {
                attributes.put(USER_EMAIL_ATTR, jwtUtil.extractEmail(token));
                return true;
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    /**
     * No post-handshake action required.
     *
     * @param request   the HTTP upgrade request
     * @param response  the HTTP upgrade response
     * @param wsHandler the target WebSocket handler
     * @param exception any exception raised during the handshake, or {@code null}
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no post-handshake action needed
    }
}
