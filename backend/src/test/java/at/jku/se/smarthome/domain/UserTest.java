package at.jku.se.smarthome.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link User}.
 *
 * <p>Verifies constructors, default field values, and all getter/setter pairs
 * without requiring a JPA persistence provider.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserTest {

    /**
     * The default constructor must leave id as null and initialise createdAt.
     */
    @Test
    void defaultConstructor_idNullCreatedAtNotNull() {
        User user = new User();

        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    /**
     * The parameterized constructor must assign name, email, and passwordHash.
     */
    @Test
    void parameterizedConstructor_setsAllFields() {
        User user = new User("Alice", "alice@example.com", "hashed_pw");

        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed_pw");
    }

    /**
     * {@code getId()} returns null before the entity is persisted.
     */
    @Test
    void getId_returnsNull_beforePersistence() {
        assertThat(new User().getId()).isNull();
    }

    /**
     * {@code getCreatedAt()} returns a non-null timestamp immediately after construction.
     */
    @Test
    void getCreatedAt_isNotNull() {
        assertThat(new User().getCreatedAt()).isNotNull();
    }

    /**
     * {@code setName} / {@code getName} must form a consistent round-trip.
     */
    @Test
    void setName_getName_roundTrip() {
        User user = new User();
        user.setName("Bob");

        assertThat(user.getName()).isEqualTo("Bob");
    }

    /**
     * {@code setEmail} / {@code getEmail} must form a consistent round-trip.
     */
    @Test
    void setEmail_getEmail_roundTrip() {
        User user = new User();
        user.setEmail("bob@example.com");

        assertThat(user.getEmail()).isEqualTo("bob@example.com");
    }

    /**
     * {@code setPasswordHash} / {@code getPasswordHash} must form a consistent round-trip.
     */
    @Test
    void setPasswordHash_getPasswordHash_roundTrip() {
        User user = new User();
        user.setPasswordHash("new_hash");

        assertThat(user.getPasswordHash()).isEqualTo("new_hash");
    }
}
