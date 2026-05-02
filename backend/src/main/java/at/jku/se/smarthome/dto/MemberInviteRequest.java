package at.jku.se.smarthome.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for inviting a new member to the owner's home (FR-20).
 */
public class MemberInviteRequest {

    /** Email address of the user to invite. */
    @NotBlank
    @Email
    private String email;

    /** Role to grant inside the home. Defaults to MEMBER for backward compatibility. */
    @Pattern(regexp = "OWNER|MEMBER", message = "Role must be OWNER or MEMBER.")
    private String role = "MEMBER";

    /** Default constructor for deserialization. */
    public MemberInviteRequest() {
    }

    /**
     * Returns the email address of the user to invite.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user to invite.
     *
     * @param email the email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the role to grant to the invited user.
     *
     * @return {@code "OWNER"} or {@code "MEMBER"}
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role to grant to the invited user.
     *
     * @param role the role
     */
    public void setRole(String role) {
        this.role = role;
    }
}
