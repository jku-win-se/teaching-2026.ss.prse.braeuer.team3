package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.MemberInviteRequest;
import at.jku.se.smarthome.dto.MemberResponse;
import at.jku.se.smarthome.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing home memberships.
 *
 * <p>All endpoints are OWNER-only; members receive 403 Forbidden.
 * Implements FR-20: invite members by email and revoke access.</p>
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /**
     * Constructs a MemberController with the required service.
     *
     * @param memberService the service handling membership operations
     */
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Invites a user to the caller's home as a member (FR-20).
     *
     * @param principal the authenticated owner injected by Spring Security
     * @param request   the invite request containing the invitee's email
     * @return 201 Created with the new membership details
     */
    @PostMapping("/invite")
    public ResponseEntity<MemberResponse> invite(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberInviteRequest request) {
        MemberResponse response = memberService.inviteMember(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all members of the caller's home (FR-20).
     *
     * @param principal the authenticated owner injected by Spring Security
     * @return 200 OK with the list of members
     */
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(memberService.getMembers(principal.getUsername()));
    }

    /**
     * Removes a member from the caller's home (FR-20).
     *
     * @param principal the authenticated owner injected by Spring Security
     * @param memberId  the primary key of the member user to remove
     * @return 204 No Content on success
     */
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long memberId) {
        memberService.removeMember(principal.getUsername(), memberId);
        return ResponseEntity.noContent().build();
    }
}
