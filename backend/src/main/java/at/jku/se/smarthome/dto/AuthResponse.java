package at.jku.se.smarthome.dto;

/**
 * Response DTO returned after a successful login or registration (US-001, US-002).
 *
 * <p>Contains the JWT token that the client must include in subsequent
 * requests as a Bearer token in the {@code Authorization} header.</p>
 *
 * <p>FR-13: The {@code role} field indicates whether the authenticated user
 * is an {@code "OWNER"} or a {@code "MEMBER"}, allowing the frontend to
 * show or hide management controls accordingly.</p>
 */
public class AuthResponse {

    /** The JWT access token. */
    private final String token;

    /** The authenticated user's display name. */
    private final String name;

    /** The authenticated user's email address. */
    private final String email;

    /**
     * The user's current role: {@code "OWNER"} or {@code "MEMBER"} (FR-13).
     * Derived from the {@code home_members} table at login time.
     */
    private final String role;

    /**
     * Creates a new AuthResponse.
     *
     * @param token the JWT token
     * @param name  the user's display name
     * @param email the user's email address
     * @param role  the user's role ({@code "OWNER"} or {@code "MEMBER"})
     */
    public AuthResponse(String token, String name, String email, String role) {
        this.token = token;
        this.name = name;
        this.email = email;
        this.role = role;
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

    /**
     * Returns the user's role ({@code "OWNER"} or {@code "MEMBER"}).
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }
}
