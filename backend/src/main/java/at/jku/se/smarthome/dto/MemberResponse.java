package at.jku.se.smarthome.dto;

import java.time.LocalDateTime;

/**
 * Response DTO representing a home member (FR-20).
 *
 * <p>Returned by the member management endpoints to convey the member's
 * identity and the timestamp when they joined the home.</p>
 */
public class MemberResponse {

    /** Primary key of the member user. */
    private Long id;

    /** Display name of the member. */
    private String name;

    /** Email address of the member. */
    private String email;

    /** Timestamp when the membership was created. */
    private LocalDateTime joinedAt;

    /** Role granted to the user inside the home. */
    private String role;

    /**
     * Creates a new MemberResponse.
     *
     * @param id       the member user's primary key
     * @param name     the member's display name
     * @param email    the member's email address
     * @param joinedAt the timestamp when the membership was created
     * @param role     the user's role in the home
     */
    public MemberResponse(Long id, String name, String email, LocalDateTime joinedAt, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.joinedAt = joinedAt;
        this.role = role;
    }

    /**
     * Returns the primary key of the member user.
     *
     * @return the member id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the display name of the member.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the email address of the member.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the timestamp when this membership was created.
     *
     * @return the joined-at timestamp
     */
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    /**
     * Returns the user's role inside the home.
     *
     * @return {@code "OWNER"} or {@code "MEMBER"}
     */
    public String getRole() {
        return role;
    }
}
