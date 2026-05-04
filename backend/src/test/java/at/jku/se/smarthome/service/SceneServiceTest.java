package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.Scene;
import at.jku.se.smarthome.domain.SceneEntry;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.SceneEntryRequest;
import at.jku.se.smarthome.dto.SceneRequest;
import at.jku.se.smarthome.dto.SceneResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.SceneRepository;
import at.jku.se.smarthome.websocket.DeviceWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SceneServiceTest {

    @Mock private SceneRepository sceneRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private MemberService memberService;
    @Mock private DeviceService deviceService;
    @Mock private DeviceWebSocketHandler wsHandler;

    private SceneService sceneService;

    private User user;
    private Room room;
    private Device switchDevice;
    private Device coverDevice;

    private static final String EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        sceneService = new SceneService(sceneRepository, deviceRepository, memberService, deviceService, wsHandler);

        user = new User("Test User", EMAIL, "hashed");
        ReflectionTestUtils.setField(user, "id", 1L);

        room = new Room(user, "Living Room", "weekend");
        ReflectionTestUtils.setField(room, "id", 2L);

        switchDevice = new Device(room, "TV", DeviceType.SWITCH);
        ReflectionTestUtils.setField(switchDevice, "id", 10L);

        coverDevice = new Device(room, "Blind", DeviceType.COVER);
        ReflectionTestUtils.setField(coverDevice, "id", 11L);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(user);
    }

    // --- getScenes ---

    @Test
    void getScenes_returnsOwnedScenes() {
        Scene scene = buildScene("Movie Night", "movie");
        when(sceneRepository.findByUserOrderByIdAsc(user)).thenReturn(List.of(scene));

        List<SceneResponse> result = sceneService.getScenes(EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Movie Night");
    }

    @Test
    void getScenes_emptyList_returnsEmpty() {
        when(sceneRepository.findByUserOrderByIdAsc(user)).thenReturn(List.of());

        List<SceneResponse> result = sceneService.getScenes(EMAIL);

        assertThat(result).isEmpty();
    }

    // --- createScene ---

    @Test
    void createScene_validRequest_persistsAndReturnsResponse() {
        SceneRequest request = buildRequest("Movie Night", "movie", 10L, "true");
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(switchDevice));

        Scene saved = buildScene("Movie Night", "movie");
        when(sceneRepository.save(any(Scene.class))).thenReturn(saved);

        SceneResponse response = sceneService.createScene(EMAIL, request);

        assertThat(response.getName()).isEqualTo("Movie Night");
        verify(sceneRepository).save(any(Scene.class));
    }

    @Test
    void createScene_missingName_throws400() {
        SceneRequest request = buildRequest(null, "movie", 10L, "true");

        assertThatThrownBy(() -> sceneService.createScene(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void createScene_emptyEntries_throws400() {
        SceneRequest request = new SceneRequest();
        request.setName("Test");
        request.setIcon("star");
        request.setEntries(List.of());

        assertThatThrownBy(() -> sceneService.createScene(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void createScene_deviceNotOwned_throws404() {
        SceneRequest request = buildRequest("Test", "star", 99L, "true");
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sceneService.createScene(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // --- updateScene ---

    @Test
    void updateScene_existing_updatesAndReturns() {
        Scene existing = buildScene("Old Name", "star");
        when(sceneRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(existing));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(switchDevice));
        when(sceneRepository.save(any(Scene.class))).thenReturn(existing);

        SceneRequest request = buildRequest("New Name", "movie", 10L, "false");
        SceneResponse response = sceneService.updateScene(EMAIL, 1L, request);

        assertThat(response).isNotNull();
        verify(sceneRepository).save(existing);
    }

    @Test
    void updateScene_notFound_throws404() {
        when(sceneRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        SceneRequest request = buildRequest("Test", "star", 10L, "true");

        assertThatThrownBy(() -> sceneService.updateScene(EMAIL, 99L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // --- deleteScene ---

    @Test
    void deleteScene_existing_deletesScene() {
        Scene scene = buildScene("Movie Night", "movie");
        when(sceneRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(scene));

        sceneService.deleteScene(EMAIL, 1L);

        verify(sceneRepository).delete(scene);
    }

    @Test
    void deleteScene_notFound_throws404() {
        when(sceneRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sceneService.deleteScene(EMAIL, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));

        verify(sceneRepository, never()).delete(any());
    }

    // --- activateScene ---

    @Test
    void activateScene_switchEntry_callsUpdateStateAsActor() {
        Scene scene = buildScene("Movie Night", "movie");
        SceneEntry entry = new SceneEntry();
        entry.setScene(scene);
        entry.setDevice(switchDevice);
        entry.setActionValue("true");
        scene.getEntries().add(entry);

        when(sceneRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(scene));

        sceneService.activateScene(EMAIL, 1L);

        verify(deviceService).updateStateAsActor(anyLong(), any(), any(User.class), anyString());
    }

    @Test
    void activateScene_coverEntry_callsUpdateStateAsActorWithOpenPosition() {
        Scene scene = buildScene("Night Mode", "bedtime");
        SceneEntry entry = new SceneEntry();
        entry.setScene(scene);
        entry.setDevice(coverDevice);
        entry.setActionValue("close");
        scene.getEntries().add(entry);

        when(sceneRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(scene));

        sceneService.activateScene(EMAIL, 1L);

        verify(deviceService).updateStateAsActor(anyLong(), any(), any(User.class), anyString());
    }

    @Test
    void activateScene_notFound_throws404() {
        when(sceneRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sceneService.activateScene(EMAIL, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Scene buildScene(String name, String icon) {
        Scene scene = new Scene();
        ReflectionTestUtils.setField(scene, "id", 1L);
        scene.setName(name);
        scene.setIcon(icon);
        scene.setUser(user);
        ReflectionTestUtils.setField(scene, "entries", new ArrayList<>());
        return scene;
    }

    private SceneRequest buildRequest(String name, String icon, Long deviceId, String actionValue) {
        SceneEntryRequest entry = new SceneEntryRequest();
        entry.setDeviceId(deviceId);
        entry.setActionValue(actionValue);

        SceneRequest req = new SceneRequest();
        req.setName(name);
        req.setIcon(icon);
        req.setEntries(List.of(entry));
        return req;
    }
}
