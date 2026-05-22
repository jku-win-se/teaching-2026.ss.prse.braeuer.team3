package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.domain.VacationModeAction;
import at.jku.se.smarthome.dto.VacationModeRequest;
import at.jku.se.smarthome.dto.VacationModeResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.VacationModeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VacationModeController.class)
class VacationModeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VacationModeService vacationModeService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // --- GET /api/vacation-modes ---

    @Test
    @WithMockUser(username = "owner@test.com")
    void getVacationModes_returns200() throws Exception {
        when(vacationModeService.getVacationModes(anyString())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/vacation-modes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Summer Holiday"));
    }

    @Test
    void getVacationModes_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/vacation-modes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void getVacationModes_returns403_whenNotOwner() throws Exception {
        when(vacationModeService.getVacationModes(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Owner role required."));

        mockMvc.perform(get("/api/vacation-modes"))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/vacation-modes ---

    @Test
    @WithMockUser(username = "owner@test.com")
    void createVacationMode_returns201() throws Exception {
        VacationModeRequest req = buildRequest();
        when(vacationModeService.createVacationMode(anyString(), any(VacationModeRequest.class)))
                .thenReturn(buildResponse());

        mockMvc.perform(post("/api/vacation-modes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Summer Holiday"));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void createVacationMode_returns400_onValidationError() throws Exception {
        when(vacationModeService.createVacationMode(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate."));

        mockMvc.perform(post("/api/vacation-modes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVacationMode_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/vacation-modes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- PATCH /api/vacation-modes/{id}/deactivate ---

    @Test
    @WithMockUser(username = "owner@test.com")
    void deactivateVacationMode_returns200() throws Exception {
        VacationModeResponse resp = buildResponse();
        when(vacationModeService.deactivateVacationMode(anyString(), anyLong())).thenReturn(resp);

        mockMvc.perform(patch("/api/vacation-modes/1/deactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Summer Holiday"));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void deactivateVacationMode_returns404_whenNotFound() throws Exception {
        when(vacationModeService.deactivateVacationMode(anyString(), anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation mode not found."));

        mockMvc.perform(patch("/api/vacation-modes/99/deactivate").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/vacation-modes/{id} ---

    @Test
    @WithMockUser(username = "owner@test.com")
    void deleteVacationMode_returns204() throws Exception {
        doNothing().when(vacationModeService).deleteVacationMode(anyString(), anyLong());

        mockMvc.perform(delete("/api/vacation-modes/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void deleteVacationMode_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vacation mode not found."))
                .when(vacationModeService).deleteVacationMode(anyString(), anyLong());

        mockMvc.perform(delete("/api/vacation-modes/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteVacationMode_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/vacation-modes/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private VacationModeResponse buildResponse() {
        return new VacationModeResponse(
                1L, "Summer Holiday", 10L, "Evening Routine",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 21),
                false, true, VacationModeAction.ENABLE);
    }

    private VacationModeRequest buildRequest() {
        VacationModeRequest req = new VacationModeRequest();
        req.setName("Summer Holiday");
        req.setScheduleId(10L);
        req.setAction(VacationModeAction.ENABLE);
        req.setStartDate(LocalDate.of(2026, 7, 1));
        req.setEndDate(LocalDate.of(2026, 7, 21));
        return req;
    }
}
