package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.domain.TriggerOperator;
import at.jku.se.smarthome.domain.TriggerType;
import at.jku.se.smarthome.dto.RuleRequest;
import at.jku.se.smarthome.dto.RuleResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.RuleService;
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
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(RuleController.class)
class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RuleService ruleService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    // --- GET /api/rules ---

    @Test
    @WithMockUser(username = "user@test.com")
    void getRules_authenticated_returns200() throws Exception {
        when(ruleService.getRules(anyString(), any())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cool Down"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getRules_withDeviceIdFilter_passesDeviceIdToService() throws Exception {
        when(ruleService.getRules(anyString(), eq(10L))).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/rules").param("deviceId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cool Down"));
    }

    @Test
    void getRules_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/rules ---

    @Test
    @WithMockUser(username = "user@test.com")
    void createRule_validRequest_returns201() throws Exception {
        when(ruleService.createRule(anyString(), any(RuleRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cool Down"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createRule_deviceNotOwned_returns404() throws Exception {
        when(ruleService.createRule(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));

        mockMvc.perform(post("/api/rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/rules/{id} ---

    @Test
    @WithMockUser(username = "user@test.com")
    void updateRule_validRequest_returns200() throws Exception {
        when(ruleService.updateRule(anyString(), eq(1L), any(RuleRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/rules/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cool Down"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateRule_notFound_returns404() throws Exception {
        when(ruleService.updateRule(anyString(), anyLong(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found."));

        mockMvc.perform(put("/api/rules/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/rules/{id}/enabled ---

    @Test
    @WithMockUser(username = "user@test.com")
    void setEnabled_returns200() throws Exception {
        RuleResponse disabled = new RuleResponse(1L, "Cool Down", false, TriggerType.THRESHOLD,
                10L, "Sensor", TriggerOperator.GT, 25.0, null, null, null, 11L, "AC", "true");
        when(ruleService.setEnabled(anyString(), eq(1L), anyBoolean())).thenReturn(disabled);

        mockMvc.perform(patch("/api/rules/1/enabled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    // --- DELETE /api/rules/{id} ---

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteRule_returns204() throws Exception {
        doNothing().when(ruleService).deleteRule(anyString(), eq(1L));

        mockMvc.perform(delete("/api/rules/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteRule_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found."))
                .when(ruleService).deleteRule(anyString(), eq(99L));

        mockMvc.perform(delete("/api/rules/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/rules/conflicts ---

    @Test
    @WithMockUser(username = "user@test.com")
    void checkConflicts_withConflicts_returns200WithList() throws Exception {
        when(ruleService.checkConflicts(anyString(), eq(11L), eq("false"), isNull()))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/rules/conflicts")
                        .param("actionDeviceId", "11")
                        .param("actionValue", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cool Down"))
                .andExpect(jsonPath("$[0].actionValue").value("true"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void checkConflicts_noConflicts_returns200EmptyList() throws Exception {
        when(ruleService.checkConflicts(anyString(), eq(11L), eq("true"), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/rules/conflicts")
                        .param("actionDeviceId", "11")
                        .param("actionValue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void checkConflicts_withExcludeRuleId_passesExcludeToService() throws Exception {
        when(ruleService.checkConflicts(anyString(), eq(11L), eq("false"), eq(1L)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/rules/conflicts")
                        .param("actionDeviceId", "11")
                        .param("actionValue", "false")
                        .param("excludeRuleId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void checkConflicts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/rules/conflicts")
                        .param("actionDeviceId", "11")
                        .param("actionValue", "false"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private RuleRequest buildRequest() {
        RuleRequest req = new RuleRequest();
        req.setName("Cool Down");
        req.setTriggerType(TriggerType.THRESHOLD);
        req.setTriggerDeviceId(10L);
        req.setTriggerOperator(TriggerOperator.GT);
        req.setTriggerThresholdValue(25.0);
        req.setActionDeviceId(11L);
        req.setActionValue("true");
        req.setEnabled(true);
        return req;
    }

    private RuleResponse buildResponse() {
        return new RuleResponse(1L, "Cool Down", true, TriggerType.THRESHOLD,
                10L, "Sensor", TriggerOperator.GT, 25.0, null, null, null, 11L, "AC", "true");
    }

    private RuleResponse buildTimeResponse() {
        return new RuleResponse(2L, "Morning Lights", true, TriggerType.TIME,
                null, null, null, null, 7, 30, "MONDAY,FRIDAY", 11L, "AC", "true");
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createRule_timeRequest_returns201WithTimeFields() throws Exception {
        RuleRequest req = new RuleRequest();
        req.setName("Morning Lights");
        req.setTriggerType(TriggerType.TIME);
        req.setTriggerHour(7);
        req.setTriggerMinute(30);
        req.setTriggerDaysOfWeek("MONDAY,FRIDAY");
        req.setActionDeviceId(11L);
        req.setActionValue("true");

        when(ruleService.createRule(anyString(), any(RuleRequest.class))).thenReturn(buildTimeResponse());

        mockMvc.perform(post("/api/rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.triggerType").value("TIME"))
                .andExpect(jsonPath("$.triggerHour").value(7))
                .andExpect(jsonPath("$.triggerMinute").value(30))
                .andExpect(jsonPath("$.triggerDaysOfWeek").value("MONDAY,FRIDAY"));
    }
}
