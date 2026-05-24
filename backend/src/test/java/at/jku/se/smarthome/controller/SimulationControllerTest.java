package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.SimulationEvent;
import at.jku.se.smarthome.dto.SimulationRequest;
import at.jku.se.smarthome.dto.SimulationResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.SimulationService;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice tests for {@link SimulationController}.
 *
 * <p>Verifies routing, HTTP status codes, and JSON serialization for the
 * day simulation endpoint (US-020).</p>
 */
@WebMvcTest(SimulationController.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimulationService simulationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    // ── POST /api/simulation/run ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner@test.com")
    @DisplayName("POST /api/simulation/run — returns 200 with event list")
    void runSimulation_validRequest_returns200() throws Exception {
        SimulationEvent event = new SimulationEvent(
                7, 0, 1L, "Hallway Light", "Hallway", "true", "Morning On", 10L);
        when(simulationService.run(anyString(), any())).thenReturn(new SimulationResponse(List.of(event)));

        SimulationRequest request = new SimulationRequest();
        request.setDayOfWeek("MONDAY");

        mockMvc.perform(post("/api/simulation/run")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].hour").value(7))
                .andExpect(jsonPath("$.events[0].minute").value(0))
                .andExpect(jsonPath("$.events[0].deviceName").value("Hallway Light"))
                .andExpect(jsonPath("$.events[0].actionValue").value("true"))
                .andExpect(jsonPath("$.events[0].ruleName").value("Morning On"));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    @DisplayName("POST /api/simulation/run — returns 200 with empty list when no rules fire")
    void runSimulation_noRulesFire_returnsEmptyList() throws Exception {
        when(simulationService.run(anyString(), any())).thenReturn(new SimulationResponse(List.of()));

        SimulationRequest request = new SimulationRequest();
        request.setDayOfWeek("SUNDAY");

        mockMvc.perform(post("/api/simulation/run")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    @DisplayName("POST /api/simulation/run — returns 400 when dayOfWeek is invalid")
    void runSimulation_invalidDayOfWeek_returns400() throws Exception {
        when(simulationService.run(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dayOfWeek"));

        SimulationRequest request = new SimulationRequest();
        request.setDayOfWeek("FUNDAY");

        mockMvc.perform(post("/api/simulation/run")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "member@test.com")
    @DisplayName("POST /api/simulation/run — returns 403 for member role")
    void runSimulation_memberRole_returns403() throws Exception {
        when(simulationService.run(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner only"));

        SimulationRequest request = new SimulationRequest();
        request.setDayOfWeek("MONDAY");

        mockMvc.perform(post("/api/simulation/run")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/simulation/run — returns 401 for unauthenticated request")
    void runSimulation_unauthenticated_returns401() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setDayOfWeek("MONDAY");

        mockMvc.perform(post("/api/simulation/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
