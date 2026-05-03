package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.security.JwtUtil;
import at.jku.se.smarthome.service.EnergyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnergyController.class)
class EnergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnergyService energyService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "alice@example.com")
    void getDeviceEnergy_returns200WithEnergyDevices() throws Exception {
        EnergyDeviceResponse device = new EnergyDeviceResponse(
                1L, "Ceiling Light", "Living Room", 12, 0.04, 0.29);
        when(energyService.getDeviceEnergy("alice@example.com")).thenReturn(List.of(device));

        mockMvc.perform(get("/api/energy/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value(1))
                .andExpect(jsonPath("$[0].deviceName").value("Ceiling Light"))
                .andExpect(jsonPath("$[0].room").value("Living Room"))
                .andExpect(jsonPath("$[0].wattage").value(12))
                .andExpect(jsonPath("$[0].todayKwh").value(0.04))
                .andExpect(jsonPath("$[0].weekKwh").value(0.29));
    }

    @Test
    void getDeviceEnergy_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/energy/devices"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/energy/export ---

    @Test
    @WithMockUser(username = "alice@example.com")
    void exportCsv_returns200WithCsvContentType() throws Exception {
        when(energyService.exportEnergyCsv(any()))
                .thenReturn("Device,Room,Wattage (W),Today (kWh),Week (kWh)\r\nCeiling Light,Living Room,12,0.04,0.29\r\n");

        mockMvc.perform(get("/api/energy/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("energy-summary.csv")))
                .andExpect(content().string(containsString("Device,Room,Wattage (W),Today (kWh),Week (kWh)")));
    }

    @Test
    void exportCsv_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/energy/export"))
                .andExpect(status().isUnauthorized());
    }
}
