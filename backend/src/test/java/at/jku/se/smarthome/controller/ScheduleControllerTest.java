package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.ScheduleRequest;
import at.jku.se.smarthome.dto.ScheduleResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    // --- GET /api/schedules ---

    @Test
    @WithMockUser(username = "user@test.com")
    void getSchedules_returns200() throws Exception {
        ScheduleResponse resp = buildResponse();
        when(scheduleService.getSchedules(anyString(), any())).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Morning"));
    }

    @Test
    void getSchedules_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/schedules ---

    @Test
    @WithMockUser(username = "user@test.com")
    void createSchedule_returns201() throws Exception {
        ScheduleRequest req = buildRequest();
        ScheduleResponse resp = buildResponse();
        when(scheduleService.createSchedule(anyString(), any(ScheduleRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Morning"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createSchedule_returns400_onValidationError() throws Exception {
        ScheduleRequest req = buildRequest();
        when(scheduleService.createSchedule(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request"));

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSchedule_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- PUT /api/schedules/{id} ---

    @Test
    @WithMockUser(username = "user@test.com")
    void updateSchedule_returns200() throws Exception {
        ScheduleRequest req = buildRequest();
        ScheduleResponse resp = buildResponse();
        when(scheduleService.updateSchedule(anyString(), eq(1L), any())).thenReturn(resp);

        mockMvc.perform(put("/api/schedules/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Morning"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateSchedule_returns404_whenNotFound() throws Exception {
        when(scheduleService.updateSchedule(anyString(), eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(put("/api/schedules/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/schedules/{id}/enabled ---

    @Test
    @WithMockUser(username = "user@test.com")
    void setEnabled_returns200() throws Exception {
        ScheduleResponse resp = buildResponse();
        when(scheduleService.setEnabled(anyString(), eq(1L), anyBoolean())).thenReturn(resp);

        mockMvc.perform(patch("/api/schedules/1/enabled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk());
    }

    // --- DELETE /api/schedules/{id} ---

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteSchedule_returns204() throws Exception {
        doNothing().when(scheduleService).deleteSchedule(anyString(), eq(1L));

        mockMvc.perform(delete("/api/schedules/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteSchedule_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"))
                .when(scheduleService).deleteSchedule(anyString(), eq(99L));

        mockMvc.perform(delete("/api/schedules/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSchedule_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/schedules/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private ScheduleResponse buildResponse() {
        return new ScheduleResponse(1L, "Morning", 10L, "Lamp", "Living Room",
                List.of("MONDAY", "FRIDAY"), 7, 30, "{\"stateOn\":true}", true);
    }

    private ScheduleRequest buildRequest() {
        ScheduleRequest req = new ScheduleRequest();
        req.setName("Morning");
        req.setDeviceId(10L);
        req.setDaysOfWeek(List.of("MONDAY", "FRIDAY"));
        req.setHour(7);
        req.setMinute(30);
        req.setActionPayload("{\"stateOn\":true}");
        req.setEnabled(true);
        return req;
    }
}
