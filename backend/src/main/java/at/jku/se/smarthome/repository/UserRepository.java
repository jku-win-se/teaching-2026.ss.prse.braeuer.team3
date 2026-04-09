package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations and a custom query for
 * looking up users by their unique email address.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user if found, or empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email address already exists.
     * Used to enforce the uniqueness constraint at the service level.
     *
     * @param email the email address to check
     * @return {@code true} if a user with that email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
