package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.AuthResponse;
import at.jku.se.smarthome.dto.LoginRequest;
import at.jku.se.smarthome.dto.RegisterRequest;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import java.util.Locale;
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
 *   <li>FR-13: Rolle (OWNER/MEMBER) wird in der AuthResponse zurückgegeben</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MemberService memberService;

    /**
     * Constructs an AuthService with required dependencies.
     *
     * @param userRepository  the repository for user persistence
     * @param passwordEncoder the BCrypt password encoder
     * @param jwtUtil         the JWT utility for token generation
     * @param memberService   the service used to resolve the user's role (FR-13)
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       MemberService memberService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.memberService = memberService;
    }

    /**
     * Registers a new user account.
     *
     * <p>US-001: Validates that the email is unique and hashes the password
     * using BCrypt before persisting the user. The email is normalized to
     * lowercase before storage to ensure case-insensitive uniqueness.
     * New users always receive the OWNER role (FR-13).</p>
     *
     * @param request the registration data (name, email, password)
     * @return an {@link AuthResponse} containing the JWT token, user details, and role
     * @throws ResponseStatusException with status 409 if the email is already in use
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase(Locale.ROOT).strip();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email address already exists.");
        }
        String hash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), email, hash);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), "OWNER");
    }

    /**
     * Authenticates a user with their email and password.
     *
     * <p>US-002: Returns a JWT token on success, or throws an exception
     * when the credentials are invalid. The email is normalized to lowercase
     * before lookup to support case-insensitive login.
     * FR-13: The current role (OWNER/MEMBER) is resolved from the database
     * and included in the response.</p>
     *
     * @param request the login credentials (email, password)
     * @return an {@link AuthResponse} containing the JWT token, user details, and role
     * @throws ResponseStatusException with status 401 if the credentials are invalid
     */
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase(Locale.ROOT).strip();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        String role = memberService.resolveRole(user);
        return new AuthResponse(token, user.getName(), user.getEmail(), role);
    }
}
