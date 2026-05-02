package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.AuthResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.security.SecurityConfig;
import at.jku.se.smarthome.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice tests for {@link AuthController}.
 *
 * <p>Tests the HTTP layer (routing, status codes, request/response bodies)
 * without starting a full application context. The service layer is mocked.</p>
 *
 * <p>Covers US-001 (register) and US-002 (login) from the HTTP perspective.</p>
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    @DisplayName("US-001: POST /api/auth/register → 201 Created mit Token")
    void register_withValidData_returns201() throws Exception {
        AuthResponse authResponse = new AuthResponse("jwt-token", "Alice", "alice@example.com", "OWNER");
        when(authService.register(any())).thenReturn(authResponse);

        String body = """
                {
                  "name": "Alice",
                  "email": "alice@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("US-001: POST /api/auth/register - Doppelte E-Mail → 409 Conflict")
    void register_withDuplicateEmail_returns409() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already exists"));

        String body = """
                {
                  "name": "Alice",
                  "email": "alice@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("US-001: POST /api/auth/register - Fehlende Felder → 400 Bad Request")
    void register_withMissingFields_returns400() throws Exception {
        String body = """
                {
                  "email": "alice@example.com"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-001: POST /api/auth/register - Ungültige E-Mail → 400 Bad Request")
    void register_withInvalidEmail_returns400() throws Exception {
        String body = """
                {
                  "name": "Alice",
                  "email": "not-an-email",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-001: POST /api/auth/register - Passwort zu kurz → 400 Bad Request")
    void register_withShortPassword_returns400() throws Exception {
        String body = """
                {
                  "name": "Alice",
                  "email": "alice@example.com",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    @DisplayName("US-002: POST /api/auth/login - Korrekte Zugangsdaten → 200 OK mit Token")
    void login_withCorrectCredentials_returns200() throws Exception {
        AuthResponse authResponse = new AuthResponse("jwt-token", "Alice", "alice@example.com", "OWNER");
        when(authService.login(any())).thenReturn(authResponse);

        String body = """
                {
                  "email": "alice@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("US-002: POST /api/auth/login - Falsche Zugangsdaten → 401 Unauthorized")
    void login_withWrongCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        String body = """
                {
                  "email": "alice@example.com",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("US-002: POST /api/auth/login - Fehlende Felder → 400 Bad Request")
    void login_withMissingFields_returns400() throws Exception {
        String body = """
                {
                  "email": "alice@example.com"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
