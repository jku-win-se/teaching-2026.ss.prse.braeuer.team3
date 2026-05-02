package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.domain.HomeMember;
import at.jku.se.smarthome.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link HomeMember} entities.
 *
 * <p>Provides membership lookups used for role resolution (FR-13)
 * and member management (FR-20).</p>
 */
public interface HomeMemberRepository extends JpaRepository<HomeMember, Long> {

    /**
     * Finds the membership record for the given member user.
     *
     * <p>Returns empty if the user is an owner (has no membership record).</p>
     *
     * @param member the user to check for membership
     * @return an Optional containing the membership, or empty if the user is an owner
     */
    Optional<HomeMember> findByMember(User member);

    /**
     * Returns all membership records for the given owner.
     *
     * @param owner the owner whose members to list
     * @return list of membership records
     */
    List<HomeMember> findByOwner(User owner);

    /**
     * Finds a specific membership record by owner and member user ID.
     *
     * @param owner    the owner of the home
     * @param memberId the primary key of the member user
     * @return an Optional containing the membership, or empty if not found
     */
    Optional<HomeMember> findByOwnerAndMemberId(User owner, Long memberId);
}
