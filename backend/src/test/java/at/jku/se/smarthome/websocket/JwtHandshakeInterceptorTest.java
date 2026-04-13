package at.jku.se.smarthome.websocket;

import at.jku.se.smarthome.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtHandshakeInterceptor}.
 *
 * <p>Covers all branching paths in {@code beforeHandshake}: valid token, invalid token,
 * missing token, and a non-servlet request. Also covers the no-op {@code afterHandshake}.</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ServletServerHttpRequest servletRequest;

    @Mock
    private ServerHttpRequest plainRequest;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    @Mock
    private HttpServletRequest httpRequest;

    private JwtHandshakeInterceptor interceptor;

    /**
     * Creates a fresh interceptor before each test.
     */
    @BeforeEach
    void setUp() {
        interceptor = new JwtHandshakeInterceptor(jwtUtil);
    }

    /**
     * A valid JWT token should allow the handshake and store the user email in attributes.
     */
    @Test
    void beforeHandshake_validToken_returnsTrue() {
        Map<String, Object> attributes = new HashMap<>();
        when(servletRequest.getServletRequest()).thenReturn(httpRequest);
        when(httpRequest.getParameter("token")).thenReturn("valid.jwt.token");
        when(jwtUtil.isValid("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.jwt.token")).thenReturn("user@example.com");

        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(JwtHandshakeInterceptor.USER_EMAIL_ATTR, "user@example.com");
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    /**
     * An invalid JWT token should reject the handshake with HTTP 401.
     */
    @Test
    void beforeHandshake_invalidToken_returnsFalse() {
        Map<String, Object> attributes = new HashMap<>();
        when(servletRequest.getServletRequest()).thenReturn(httpRequest);
        when(httpRequest.getParameter("token")).thenReturn("bad.token");
        when(jwtUtil.isValid("bad.token")).thenReturn(false);

        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey(JwtHandshakeInterceptor.USER_EMAIL_ATTR);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    /**
     * A missing (null) token should reject the handshake with HTTP 401.
     */
    @Test
    void beforeHandshake_nullToken_returnsFalse() {
        Map<String, Object> attributes = new HashMap<>();
        when(servletRequest.getServletRequest()).thenReturn(httpRequest);
        when(httpRequest.getParameter("token")).thenReturn(null);

        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey(JwtHandshakeInterceptor.USER_EMAIL_ATTR);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    /**
     * A non-servlet request (not a {@link ServletServerHttpRequest}) should be rejected with HTTP 401.
     */
    @Test
    void beforeHandshake_notServletRequest_returnsFalse() {
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(plainRequest, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey(JwtHandshakeInterceptor.USER_EMAIL_ATTR);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    /**
     * The {@code afterHandshake} callback is a no-op and must not throw any exception.
     */
    @Test
    void afterHandshake_doesNotThrow() {
        assertThatCode(() ->
                interceptor.afterHandshake(servletRequest, response, wsHandler, null)
        ).doesNotThrowAnyException();
    }
}
