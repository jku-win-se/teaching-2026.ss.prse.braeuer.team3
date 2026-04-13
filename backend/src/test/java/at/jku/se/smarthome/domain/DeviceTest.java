package at.jku.se.smarthome.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Device}.
 *
 * <p>Verifies constructors, default field values, and all getter/setter pairs
 * without requiring a JPA persistence provider.</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceTest {

    /**
     * The default constructor must initialise state fields to their documented defaults.
     */
    @Test
    void defaultConstructor_setsDefaults() {
        Device device = new Device();

        assertThat(device.getId()).isNull();
        assertThat(device.isStateOn()).isFalse();
        assertThat(device.getBrightness()).isEqualTo(50);
        assertThat(device.getTemperature()).isEqualTo(21.0);
        assertThat(device.getSensorValue()).isEqualTo(0.0);
        assertThat(device.getCoverPosition()).isEqualTo(0);
        assertThat(device.getCreatedAt()).isNotNull();
    }

    /**
     * The parameterized constructor must assign room, name, and type.
     */
    @Test
    void parameterizedConstructor_setsRoomNameType() {
        Room room = new Room();
        Device device = new Device(room, "Lamp", DeviceType.SWITCH);

        assertThat(device.getRoom()).isSameAs(room);
        assertThat(device.getName()).isEqualTo("Lamp");
        assertThat(device.getType()).isEqualTo(DeviceType.SWITCH);
    }

    /**
     * {@code getId()} returns null before the entity is persisted.
     */
    @Test
    void getId_returnsNull_beforePersistence() {
        assertThat(new Device().getId()).isNull();
    }

    /**
     * {@code getCreatedAt()} returns a non-null timestamp immediately after construction.
     */
    @Test
    void getCreatedAt_isNotNull() {
        assertThat(new Device().getCreatedAt()).isNotNull();
    }

    /**
     * {@code setName} / {@code getName} must form a consistent round-trip.
     */
    @Test
    void setName_getName_roundTrip() {
        Device device = new Device();
        device.setName("Thermostat");

        assertThat(device.getName()).isEqualTo("Thermostat");
    }

    /**
     * {@code setStateOn} / {@code isStateOn} must form a consistent round-trip.
     */
    @Test
    void setStateOn_isStateOn_roundTrip() {
        Device device = new Device();
        device.setStateOn(true);

        assertThat(device.isStateOn()).isTrue();
    }

    /**
     * {@code setBrightness} / {@code getBrightness} must form a consistent round-trip.
     */
    @Test
    void setBrightness_getBrightness_roundTrip() {
        Device device = new Device();
        device.setBrightness(80);

        assertThat(device.getBrightness()).isEqualTo(80);
    }

    /**
     * {@code setTemperature} / {@code getTemperature} must form a consistent round-trip.
     */
    @Test
    void setTemperature_getTemperature_roundTrip() {
        Device device = new Device();
        device.setTemperature(23.5);

        assertThat(device.getTemperature()).isEqualTo(23.5);
    }

    /**
     * {@code setSensorValue} / {@code getSensorValue} must form a consistent round-trip.
     */
    @Test
    void setSensorValue_getSensorValue_roundTrip() {
        Device device = new Device();
        device.setSensorValue(42.7);

        assertThat(device.getSensorValue()).isEqualTo(42.7);
    }

    /**
     * {@code setCoverPosition} / {@code getCoverPosition} must form a consistent round-trip.
     */
    @Test
    void setCoverPosition_getCoverPosition_roundTrip() {
        Device device = new Device();
        device.setCoverPosition(75);

        assertThat(device.getCoverPosition()).isEqualTo(75);
    }
}
