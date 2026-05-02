package at.jku.se.smarthome.dto;

/**
 * Response DTO containing estimated energy consumption for one device.
 *
 * <p>US-016: The frontend uses this data to display device-level consumption
 * and to aggregate room and household totals.</p>
 */
public class EnergyDeviceResponse {

    /** The device's primary key. */
    private final Long deviceId;

    /** Human-readable device name. */
    private final String deviceName;

    /** Human-readable room name. */
    private final String room;

    /** Estimated nominal wattage for this device. */
    private final int wattage;

    /** Estimated consumption for today in kWh. */
    private final double todayKwh;

    /** Estimated consumption for the current week in kWh. */
    private final double weekKwh;

    /**
     * Creates an energy response for a single device.
     *
     * @param deviceId   the device id
     * @param deviceName the device name
     * @param room       the room name
     * @param wattage    estimated nominal wattage
     * @param todayKwh   estimated daily consumption
     * @param weekKwh    estimated weekly consumption
     */
    public EnergyDeviceResponse(Long deviceId, String deviceName, String room,
                                int wattage, double todayKwh, double weekKwh) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.room = room;
        this.wattage = wattage;
        this.todayKwh = todayKwh;
        this.weekKwh = weekKwh;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getRoom() {
        return room;
    }

    public int getWattage() {
        return wattage;
    }

    public double getTodayKwh() {
        return todayKwh;
    }

    public double getWeekKwh() {
        return weekKwh;
    }
}
