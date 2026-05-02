package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.HomeMember;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.MemberInviteRequest;
import at.jku.se.smarthome.dto.MemberResponse;
import at.jku.se.smarthome.repository.HomeMemberRepository;
import at.jku.se.smarthome.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private HomeMemberRepository homeMemberRepository;
    @Mock private UserRepository userRepository;

    private MemberService memberService;
    private User owner;
    private User member;
    private User coOwner;
    private HomeMember membership;
    private HomeMember coOwnerMembership;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(homeMemberRepository, userRepository);
        owner = new User("Owner", "owner@test.com", "hash");
        ReflectionTestUtils.setField(owner, "id", 1L);
        member = new User("Member", "member@test.com", "hash");
        ReflectionTestUtils.setField(member, "id", 2L);
        coOwner = new User("Co Owner", "co-owner@test.com", "hash");
        ReflectionTestUtils.setField(coOwner, "id", 3L);
        membership = new HomeMember(owner, member);
        coOwnerMembership = new HomeMember(owner, coOwner, "OWNER");
    }

    @Test
    void inviteMember_createsMembershipForRegisteredUser() {
        MemberInviteRequest request = invite("MEMBER@Test.COM ");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.empty());
        when(homeMemberRepository.save(any(HomeMember.class))).thenReturn(membership);

        MemberResponse response = memberService.inviteMember("owner@test.com", request);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("member@test.com");
        assertThat(response.getRole()).isEqualTo("MEMBER");
        verify(homeMemberRepository).save(any(HomeMember.class));
    }

    @Test
    void inviteMember_canGrantOwnerRole() {
        MemberInviteRequest request = invite("member@test.com", "OWNER");
        HomeMember ownerInvite = new HomeMember(owner, member, "OWNER");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.empty());
        when(homeMemberRepository.save(any(HomeMember.class))).thenReturn(ownerInvite);

        MemberResponse response = memberService.inviteMember("owner@test.com", request);

        assertThat(response.getRole()).isEqualTo("OWNER");
    }

    @Test
    void inviteMember_allowsCoOwnerCaller() {
        MemberInviteRequest request = invite("member@test.com", "MEMBER");
        when(userRepository.findByEmail("co-owner@test.com")).thenReturn(Optional.of(coOwner));
        when(homeMemberRepository.findByMember(coOwner)).thenReturn(Optional.of(coOwnerMembership));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.empty());
        when(homeMemberRepository.save(any(HomeMember.class))).thenReturn(membership);

        MemberResponse response = memberService.inviteMember("co-owner@test.com", request);

        assertThat(response.getEmail()).isEqualTo("member@test.com");
    }

    @Test
    void inviteMember_rejectsMemberCaller() {
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> memberService.inviteMember("member@test.com", invite("other@test.com")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void inviteMember_rejectsSelfInvite() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.inviteMember("owner@test.com", invite("owner@test.com")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void inviteMember_rejectsAlreadyMemberUser() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> memberService.inviteMember("owner@test.com", invite("member@test.com")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void getMembers_returnsOwnerMembers() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());
        when(homeMemberRepository.findByOwner(owner)).thenReturn(List.of(membership));

        List<MemberResponse> result = memberService.getMembers("owner@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Member");
    }

    @Test
    void getMembers_allowsCoOwnerAndUsesPrimaryOwnerHome() {
        when(userRepository.findByEmail("co-owner@test.com")).thenReturn(Optional.of(coOwner));
        when(homeMemberRepository.findByMember(coOwner)).thenReturn(Optional.of(coOwnerMembership));
        when(homeMemberRepository.findByOwner(owner)).thenReturn(List.of(coOwnerMembership, membership));

        List<MemberResponse> result = memberService.getMembers("co-owner@test.com");

        assertThat(result).extracting(MemberResponse::getRole).containsExactly("OWNER", "MEMBER");
    }

    @Test
    void removeMember_deletesOwnedMembership() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());
        when(homeMemberRepository.findByOwnerAndMemberId(owner, 2L)).thenReturn(Optional.of(membership));

        memberService.removeMember("owner@test.com", 2L);

        verify(homeMemberRepository).delete(membership);
    }

    @Test
    void resolveEffectiveOwner_returnsMembershipOwnerForMember() {
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.of(membership));

        User result = memberService.resolveEffectiveOwner("member@test.com");

        assertThat(result).isSameAs(owner);
    }

    @Test
    void resolveRole_returnsOwnerOrMember() {
        when(homeMemberRepository.findByMember(owner)).thenReturn(Optional.empty());
        when(homeMemberRepository.findByMember(member)).thenReturn(Optional.of(membership));

        assertThat(memberService.resolveRole(owner)).isEqualTo("OWNER");
        assertThat(memberService.resolveRole(member)).isEqualTo("MEMBER");
    }

    @Test
    void resolveRole_returnsOwnerForOwnerMembership() {
        when(homeMemberRepository.findByMember(coOwner)).thenReturn(Optional.of(coOwnerMembership));

        assertThat(memberService.resolveRole(coOwner)).isEqualTo("OWNER");
    }

    private static MemberInviteRequest invite(String email) {
        return invite(email, null);
    }

    private static MemberInviteRequest invite(String email, String role) {
        MemberInviteRequest request = new MemberInviteRequest();
        request.setEmail(email);
        request.setRole(role);
        return request;
    }
}
