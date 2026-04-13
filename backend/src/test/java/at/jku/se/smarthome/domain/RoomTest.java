package at.jku.se.smarthome.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Room}.
 *
 * <p>Verifies constructors, default field values, and all getter/setter pairs
 * without requiring a JPA persistence provider.</p>
 */
@ExtendWith(MockitoExtension.class)
class RoomTest {

    /**
     * The default constructor must leave id as null and initialise createdAt.
     */
    @Test
    void defaultConstructor_idNullCreatedAtNotNull() {
        Room room = new Room();

        assertThat(room.getId()).isNull();
        assertThat(room.getCreatedAt()).isNotNull();
    }

    /**
     * The parameterized constructor must assign user, name, and icon.
     */
    @Test
    void parameterizedConstructor_setsUserNameIcon() {
        User user = new User("Bob", "bob@example.com", "hash");
        Room room = new Room(user, "Living Room", "living_room");

        assertThat(room.getUser()).isSameAs(user);
        assertThat(room.getName()).isEqualTo("Living Room");
        assertThat(room.getIcon()).isEqualTo("living_room");
    }

    /**
     * {@code getId()} returns null before the entity is persisted.
     */
    @Test
    void getId_returnsNull_beforePersistence() {
        assertThat(new Room().getId()).isNull();
    }

    /**
     * {@code getUser()} returns the owner set via the constructor.
     */
    @Test
    void getUser_returnsConstructorUser() {
        User user = new User("Carol", "carol@example.com", "hash");
        Room room = new Room(user, "Kitchen", "kitchen");

        assertThat(room.getUser()).isSameAs(user);
    }

    /**
     * {@code getCreatedAt()} returns a non-null timestamp immediately after construction.
     */
    @Test
    void getCreatedAt_isNotNull() {
        assertThat(new Room().getCreatedAt()).isNotNull();
    }

    /**
     * {@code setName} / {@code getName} must form a consistent round-trip.
     */
    @Test
    void setName_getName_roundTrip() {
        Room room = new Room();
        room.setName("Office");

        assertThat(room.getName()).isEqualTo("Office");
    }

    /**
     * {@code setIcon} / {@code getIcon} must form a consistent round-trip.
     */
    @Test
    void setIcon_getIcon_roundTrip() {
        Room room = new Room();
        room.setIcon("bedroom");

        assertThat(room.getIcon()).isEqualTo("bedroom");
    }
}
