package at.jku.se.smarthome.dto;

/**
 * Response DTO returned after a successful login or registration (US-001, US-002).
 *
 * <p>Contains the JWT token that the client must include in subsequent
 * requests as a Bearer token in the {@code Authorization} header.</p>
 */
public class AuthResponse {

    /** The JWT access token. */
    private String token;

    /** The authenticated user's display name. */
    private String name;

    /** The authenticated user's email address. */
    private String email;

    /**
     * Creates a new AuthResponse.
     *
     * @param token the JWT token
     * @param name  the user's display name
     * @param email the user's email address
     */
    public AuthResponse(String token, String name, String email) {
        this.token = token;
        this.name = name;
        this.email = email;
    }

    /**
     * Returns the JWT token.
     *
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the user's display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user's email address.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }
}
