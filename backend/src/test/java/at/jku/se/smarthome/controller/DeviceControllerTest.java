package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.dto.DeviceResponse;
import at.jku.se.smarthome.dto.RenameDeviceRequest;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.DeviceService;
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
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeviceService deviceService;

    // Required so JwtAuthFilter can be instantiated (it checks isValid but finds no header in tests)
    @MockBean
    private JwtUtil jwtUtil;

    // Required by JwtAuthFilter and to satisfy JPA auto-configuration in the test slice
    @MockBean
    private UserRepository userRepository;

    // SmarthomeApplication.testDatabase bean requires JdbcTemplate, not loaded by @WebMvcTest
    @MockBean
    private JdbcTemplate jdbcTemplate;

    // --- GET /api/rooms/{roomId}/devices ---

    @Test
    @WithMockUser
    void getDevices_returns200WithList() throws Exception {
        DeviceResponse device = new DeviceResponse(1L, "Lamp", DeviceType.SWITCH);
        when(deviceService.getDevices(any(), eq(1L))).thenReturn(List.of(device));

        mockMvc.perform(get("/api/rooms/1/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lamp"))
                .andExpect(jsonPath("$[0].type").value("switch"));
    }

    @Test
    void getDevices_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/rooms/1/devices"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/rooms/{roomId}/devices ---

    @Test
    @WithMockUser
    void addDevice_returns201_withValidRequest() throws Exception {
        DeviceResponse created = new DeviceResponse(2L, "Thermostat", DeviceType.THERMOSTAT);
        when(deviceService.addDevice(any(), eq(1L), any())).thenReturn(created);

        String body = objectMapper.writeValueAsString(Map.of("name", "Thermostat", "type", "thermostat"));

        mockMvc.perform(post("/api/rooms/1/devices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Thermostat"))
                .andExpect(jsonPath("$.type").value("thermostat"));
    }

    @Test
    @WithMockUser
    void addDevice_returns400_whenNameIsBlank() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "", "type", "switch"));

        mockMvc.perform(post("/api/rooms/1/devices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void addDevice_returns400_whenTypeIsMissing() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Lamp"));

        mockMvc.perform(post("/api/rooms/1/devices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void addDevice_returns404_whenRoomNotFound() throws Exception {
        when(deviceService.addDevice(any(), eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));

        String body = objectMapper.writeValueAsString(Map.of("name", "Lamp", "type", "switch"));

        mockMvc.perform(post("/api/rooms/1/devices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void addDevice_returns409_whenNameConflict() throws Exception {
        when(deviceService.addDevice(any(), eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Device already exists."));

        String body = objectMapper.writeValueAsString(Map.of("name", "Lamp", "type", "switch"));

        mockMvc.perform(post("/api/rooms/1/devices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void addDevice_returns401_whenNotAuthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Lamp", "type", "switch"));

        mockMvc.perform(post("/api/rooms/1/devices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- PUT /api/rooms/{roomId}/devices/{deviceId} ---

    @Test
    @WithMockUser
    void renameDevice_returns200_withValidRequest() throws Exception {
        DeviceResponse updated = new DeviceResponse(5L, "Smart Lamp", DeviceType.SWITCH);
        when(deviceService.renameDevice(any(), eq(1L), eq(5L), any())).thenReturn(updated);

        String body = objectMapper.writeValueAsString(Map.of("name", "Smart Lamp"));

        mockMvc.perform(put("/api/rooms/1/devices/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Smart Lamp"));
    }

    @Test
    @WithMockUser
    void renameDevice_returns400_whenNameIsBlank() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(put("/api/rooms/1/devices/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void renameDevice_returns404_whenDeviceNotFound() throws Exception {
        when(deviceService.renameDevice(any(), eq(1L), eq(5L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));

        String body = objectMapper.writeValueAsString(Map.of("name", "Smart Lamp"));

        mockMvc.perform(put("/api/rooms/1/devices/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void renameDevice_returns409_whenNameConflict() throws Exception {
        when(deviceService.renameDevice(any(), eq(1L), eq(5L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Device already exists."));

        String body = objectMapper.writeValueAsString(Map.of("name", "Thermostat"));

        mockMvc.perform(put("/api/rooms/1/devices/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // --- DELETE /api/rooms/{roomId}/devices/{deviceId} ---

    @Test
    @WithMockUser
    void deleteDevice_returns204() throws Exception {
        mockMvc.perform(delete("/api/rooms/1/devices/5")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteDevice_returns404_whenDeviceNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."))
                .when(deviceService).deleteDevice(any(), eq(1L), eq(5L));

        mockMvc.perform(delete("/api/rooms/1/devices/5")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/rooms/{roomId}/devices/{deviceId}/state ---

    @Test
    @WithMockUser
    void updateState_returns200() throws Exception {
        DeviceResponse updated = new DeviceResponse(5L, "Lamp", DeviceType.SWITCH,
                true, 50, 21.0, 0.0, 0);
        when(deviceService.updateState(any(), eq(1L), eq(5L), any())).thenReturn(updated);

        String body = objectMapper.writeValueAsString(Map.of("stateOn", true));

        mockMvc.perform(patch("/api/rooms/1/devices/5/state")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateOn").value(true));
    }

    @Test
    @WithMockUser
    void updateState_returns404_whenDeviceNotFound() throws Exception {
        when(deviceService.updateState(any(), eq(1L), eq(5L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));

        String body = objectMapper.writeValueAsString(Map.of("stateOn", true));

        mockMvc.perform(patch("/api/rooms/1/devices/5/state")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
