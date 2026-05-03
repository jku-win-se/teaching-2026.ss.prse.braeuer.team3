package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.ActivityLog;
import at.jku.se.smarthome.dto.EnergyDeviceResponse;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service responsible for building RFC-4180-compliant CSV content for export (FR-16).
 *
 * <p>Supports two export types:</p>
 * <ul>
 *   <li>Activity log — columns: Timestamp, Device, Room, Actor, Action</li>
 *   <li>Energy summary — columns: Device, Room, Wattage (W), Today (kWh), Week (kWh)</li>
 * </ul>
 *
 * <p>All field values are escaped according to RFC 4180: values containing commas,
 * double-quotes, or newlines are wrapped in double-quotes, and any embedded
 * double-quote characters are doubled.</p>
 */
@Service
public class CsvExportService {

    /** ISO-8601 formatter used for timestamp columns (UTC, no millis). */
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC);

    /** CSV header for the activity log export. */
    static final String ACTIVITY_LOG_HEADER = "Timestamp,Device,Room,Actor,Action";

    /** CSV header for the energy summary export. */
    static final String ENERGY_HEADER = "Device,Room,Wattage (W),Today (kWh),Week (kWh)";

    /**
     * Builds a CSV string from a list of activity log entries.
     *
     * <p>Columns: Timestamp, Device, Room, Actor, Action.</p>
     * <p>Rows are ordered exactly as provided in the input list.</p>
     *
     * @param entries the log entries to export; must not be {@code null}
     * @return the complete CSV content including header, using CRLF line endings
     */
    public String buildActivityLogCsv(List<ActivityLog> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append(ACTIVITY_LOG_HEADER).append("\r\n");
        for (ActivityLog entry : entries) {
            sb.append(escapeCsv(TIMESTAMP_FMT.format(entry.getTimestamp()))).append(',');
            sb.append(escapeCsv(entry.getDevice().getName())).append(',');
            sb.append(escapeCsv(entry.getDevice().getRoom().getName())).append(',');
            sb.append(escapeCsv(entry.getActorName())).append(',');
            sb.append(escapeCsv(entry.getAction())).append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Builds a CSV string from a list of energy device responses.
     *
     * <p>Columns: Device, Room, Wattage (W), Today (kWh), Week (kWh).</p>
     * <p>Rows are ordered exactly as provided in the input list.</p>
     *
     * @param devices the energy device responses to export; must not be {@code null}
     * @return the complete CSV content including header, using CRLF line endings
     */
    public String buildEnergyCsv(List<EnergyDeviceResponse> devices) {
        StringBuilder sb = new StringBuilder();
        sb.append(ENERGY_HEADER).append("\r\n");
        for (EnergyDeviceResponse d : devices) {
            sb.append(escapeCsv(d.getDeviceName())).append(',');
            sb.append(escapeCsv(d.getRoom())).append(',');
            sb.append(d.getWattage()).append(',');
            sb.append(d.getTodayKwh()).append(',');
            sb.append(d.getWeekKwh()).append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Escapes a single CSV field value according to RFC 4180.
     *
     * <p>If the value contains a comma, double-quote, or newline, it is wrapped
     * in double-quotes and any embedded double-quote characters are doubled.</p>
     *
     * @param value the raw field value; {@code null} is treated as an empty string
     * @return the escaped field value, ready to embed in a CSV row
     */
    static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
