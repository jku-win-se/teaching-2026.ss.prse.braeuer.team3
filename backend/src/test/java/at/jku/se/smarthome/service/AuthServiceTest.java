package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.AuthResponse;
import at.jku.se.smarthome.dto.LoginRequest;
import at.jku.se.smarthome.dto.RegisterRequest;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>Covers acceptance criteria for:</p>
 * <ul>
 *   <li>US-001: Registrierung mit gültiger E-Mail und Passwort möglich</li>
 *   <li>US-001: Doppelte E-Mail-Adressen werden abgelehnt</li>
 *   <li>US-001: Passwort wird mit bcrypt gehasht gespeichert</li>
 *   <li>US-002: Login mit korrekten Zugangsdaten erfolgreich</li>
 *   <li>US-002: Fehlermeldung bei falschen Zugangsdaten</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Alice");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("password123");

        savedUser = new User("Alice", "alice@example.com", "hashed-password");
    }

    // ── US-001: Register ──────────────────────────────────────────────────────

    @Test
    @DisplayName("US-001: Registrierung mit gültiger E-Mail und Passwort möglich")
    void register_withValidCredentials_returnsTokenAndUserInfo() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("US-001: Passwort wird mit bcrypt gehasht gespeichert")
    void register_passwordIsHashedWithBcrypt() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(anyString())).thenReturn("token");

        authService.register(registerRequest);

        // Verify that encode() was called — plain password must NEVER be stored
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("US-001: Doppelte E-Mail-Adressen werden abgelehnt (409 Conflict)")
    void register_withDuplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        // User must NOT be saved when email is duplicate
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-001: JWT-Token wird nach erfolgreicher Registrierung zurückgegeben")
    void register_returnsNonNullToken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString())).thenReturn("some.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isNotBlank();
    }

    // ── US-002: Login ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("US-002: Login mit korrekten Zugangsdaten erfolgreich")
    void login_withCorrectCredentials_returnsToken() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("US-002: Fehlermeldung bei falschem Passwort (401 Unauthorized)")
    void login_withWrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("US-002: Fehlermeldung bei unbekannter E-Mail (401 Unauthorized)")
    void login_withUnknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("US-002: Login gibt Name und E-Mail zurück")
    void login_returnsNameAndEmail() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString())).thenReturn("token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    // ── Bugfix #63: Email case-insensitivity ──────────────────────────────────

    @Test
    @DisplayName("Bugfix #63: Registrierung mit gemischter Groß-/Kleinschreibung speichert E-Mail in Kleinbuchstaben")
    void register_withMixedCaseEmail_normalizesToLowercase() {
        RegisterRequest mixedCaseRequest = new RegisterRequest();
        mixedCaseRequest.setName("Alice");
        mixedCaseRequest.setEmail("Alice@Example.COM");
        mixedCaseRequest.setPassword("password123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(mixedCaseRequest);

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).existsByEmail("alice@example.com");
    }

    @Test
    @DisplayName("Bugfix #63: Doppelte E-Mail mit anderer Groß-/Kleinschreibung wird als Duplikat abgelehnt (409)")
    void register_withDuplicateEmailDifferentCase_throwsConflict() {
        RegisterRequest upperCaseRequest = new RegisterRequest();
        upperCaseRequest.setName("Alice");
        upperCaseRequest.setEmail("ALICE@EXAMPLE.COM");
        upperCaseRequest.setPassword("password123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(upperCaseRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bugfix #63: Login mit anderer Groß-/Kleinschreibung als bei Registrierung erfolgreich")
    void login_withDifferentCaseThanRegistered_succeeds() {
        LoginRequest upperCaseLogin = new LoginRequest();
        upperCaseLogin.setEmail("ALICE@EXAMPLE.COM");
        upperCaseLogin.setPassword("password123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(upperCaseLogin);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).findByEmail("alice@example.com");
    }
}
