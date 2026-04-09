package at.jku.se.smarthome.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the login endpoint (US-002).
 */
public class LoginRequest {

    /** The user's email address. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** The user's plain-text password. */
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Returns the email address.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the plain-text password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the plain-text password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
