package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.RoomRequest;
import at.jku.se.smarthome.dto.RoomResponse;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing rooms in the SmartHome Orchestrator.
 *
 * <p>Implements the acceptance criteria for:</p>
 * <ul>
 *   <li>US-004: Raum mit Name erstellen möglich</li>
 *   <li>US-004: Raum umbenennen möglich</li>
 *   <li>US-004: Raum löschen möglich (inkl. Hinweis bei vorhandenen Geräten)</li>
 * </ul>
 *
 * <p>FR-13: Read operations (getRooms) are available to members and resolve against
 * the effective owner's home. Write operations (create, rename, delete) are
 * restricted to owners only (403 Forbidden for members).</p>
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MemberService memberService;

    /**
     * Constructs a RoomService with the required repositories and member service.
     *
     * @param roomRepository the repository for room persistence
     * @param userRepository the repository for resolving the current user
     * @param memberService  the service for role resolution and owner context (FR-13)
     */
    public RoomService(RoomRepository roomRepository,
                       UserRepository userRepository,
                       MemberService memberService) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.memberService = memberService;
    }

    /**
     * Returns all rooms belonging to the effective owner's home.
     *
     * <p>FR-13: Members see the owner's rooms instead of their own.</p>
     *
     * @param email the email of the authenticated user (owner or member)
     * @return list of room responses ordered by creation date
     */
    public List<RoomResponse> getRooms(String email) {
        User effectiveOwner = memberService.resolveEffectiveOwner(email);
        return roomRepository.findByUserIdOrderByCreatedAtAsc(effectiveOwner.getId())
                .stream()
                .map(r -> new RoomResponse(r.getId(), r.getName(), r.getIcon()))
                .toList();
    }

    /**
     * Creates a new room for the authenticated owner.
     * US-004: Raum mit Name erstellen möglich.
     *
     * <p>FR-13: Only owners may create rooms (403 for members).</p>
     *
     * @param email   the email of the authenticated owner
     * @param request the room creation request
     * @return the newly created room
     * @throws ResponseStatusException with status 403 if the caller is a member
     * @throws ResponseStatusException with status 409 if a room with the same name already exists
     */
    @Transactional
    public RoomResponse createRoom(String email, RoomRequest request) {
        memberService.requireOwnerRole(email, "add rooms");
        User effectiveOwner = memberService.resolveEffectiveOwner(email);
        if (roomRepository.existsByUserIdAndName(effectiveOwner.getId(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A room named '" + request.getName() + "' already exists.");
        }
        Room room = new Room(effectiveOwner, request.getName(), request.getIcon());
        Room saved = roomRepository.save(room);
        return new RoomResponse(saved.getId(), saved.getName(), saved.getIcon());
    }

    /**
     * Renames an existing room.
     * US-004: Raum umbenennen möglich.
     *
     * <p>FR-13: Only owners may rename rooms (403 for members).</p>
     *
     * @param email   the email of the authenticated owner
     * @param id      the room id to rename
     * @param request the rename request containing the new name
     * @return the updated room
     * @throws ResponseStatusException with status 403 if the caller is a member
     * @throws ResponseStatusException with status 404 if the room is not found or belongs to another user
     * @throws ResponseStatusException with status 409 if a room with the new name already exists
     */
    @Transactional
    public RoomResponse renameRoom(String email, Long id, RoomRequest request) {
        memberService.requireOwnerRole(email, "rename rooms");
        User effectiveOwner = memberService.resolveEffectiveOwner(email);
        Room room = roomRepository.findByIdAndUserId(id, effectiveOwner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        if (!room.getName().equals(request.getName())
                && roomRepository.existsByUserIdAndName(effectiveOwner.getId(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A room named '" + request.getName() + "' already exists.");
        }
        room.setName(request.getName());
        if (request.getIcon() != null) {
            room.setIcon(request.getIcon());
        }
        Room saved = roomRepository.save(room);
        return new RoomResponse(saved.getId(), saved.getName(), saved.getIcon());
    }

    /**
     * Deletes a room.
     * US-004: Raum löschen möglich.
     *
     * <p>FR-13: Only owners may delete rooms (403 for members).</p>
     *
     * @param email the email of the authenticated owner
     * @param id    the room id to delete
     * @throws ResponseStatusException with status 403 if the caller is a member
     * @throws ResponseStatusException with status 404 if the room is not found or belongs to another user
     */
    @Transactional
    public void deleteRoom(String email, Long id) {
        memberService.requireOwnerRole(email, "delete rooms");
        User effectiveOwner = memberService.resolveEffectiveOwner(email);
        Room room = roomRepository.findByIdAndUserId(id, effectiveOwner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        roomRepository.delete(room);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
