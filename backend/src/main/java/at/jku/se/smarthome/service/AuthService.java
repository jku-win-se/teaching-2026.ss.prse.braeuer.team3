package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.AuthResponse;
import at.jku.se.smarthome.dto.LoginRequest;
import at.jku.se.smarthome.dto.RegisterRequest;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling user registration and authentication.
 *
 * <p>Implements the acceptance criteria for:</p>
 * <ul>
 *   <li>US-001: Registrierung mit gültiger E-Mail und Passwort möglich</li>
 *   <li>US-001: Doppelte E-Mail-Adressen werden abgelehnt</li>
 *   <li>US-001: Passwort wird mit bcrypt gehasht gespeichert</li>
 *   <li>US-002: Login mit korrekten Zugangsdaten erfolgreich</li>
 *   <li>US-002: Fehlermeldung bei falschen Zugangsdaten</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Constructs an AuthService with required dependencies.
     *
     * @param userRepository  the repository for user persistence
     * @param passwordEncoder the BCrypt password encoder
     * @param jwtUtil         the JWT utility for token generation
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user account.
     *
     * <p>US-001: Validates that the email is unique and hashes the password
     * using BCrypt before persisting the user.</p>
     *
     * @param request the registration data (name, email, password)
     * @return an {@link AuthResponse} containing the JWT token and user details
     * @throws ResponseStatusException with status 409 if the email is already in use
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email address already exists.");
        }
        String hash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), request.getEmail(), hash);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail());
    }

    /**
     * Authenticates a user with their email and password.
     *
     * <p>US-002: Returns a JWT token on success, or throws an exception
     * when the credentials are invalid.</p>
     *
     * @param request the login credentials (email, password)
     * @return an {@link AuthResponse} containing the JWT token and user details
     * @throws ResponseStatusException with status 401 if the credentials are invalid
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}
