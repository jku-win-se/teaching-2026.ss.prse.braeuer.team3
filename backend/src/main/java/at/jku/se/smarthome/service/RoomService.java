package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.RoomRequest;
import at.jku.se.smarthome.dto.RoomResponse;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
 * <p>All operations are scoped to the authenticated user identified by their email.</p>
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a RoomService with the required repositories.
     *
     * @param roomRepository the repository for room persistence
     * @param userRepository the repository for resolving the current user
     */
    public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns all rooms belonging to the authenticated user.
     *
     * @param email the email of the authenticated user
     * @return list of room responses ordered by creation date
     */
    public List<RoomResponse> getRooms(String email) {
        User user = getUser(email);
        return roomRepository.findByUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(r -> new RoomResponse(r.getId(), r.getName(), r.getIcon()))
                .toList();
    }

    /**
     * Creates a new room for the authenticated user.
     * US-004: Raum mit Name erstellen möglich.
     *
     * @param email   the email of the authenticated user
     * @param request the room creation request
     * @return the newly created room
     * @throws ResponseStatusException with status 409 if a room with the same name already exists
     */
    @Transactional
    public RoomResponse createRoom(String email, RoomRequest request) {
        User user = getUser(email);
        if (roomRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A room named '" + request.getName() + "' already exists.");
        }
        Room room = new Room(user, request.getName(), request.getIcon());
        Room saved = roomRepository.save(room);
        return new RoomResponse(saved.getId(), saved.getName(), saved.getIcon());
    }

    /**
     * Renames an existing room.
     * US-004: Raum umbenennen möglich.
     *
     * @param email   the email of the authenticated user
     * @param id      the room id to rename
     * @param request the rename request containing the new name
     * @return the updated room
     * @throws ResponseStatusException with status 404 if the room is not found or belongs to another user
     * @throws ResponseStatusException with status 409 if a room with the new name already exists
     */
    @Transactional
    public RoomResponse renameRoom(String email, Long id, RoomRequest request) {
        User user = getUser(email);
        Room room = roomRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        if (!room.getName().equals(request.getName())
                && roomRepository.existsByUserIdAndName(user.getId(), request.getName())) {
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
     * Note: Device-existence warnings are handled at the frontend level.
     * The backend enforces integrity through cascade configuration.
     *
     * @param email the email of the authenticated user
     * @param id    the room id to delete
     * @throws ResponseStatusException with status 404 if the room is not found or belongs to another user
     */
    @Transactional
    public void deleteRoom(String email, Long id) {
        User user = getUser(email);
        Room room = roomRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        roomRepository.delete(room);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
