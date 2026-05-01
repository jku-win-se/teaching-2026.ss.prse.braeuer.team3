package at.jku.se.smarthome.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for inviting a new member to the owner's home (FR-20).
 */
public class MemberInviteRequest {

    /** Email address of the user to invite. */
    @NotBlank
    @Email
    private String email;

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
}
