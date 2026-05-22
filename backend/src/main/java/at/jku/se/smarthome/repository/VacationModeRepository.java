package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.domain.VacationMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link VacationMode} entities.
 *
 * <p>Provides queries needed by {@link at.jku.se.smarthome.service.VacationModeService}
 * for CRUD operations and the daily scheduler check.</p>
 */
public interface VacationModeRepository extends JpaRepository<VacationMode, Long> {

    /**
     * Returns all vacation modes owned by the given user.
     *
     * @param user the owning user
     * @return list of vacation modes for that user, in no particular order
     */
    List<VacationMode> findByUser(User user);

    /**
     * Returns the vacation mode with the given id if it belongs to the given user.
     *
     * @param id   the vacation mode primary key
     * @param user the owning user
     * @return an {@link Optional} containing the vacation mode, or empty if not found or not owned
     */
    Optional<VacationMode> findByIdAndUser(Long id, User user);

    /**
     * Returns all vacation modes that have not yet been permanently deactivated.
     * Used by the daily scheduler to check which modes need state updates.
     *
     * @return list of non-deactivated vacation modes
     */
    List<VacationMode> findByDeactivatedFalse();
}
