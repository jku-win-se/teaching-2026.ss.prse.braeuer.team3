package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.ActivityLogResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.ActivityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityLogController.class)
class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityLogService activityLogService;

    // Required so JwtAuthFilter can be instantiated
    @MockBean
    private JwtUtil jwtUtil;

    // Required by JwtAuthFilter
    @MockBean
    private UserRepository userRepository;

    // Required by SmarthomeApplication.testDatabase bean
    @MockBean
    private JdbcTemplate jdbcTemplate;

    // --- GET /api/activity-log ---

    @Test
    @WithMockUser
    void getActivityLog_returns200() throws Exception {
        ActivityLogResponse entry = new ActivityLogResponse(
                1L, Instant.parse("2026-04-24T10:00:00Z"),
                42L, "Lamp", "Living Room", "Test User", "Turned on");
        when(activityLogService.getLogs(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/activity-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("Turned on"))
                .andExpect(jsonPath("$.content[0].deviceName").value("Lamp"))
                .andExpect(jsonPath("$.content[0].roomName").value("Living Room"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getActivityLog_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/activity-log"))
                .andExpect(status().isUnauthorized());
    }

    // --- DELETE /api/activity-log/{id} ---

    @Test
    @WithMockUser
    void deleteActivityLog_returns204() throws Exception {
        doNothing().when(activityLogService).deleteLog(any(), eq(1L));

        mockMvc.perform(delete("/api/activity-log/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteActivityLog_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity log entry not found."))
                .when(activityLogService).deleteLog(any(), anyLong());

        mockMvc.perform(delete("/api/activity-log/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteActivityLog_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/activity-log/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
