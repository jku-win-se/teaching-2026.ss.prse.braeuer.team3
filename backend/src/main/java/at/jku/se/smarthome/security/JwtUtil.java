package at.jku.se.smarthome.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility component for generating and validating JSON Web Tokens (JWT).
 *
 * <p>Tokens are signed with an HMAC-SHA key derived from the configured secret.
 * The token subject is the user's email address.</p>
 */
@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 86_400_000L; // 24 hours

    private final SecretKey signingKey;

    /**
     * Constructs a JwtUtil and initialises the signing key from the application configuration.
     *
     * @param secret the JWT secret from {@code app.jwt.secret} (must be at least 32 characters)
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT for the given email address with a 24-hour expiry.
     *
     * @param email the subject (user's email) to embed in the token
     * @return a signed JWT string
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the subject (email) from a valid JWT.
     *
     * @param token the JWT string to parse
     * @return the email address embedded in the token
     * @throws JwtException if the token is invalid, expired, or tampered with
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates the given JWT token.
     *
     * @param token the JWT string to validate
     * @return {@code true} if the token is valid and not expired, {@code false} otherwise
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
