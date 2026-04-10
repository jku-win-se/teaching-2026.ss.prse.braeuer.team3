package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.RoomRequest;
import at.jku.se.smarthome.dto.RoomResponse;
import at.jku.se.smarthome.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing CRUD operations for rooms.
 *
 * <p>All endpoints require a valid JWT Bearer token.
 * Rooms are always scoped to the authenticated user.</p>
 *
 * <p>Implements US-004: Raum erstellen, umbenennen, löschen.</p>
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    /**
     * Constructs a RoomController with the required service.
     *
     * @param roomService the service handling room operations
     */
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Returns all rooms of the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @return 200 OK with the list of rooms
     */
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms(@AuthenticationPrincipal UserDetails principal) {
        List<RoomResponse> rooms = roomService.getRooms(principal.getUsername());
        return ResponseEntity.ok(rooms);
    }

    /**
     * Creates a new room for the authenticated user.
     * US-004: Raum mit Name erstellen möglich.
     *
     * @param principal the authenticated user
     * @param request   the room creation request
     * @return 201 Created with the new room data
     */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.createRoom(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    /**
     * Renames an existing room.
     * US-004: Raum umbenennen möglich.
     *
     * @param principal the authenticated user
     * @param id        the room id to update
     * @param request   the rename request
     * @return 200 OK with the updated room data
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> renameRoom(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.renameRoom(principal.getUsername(), id, request);
        return ResponseEntity.ok(room);
    }

    /**
     * Deletes a room.
     * US-004: Raum löschen möglich.
     *
     * @param principal the authenticated user
     * @param id        the room id to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        roomService.deleteRoom(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
