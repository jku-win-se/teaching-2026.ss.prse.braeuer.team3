package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.HomeMember;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.MemberInviteRequest;
import at.jku.se.smarthome.dto.MemberResponse;
import at.jku.se.smarthome.repository.HomeMemberRepository;
import at.jku.se.smarthome.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing home memberships in the SmartHome Orchestrator.
 *
 * <p>Provides invite, list, and remove operations for owners (FR-20),
 * and shared helpers used by all other services for role resolution (FR-13).</p>
 *
 * <p>Role model: a user is an OWNER if no {@link HomeMember} record exists with
 * that user as the {@code member}. Otherwise they are a MEMBER scoped to the
 * corresponding owner's home.</p>
 */
@Service
public class MemberService {

    private final HomeMemberRepository homeMemberRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a MemberService with the required repositories.
     *
     * @param homeMemberRepository the repository for home membership persistence
     * @param userRepository       the repository for user lookups
     */
    public MemberService(HomeMemberRepository homeMemberRepository, UserRepository userRepository) {
        this.homeMemberRepository = homeMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Invites a user to become a member of the caller's home (FR-20).
     *
     * <p>Validates that the caller is an owner, the invitee is registered,
     * the invitee is not the caller themselves, and the invitee is not already
     * a member of any home.</p>
     *
     * @param ownerEmail  the email of the authenticated owner
     * @param request     the invite request containing the invitee's email
     * @return the created membership as a {@link MemberResponse}
     * @throws ResponseStatusException 401 if the owner is not found
     * @throws ResponseStatusException 403 if the caller is a member (not an owner)
     * @throws ResponseStatusException 404 if the invitee email is not registered
     * @throws ResponseStatusException 400 if the owner tries to invite themselves
     * @throws ResponseStatusException 409 if the invitee is already a member of a home
     */
    @Transactional
    public MemberResponse inviteMember(String ownerEmail, MemberInviteRequest request) {
        User owner = resolveUser(ownerEmail);
        requireOwnerRole(owner);
        String inviteEmail = request.getEmail().toLowerCase(Locale.ROOT).strip();
        User invitee = userRepository.findByEmail(inviteEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found."));
        if (invitee.getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite yourself.");
        }
        if (homeMemberRepository.findByMember(invitee).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User is already a member of a home.");
        }
        HomeMember membership = homeMemberRepository.save(new HomeMember(owner, invitee));
        return toResponse(membership);
    }

    /**
     * Returns all members of the caller's home (FR-20).
     *
     * @param ownerEmail the email of the authenticated owner
     * @return list of members belonging to the owner's home
     * @throws ResponseStatusException 401 if the owner is not found
     * @throws ResponseStatusException 403 if the caller is a member (not an owner)
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(String ownerEmail) {
        User owner = resolveUser(ownerEmail);
        requireOwnerRole(owner);
        return homeMemberRepository.findByOwner(owner).stream()
                .map(MemberService::toResponse)
                .toList();
    }

    /**
     * Removes a member from the caller's home (FR-20).
     *
     * <p>After removal the ex-member's role reverts to OWNER on their next request.</p>
     *
     * @param ownerEmail the email of the authenticated owner
     * @param memberId   the primary key of the member user to remove
     * @throws ResponseStatusException 401 if the owner is not found
     * @throws ResponseStatusException 403 if the caller is a member (not an owner)
     * @throws ResponseStatusException 404 if no such member belongs to the caller's home
     */
    @Transactional
    public void removeMember(String ownerEmail, Long memberId) {
        User owner = resolveUser(ownerEmail);
        requireOwnerRole(owner);
        HomeMember membership = homeMemberRepository.findByOwnerAndMemberId(owner, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Member not found."));
        homeMemberRepository.delete(membership);
    }

    /**
     * Resolves the effective owner for the given caller email (FR-13).
     *
     * <p>Returns the caller if they are an owner, or the caller's owner
     * if they are a member. Used by all other services to scope DB queries
     * to the correct home context.</p>
     *
     * @param callerEmail the email of the authenticated caller
     * @return the user whose home data should be used for this request
     * @throws ResponseStatusException 401 if the caller is not found
     */
    @Transactional(readOnly = true)
    public User resolveEffectiveOwner(String callerEmail) {
        User caller = resolveUser(callerEmail);
        return homeMemberRepository.findByMember(caller)
                .map(HomeMember::getOwner)
                .orElse(caller);
    }

    /**
     * Throws 403 Forbidden if the caller is a member (not an owner) (FR-13).
     *
     * <p>Used as a guard at the start of all OWNER-only service operations.</p>
     *
     * @param callerEmail the email of the authenticated caller
     * @throws ResponseStatusException 401 if the caller is not found
     * @throws ResponseStatusException 403 if the caller is a member
     */
    @Transactional(readOnly = true)
    public void requireOwnerRole(String callerEmail) {
        User caller = resolveUser(callerEmail);
        requireOwnerRole(caller);
    }

    /**
     * Throws 403 Forbidden with an action-specific message if the caller is a member.
     *
     * @param callerEmail the email of the authenticated caller
     * @param action      the protected action, e.g. {@code "rename devices"}
     */
    @Transactional(readOnly = true)
    public void requireOwnerRole(String callerEmail, String action) {
        User caller = resolveUser(callerEmail);
        requireOwnerRole(caller, action);
    }

    /**
     * Returns the role string for the given user: {@code "OWNER"} or {@code "MEMBER"} (FR-13).
     *
     * <p>Used by {@link AuthService} to populate the role field in {@link at.jku.se.smarthome.dto.AuthResponse}.</p>
     *
     * @param user the user whose role to determine
     * @return {@code "OWNER"} if no membership exists, {@code "MEMBER"} otherwise
     */
    @Transactional(readOnly = true)
    public String resolveRole(User user) {
        return homeMemberRepository.findByMember(user).isPresent() ? "MEMBER" : "OWNER";
    }

    private void requireOwnerRole(User caller) {
        requireOwnerRole(caller, "manage this home");
    }

    private void requireOwnerRole(User caller, String action) {
        if (homeMemberRepository.findByMember(caller).isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the home owner can " + action
                            + ". Members can view rooms and control devices, but cannot change the home setup.");
        }
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found."));
    }

    private static MemberResponse toResponse(HomeMember m) {
        return new MemberResponse(
                m.getMember().getId(),
                m.getMember().getName(),
                m.getMember().getEmail(),
                m.getJoinedAt());
    }
}
