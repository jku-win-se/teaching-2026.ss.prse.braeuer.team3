package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.RoomResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtAuthFilter;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.eq;
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

/**
 * MockMvc slice tests for {@link RoomController}.
 *
 * <p>Uses {@code @WithMockUser} to simulate an authenticated user.
 * Tests routing, HTTP status codes, and JSON serialization.</p>
 *
 * <p>Covers US-004: Raum erstellen, umbenennen, löschen.</p>
 */
@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    private RoomResponse roomResponse;

    @BeforeEach
    void setUp() throws Exception {
        roomResponse = new RoomResponse(1L, "Living Room", "weekend");

        Mockito.doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                    (ServletRequest) invocation.getArgument(0),
                    (ServletResponse) invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    // ── GET /api/rooms ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("GET /api/rooms → 200 OK mit Liste aller Räume")
    void getRooms_returnsListOfRooms() throws Exception {
        when(roomService.getRooms("alice@example.com"))
                .thenReturn(List.of(roomResponse));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Living Room"))
                .andExpect(jsonPath("$[0].icon").value("weekend"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("GET /api/rooms → 200 OK mit leerer Liste")
    void getRooms_withNoRooms_returnsEmptyList() throws Exception {
        when(roomService.getRooms("alice@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/rooms ohne Authentifizierung → 401 Unauthorized")
    void getRooms_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/rooms ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: POST /api/rooms → 201 Created")
    void createRoom_withValidData_returns201() throws Exception {
        when(roomService.createRoom(eq("alice@example.com"), any()))
                .thenReturn(roomResponse);

        String body = """
                { "name": "Living Room", "icon": "weekend" }
                """;

        mockMvc.perform(post("/api/rooms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Living Room"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: POST /api/rooms - Duplizierter Name → 409 Conflict")
    void createRoom_withDuplicateName_returns409() throws Exception {
        when(roomService.createRoom(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already exists"));

        String body = """
                { "name": "Living Room", "icon": "weekend" }
                """;

        mockMvc.perform(post("/api/rooms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: POST /api/rooms - Leerer Name → 400 Bad Request")
    void createRoom_withEmptyName_returns400() throws Exception {
        String body = """
                { "name": "", "icon": "weekend" }
                """;

        mockMvc.perform(post("/api/rooms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/rooms/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: PUT /api/rooms/1 → 200 OK mit aktualisiertem Raum")
    void renameRoom_withValidData_returns200() throws Exception {
        RoomResponse updated = new RoomResponse(1L, "Kitchen", "kitchen");
        when(roomService.renameRoom(eq("alice@example.com"), eq(1L), any()))
                .thenReturn(updated);

        String body = """
                { "name": "Kitchen", "icon": "kitchen" }
                """;

        mockMvc.perform(put("/api/rooms/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kitchen"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: PUT /api/rooms/99 - Nicht gefunden → 404")
    void renameRoom_whenNotFound_returns404() throws Exception {
        when(roomService.renameRoom(any(), eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        String body = """
                { "name": "Kitchen", "icon": "kitchen" }
                """;

        mockMvc.perform(put("/api/rooms/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/rooms/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: DELETE /api/rooms/1 → 204 No Content")
    void deleteRoom_withValidId_returns204() throws Exception {
        doNothing().when(roomService).deleteRoom("alice@example.com", 1L);

        mockMvc.perform(delete("/api/rooms/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("US-004: DELETE /api/rooms/99 - Nicht gefunden → 404")
    void deleteRoom_whenNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"))
                .when(roomService).deleteRoom("alice@example.com", 99L);

        mockMvc.perform(delete("/api/rooms/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/rooms/1 ohne Authentifizierung → 401 Unauthorized")
    void deleteRoom_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/rooms/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
