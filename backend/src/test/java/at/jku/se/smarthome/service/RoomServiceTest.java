package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.RoomRequest;
import at.jku.se.smarthome.dto.RoomResponse;
import at.jku.se.smarthome.repository.RoomRepository;
import at.jku.se.smarthome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomService}.
 *
 * <p>Covers acceptance criteria for:</p>
 * <ul>
 *   <li>US-004: Raum mit Name erstellen möglich</li>
 *   <li>US-004: Raum umbenennen möglich</li>
 *   <li>US-004: Raum löschen möglich (inkl. Hinweis bei vorhandenen Geräten)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private RoomService roomService;

    private User user;
    private Room room;
    private RoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        user = new User("Alice", "alice@example.com", "hashed");
        room = new Room(user, "Living Room", "weekend");
        roomRequest = new RoomRequest();
        roomRequest.setName("Living Room");
        roomRequest.setIcon("weekend");
    }

    // ── getRooms ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRooms: gibt alle Räume des Benutzers zurück")
    void getRooms_returnsAllRoomsForUser() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(room));

        List<RoomResponse> rooms = roomService.getRooms("alice@example.com");

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getName()).isEqualTo("Living Room");
    }

    @Test
    @DisplayName("getRooms: gibt leere Liste zurück wenn keine Räume vorhanden")
    void getRooms_returnsEmptyListWhenNoRooms() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of());

        List<RoomResponse> rooms = roomService.getRooms("alice@example.com");

        assertThat(rooms).isEmpty();
    }

    // ── US-004: createRoom ────────────────────────────────────────────────────

    @Test
    @DisplayName("US-004: Raum mit Name erstellen möglich")
    void createRoom_withValidData_returnsCreatedRoom() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.existsByUserIdAndName(user.getId(), "Living Room")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomResponse response = roomService.createRoom("alice@example.com", roomRequest);

        assertThat(response.getName()).isEqualTo("Living Room");
        assertThat(response.getIcon()).isEqualTo("weekend");
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("US-004: Raum erstellen - Duplikat-Name wird abgelehnt (409)")
    void createRoom_withDuplicateName_throwsConflict() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.existsByUserIdAndName(user.getId(), "Living Room")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom("alice@example.com", roomRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-004: Raum erstellen - Icon wird korrekt gespeichert")
    void createRoom_savesIconCorrectly() {
        roomRequest.setIcon("kitchen");
        Room kitchenRoom = new Room(user, "Living Room", "kitchen");
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.existsByUserIdAndName(any(), any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(kitchenRoom);

        RoomResponse response = roomService.createRoom("alice@example.com", roomRequest);

        assertThat(response.getIcon()).isEqualTo("kitchen");
    }

    // ── US-004: renameRoom ────────────────────────────────────────────────────

    @Test
    @DisplayName("US-004: Raum umbenennen möglich")
    void renameRoom_withValidData_returnsUpdatedRoom() {
        RoomRequest renameRequest = new RoomRequest();
        renameRequest.setName("Kitchen");
        renameRequest.setIcon("kitchen");
        Room renamedRoom = new Room(user, "Kitchen", "kitchen");

        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsByUserIdAndName(user.getId(), "Kitchen")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(renamedRoom);

        RoomResponse response = roomService.renameRoom("alice@example.com", 1L, renameRequest);

        assertThat(response.getName()).isEqualTo("Kitchen");
    }

    @Test
    @DisplayName("US-004: Raum umbenennen - Raum nicht gefunden (404)")
    void renameRoom_whenNotFound_throwsNotFound() {
        RoomRequest renameRequest = new RoomRequest();
        renameRequest.setName("Kitchen");

        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.renameRoom("alice@example.com", 99L, renameRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("US-004: Raum umbenennen - neuer Name bereits vergeben (409)")
    void renameRoom_withDuplicateName_throwsConflict() {
        RoomRequest renameRequest = new RoomRequest();
        renameRequest.setName("Bedroom");

        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsByUserIdAndName(user.getId(), "Bedroom")).thenReturn(true);

        assertThatThrownBy(() -> roomService.renameRoom("alice@example.com", 1L, renameRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("US-004: Raum umbenennen - gleicher Name ist erlaubt (kein Konflikt)")
    void renameRoom_withSameName_doesNotThrow() {
        RoomRequest renameRequest = new RoomRequest();
        renameRequest.setName("Living Room"); // same name as current

        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomResponse response = roomService.renameRoom("alice@example.com", 1L, renameRequest);

        assertThat(response.getName()).isEqualTo("Living Room");
    }

    // ── US-004: deleteRoom ────────────────────────────────────────────────────

    @Test
    @DisplayName("US-004: Raum löschen möglich")
    void deleteRoom_withValidId_deletesRoom() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(1L, user.getId())).thenReturn(Optional.of(room));

        roomService.deleteRoom("alice@example.com", 1L);

        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("US-004: Raum löschen - Raum nicht gefunden (404)")
    void deleteRoom_whenNotFound_throwsNotFound() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        when(roomRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.deleteRoom("alice@example.com", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");

        verify(roomRepository, never()).delete(any());
    }

    @Test
    @DisplayName("US-004: Raum löschen - fremder Raum nicht löschbar (404)")
    void deleteRoom_whenRoomBelongsToOtherUser_throwsNotFound() {
        when(memberService.resolveEffectiveOwner("alice@example.com")).thenReturn(user);
        // findByIdAndUserId returns empty because the room belongs to another user
        when(roomRepository.findByIdAndUserId(5L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.deleteRoom("alice@example.com", 5L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
