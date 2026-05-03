package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.Scene;
import at.jku.se.smarthome.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Scene} entities.
 *
 * <p>Provides owner-scoped queries needed by
 * {@link at.jku.se.smarthome.service.SceneService} for CRUD and activation.</p>
 */
public interface SceneRepository extends JpaRepository<Scene, Long> {

    /**
     * Returns all scenes owned by the given user, ordered by id ascending.
     *
     * @param user the owning user
     * @return list of scenes for that user
     */
    List<Scene> findByUserOrderByIdAsc(User user);

    /**
     * Returns the scene with the given id if it belongs to the given user.
     *
     * @param id   the scene's primary key
     * @param user the owning user
     * @return an {@link Optional} containing the scene, or empty if not found or not owned
     */
    Optional<Scene> findByIdAndUser(Long id, User user);
}
