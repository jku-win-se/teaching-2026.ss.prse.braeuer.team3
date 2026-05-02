package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.MemberInviteRequest;
import at.jku.se.smarthome.dto.MemberResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MemberService memberService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserRepository userRepository;
    @MockBean private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "owner@test.com")
    void invite_validRequest_returns201() throws Exception {
        when(memberService.inviteMember(anyString(), any(MemberInviteRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/members/invite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invite())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("member@test.com"));
    }

    @Test
    @WithMockUser(username = "member@test.com")
    void invite_memberCaller_returns403() throws Exception {
        when(memberService.inviteMember(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: Owner role required."));

        mockMvc.perform(post("/api/members/invite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invite())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void invite_invalidEmail_returns400() throws Exception {
        MemberInviteRequest request = new MemberInviteRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/members/invite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void getMembers_returns200() throws Exception {
        when(memberService.getMembers(anyString())).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Member"));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void removeMember_returns204() throws Exception {
        doNothing().when(memberService).removeMember(anyString(), eq(2L));

        mockMvc.perform(delete("/api/members/2").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void removeMember_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."))
                .when(memberService).removeMember(anyString(), eq(99L));

        mockMvc.perform(delete("/api/members/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMembers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isUnauthorized());
    }

    private static MemberInviteRequest invite() {
        MemberInviteRequest request = new MemberInviteRequest();
        request.setEmail("member@test.com");
        return request;
    }

    private static MemberResponse response() {
        return new MemberResponse(2L, "Member", "member@test.com", LocalDateTime.of(2026, 5, 1, 12, 0),
                "MEMBER");
    }
}
