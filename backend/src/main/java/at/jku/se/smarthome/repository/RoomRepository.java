package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Room} entities.
 *
 * <p>Provides standard CRUD operations and user-scoped queries for rooms.</p>
 */
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Returns all rooms belonging to the specified user, ordered by creation date.
     *
     * @param userId the owner's user id
     * @return list of rooms owned by that user
     */
    List<Room> findByUserIdOrderByCreatedAtAsc(Long userId);

    /**
     * Finds a room by its id, verifying it belongs to the given user.
     * Prevents users from accessing or modifying rooms of other users.
     *
     * @param id     the room id
     * @param userId the expected owner's user id
     * @return an {@link Optional} containing the room if it exists and belongs to the user
     */
    Optional<Room> findByIdAndUserId(Long id, Long userId);

    /**
     * Checks whether a room with the given name already exists for a user.
     *
     * @param userId the owner's user id
     * @param name   the room name to check
     * @return {@code true} if a room with that name already exists for the user
     */
    boolean existsByUserIdAndName(Long userId, String name);
}
