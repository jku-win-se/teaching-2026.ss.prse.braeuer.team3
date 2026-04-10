package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.AuthResponse;
import at.jku.se.smarthome.dto.LoginRequest;
import at.jku.se.smarthome.dto.RegisterRequest;
import at.jku.se.smarthome.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints.
 *
 * <p>All endpoints under {@code /api/auth} are publicly accessible and
 * do not require a JWT token.</p>
 *
 * <p>Implements US-001 (register) and US-002 (login).</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructs an AuthController with the required service.
     *
     * @param authService the service handling authentication logic
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     *
     * <p>US-001: Registrierung mit gültiger E-Mail und Passwort möglich.
     * Returns 409 Conflict if the email is already taken.</p>
     *
     * @param request the registration data (name, email, password)
     * @return 201 Created with an {@link AuthResponse} containing the JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>US-002: Login mit korrekten Zugangsdaten erfolgreich.
     * Returns 401 Unauthorized for invalid credentials.</p>
     *
     * @param request the login credentials (email, password)
     * @return 200 OK with an {@link AuthResponse} containing the JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
