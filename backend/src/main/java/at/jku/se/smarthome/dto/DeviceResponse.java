package at.jku.se.smarthome.dto;

import at.jku.se.smarthome.domain.DeviceType;

/**
 * Response DTO representing a device returned by the API (FR-04).
 */
public class DeviceResponse {

    /** The device's primary key. */
    private Long id;

    /** The human-readable device name. */
    private String name;

    /** The device type, serialized as a lowercase string. */
    private DeviceType type;

    /**
     * Creates a DeviceResponse.
     *
     * @param id   the device id
     * @param name the device name
     * @param type the device type
     */
    public DeviceResponse(Long id, String name, DeviceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the device id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the device name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the device type.
     *
     * @return the type
     */
    public DeviceType getType() {
        return type;
    }
}
