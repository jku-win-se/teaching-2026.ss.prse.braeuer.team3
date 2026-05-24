package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.MqttConfigRequest;
import at.jku.se.smarthome.dto.MqttConfigResponse;
import at.jku.se.smarthome.dto.MqttMessageDto;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.MqttSimulatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice tests for {@link MqttController} (US-019).
 *
 * <p>Verifies routing, HTTP status mapping, and JSON serialisation for all
 * MQTT integration endpoints.</p>
 */
@WebMvcTest(MqttController.class)
class MqttControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MqttSimulatorService mqttSimulatorService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    // ── GET /api/mqtt/config ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/mqtt/config returns 200 with config")
    void getConfig_returns200() throws Exception {
        when(mqttSimulatorService.getConfig(anyString()))
                .thenReturn(new MqttConfigResponse("mqtt://localhost:1883", "home", false));

        mockMvc.perform(get("/api/mqtt/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokerUrl").value("mqtt://localhost:1883"))
                .andExpect(jsonPath("$.topic").value("home"))
                .andExpect(jsonPath("$.connected").value(false));
    }

    // ── PUT /api/mqtt/config ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("PUT /api/mqtt/config saves config and returns 200")
    void saveConfig_returns200() throws Exception {
        MqttConfigRequest req = new MqttConfigRequest("mqtt://broker:1883", "smarthome");
        when(mqttSimulatorService.saveConfig(anyString(), any(MqttConfigRequest.class)))
                .thenReturn(new MqttConfigResponse("mqtt://broker:1883", "smarthome", false));

        mockMvc.perform(put("/api/mqtt/config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokerUrl").value("mqtt://broker:1883"))
                .andExpect(jsonPath("$.topic").value("smarthome"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/mqtt/config returns 400 when brokerUrl is blank")
    void saveConfig_returns400_whenBrokerUrlBlank() throws Exception {
        MqttConfigRequest req = new MqttConfigRequest("", "smarthome");

        mockMvc.perform(put("/api/mqtt/config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/mqtt/connect ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /api/mqtt/connect returns 200 with connected=true")
    void connect_returns200() throws Exception {
        when(mqttSimulatorService.connect(anyString()))
                .thenReturn(new MqttConfigResponse("mqtt://localhost:1883", "home", true));

        mockMvc.perform(post("/api/mqtt/connect").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/mqtt/connect returns 400 when broker not configured")
    void connect_returns400_whenNoBroker() throws Exception {
        when(mqttSimulatorService.connect(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please save a broker URL before connecting."));

        mockMvc.perform(post("/api/mqtt/connect").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/mqtt/disconnect ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /api/mqtt/disconnect returns 200 with connected=false")
    void disconnect_returns200() throws Exception {
        when(mqttSimulatorService.disconnect(anyString()))
                .thenReturn(new MqttConfigResponse("mqtt://localhost:1883", "home", false));

        mockMvc.perform(post("/api/mqtt/disconnect").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
    }

    // ── GET /api/mqtt/messages ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/mqtt/messages returns 200 with message list")
    void getMessages_returns200() throws Exception {
        MqttMessageDto msg = new MqttMessageDto("12:00:00", "PUBLISH", "home/devices/1", "{\"stateOn\":true}");
        when(mqttSimulatorService.getMessages(anyString())).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/mqtt/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].direction").value("PUBLISH"))
                .andExpect(jsonPath("$[0].topic").value("home/devices/1"))
                .andExpect(jsonPath("$[0].payload").value("{\"stateOn\":true}"));
    }

    // ── DELETE /api/mqtt/messages ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/mqtt/messages returns 204")
    void clearMessages_returns204() throws Exception {
        mockMvc.perform(delete("/api/mqtt/messages").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
