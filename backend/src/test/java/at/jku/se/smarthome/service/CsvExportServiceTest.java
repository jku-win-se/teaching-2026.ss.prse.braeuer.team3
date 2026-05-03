package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.ActivityLog;
import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private CsvExportService service;

    @BeforeEach
    void setUp() {
        service = new CsvExportService();
    }

    // -------------------------------------------------------------------------
    // buildActivityLogCsv
    // -------------------------------------------------------------------------

    @Test
    void buildActivityLogCsv_emptyList_returnsHeaderOnly() {
        String csv = service.buildActivityLogCsv(List.of());
        assertThat(csv).isEqualTo(CsvExportService.ACTIVITY_LOG_HEADER + "\r\n");
    }

    @Test
    void buildActivityLogCsv_singleEntry_containsHeaderAndRow() {
        ActivityLog entry = makeActivityLog("Lamp", "Living Room", "Alice", "Turned on",
                Instant.parse("2026-05-01T08:00:00Z"));
        String csv = service.buildActivityLogCsv(List.of(entry));

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo(CsvExportService.ACTIVITY_LOG_HEADER);
        assertThat(lines[1]).contains("2026-05-01T08:00:00Z");
        assertThat(lines[1]).contains("Lamp");
        assertThat(lines[1]).contains("Living Room");
        assertThat(lines[1]).contains("Alice");
        assertThat(lines[1]).contains("Turned on");
    }

    @Test
    void buildActivityLogCsv_multipleEntries_hasCorrectRowCount() {
        ActivityLog e1 = makeActivityLog("Lamp", "Living Room", "Alice", "Turned on",
                Instant.parse("2026-05-01T08:00:00Z"));
        ActivityLog e2 = makeActivityLog("Thermostat", "Bedroom", "Rule", "Temperature set to 22°C",
                Instant.parse("2026-05-01T09:00:00Z"));
        String csv = service.buildActivityLogCsv(List.of(e1, e2));

        long lineCount = csv.lines().count();
        assertThat(lineCount).isEqualTo(3); // header + 2 data rows
    }

    // -------------------------------------------------------------------------
    // buildEnergyCsv
    // -------------------------------------------------------------------------

    @Test
    void buildEnergyCsv_emptyList_returnsHeaderOnly() {
        String csv = service.buildEnergyCsv(List.of());
        assertThat(csv).isEqualTo(CsvExportService.ENERGY_HEADER + "\r\n");
    }

    @Test
    void buildEnergyCsv_singleDevice_containsHeaderAndRow() {
        EnergyDeviceResponse device = new EnergyDeviceResponse(1L, "Lamp", "Living Room", 60, 0.36, 2.52);
        String csv = service.buildEnergyCsv(List.of(device));

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo(CsvExportService.ENERGY_HEADER);
        assertThat(lines[1]).contains("Lamp");
        assertThat(lines[1]).contains("Living Room");
        assertThat(lines[1]).contains("60");
        assertThat(lines[1]).contains("0.36");
        assertThat(lines[1]).contains("2.52");
    }

    // -------------------------------------------------------------------------
    // escapeCsv
    // -------------------------------------------------------------------------

    @Test
    void escapeCsv_plainValue_returnsUnchanged() {
        assertThat(CsvExportService.escapeCsv("Hello")).isEqualTo("Hello");
    }

    @Test
    void escapeCsv_nullValue_returnsEmptyString() {
        assertThat(CsvExportService.escapeCsv(null)).isEqualTo("");
    }

    @Test
    void escapeCsv_valueWithComma_wrapsInQuotes() {
        assertThat(CsvExportService.escapeCsv("Hello, World")).isEqualTo("\"Hello, World\"");
    }

    @Test
    void escapeCsv_valueWithDoubleQuote_doublesQuoteAndWraps() {
        assertThat(CsvExportService.escapeCsv("Say \"hi\"")).isEqualTo("\"Say \"\"hi\"\"\"");
    }

    @Test
    void escapeCsv_valueWithNewline_wrapsInQuotes() {
        assertThat(CsvExportService.escapeCsv("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ActivityLog makeActivityLog(String deviceName, String roomName,
                                        String actor, String action, Instant timestamp) {
        User user = new User("Owner", "owner@test.com", "hashed");
        Room room = new Room(user, roomName, "home");
        Device device = new Device(room, deviceName, DeviceType.SWITCH);
        return new ActivityLog(timestamp, device, user, actor, action);
    }
}
