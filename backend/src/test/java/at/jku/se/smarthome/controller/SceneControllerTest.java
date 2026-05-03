package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.SceneEntryRequest;
import at.jku.se.smarthome.dto.SceneEntryResponse;
import at.jku.se.smarthome.dto.SceneRequest;
import at.jku.se.smarthome.dto.SceneResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.SceneService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SceneController.class)
class SceneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SceneService sceneService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    // --- GET /api/scenes ---

    @Test
    @WithMockUser(username = "user@test.com")
    void getScenes_authenticated_returns200() throws Exception {
        when(sceneService.getScenes(anyString())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/scenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Movie Night"))
                .andExpect(jsonPath("$[0].icon").value("movie"))
                .andExpect(jsonPath("$[0].entries[0].deviceName").value("TV"));
    }

    @Test
    void getScenes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/scenes"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/scenes ---

    @Test
    @WithMockUser(username = "user@test.com")
    void createScene_validRequest_returns201() throws Exception {
        when(sceneService.createScene(anyString(), any(SceneRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/scenes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Movie Night"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createScene_deviceNotOwned_returns404() throws Exception {
        when(sceneService.createScene(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));

        mockMvc.perform(post("/api/scenes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createScene_missingEntries_returns400() throws Exception {
        when(sceneService.createScene(anyString(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A scene must contain at least one device entry."));

        mockMvc.perform(post("/api/scenes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/scenes/{id} ---

    @Test
    @WithMockUser(username = "user@test.com")
    void updateScene_validRequest_returns200() throws Exception {
        when(sceneService.updateScene(anyString(), anyLong(), any(SceneRequest.class)))
                .thenReturn(buildResponse());

        mockMvc.perform(put("/api/scenes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateScene_notFound_returns404() throws Exception {
        when(sceneService.updateScene(anyString(), anyLong(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found."));

        mockMvc.perform(put("/api/scenes/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/scenes/{id} ---

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteScene_existing_returns204() throws Exception {
        doNothing().when(sceneService).deleteScene(anyString(), anyLong());

        mockMvc.perform(delete("/api/scenes/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteScene_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found."))
                .when(sceneService).deleteScene(anyString(), anyLong());

        mockMvc.perform(delete("/api/scenes/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/scenes/{id}/activate ---

    @Test
    @WithMockUser(username = "user@test.com")
    void activateScene_existing_returns204() throws Exception {
        doNothing().when(sceneService).activateScene(anyString(), anyLong());

        mockMvc.perform(post("/api/scenes/1/activate").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void activateScene_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found."))
                .when(sceneService).activateScene(anyString(), anyLong());

        mockMvc.perform(post("/api/scenes/99/activate").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateScene_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/scenes/1/activate").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static SceneResponse buildResponse() {
        List<SceneEntryResponse> entries = List.of(
                new SceneEntryResponse(10L, "TV", "true")
        );
        return new SceneResponse(1L, "Movie Night", "movie", entries);
    }

    private static SceneRequest buildRequest() {
        SceneEntryRequest entry = new SceneEntryRequest();
        entry.setDeviceId(10L);
        entry.setActionValue("true");

        SceneRequest req = new SceneRequest();
        req.setName("Movie Night");
        req.setIcon("movie");
        req.setEntries(List.of(entry));
        return req;
    }
}
