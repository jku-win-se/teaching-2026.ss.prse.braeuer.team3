package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Scene;
import at.jku.se.smarthome.domain.SceneEntry;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.SceneEntryRequest;
import at.jku.se.smarthome.dto.SceneEntryResponse;
import at.jku.se.smarthome.dto.SceneRequest;
import at.jku.se.smarthome.dto.SceneResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.SceneRepository;
import at.jku.se.smarthome.websocket.DeviceWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Service for managing scenes in the SmartHome Orchestrator.
 *
 * <p>Provides CRUD operations for {@link Scene} entities and the activation
 * operation that applies all contained device-action entries to their target
 * devices using {@link DeviceService#updateStateAsActor}.</p>
 *
 * <p>Scene management is owner-only (US-018). Each device referenced in a scene
 * must be owned by the authenticated user.</p>
 */
@Service
public class SceneService {

    private static final Logger log = LoggerFactory.getLogger(SceneService.class);

    private final SceneRepository sceneRepository;
    private final DeviceRepository deviceRepository;
    private final MemberService memberService;
    private final DeviceService deviceService;
    private final DeviceWebSocketHandler wsHandler;

    /**
     * Constructs a SceneService with all required dependencies.
     *
     * @param sceneRepository  the repository for scene persistence
     * @param deviceRepository the repository for device lookups and ownership checks
     * @param memberService    the service used for owner-only authorization
     * @param deviceService    the service used to apply device state on scene activation
     * @param wsHandler        the WebSocket handler used to push real-time scene updates
     */
    public SceneService(SceneRepository sceneRepository,
                        DeviceRepository deviceRepository,
                        MemberService memberService,
                        DeviceService deviceService,
                        DeviceWebSocketHandler wsHandler) {
        this.sceneRepository = sceneRepository;
        this.deviceRepository = deviceRepository;
        this.memberService = memberService;
        this.deviceService = deviceService;
        this.wsHandler = wsHandler;
    }

    /**
     * Returns all scenes owned by the authenticated user.
     *
     * @param email the email of the authenticated user
     * @return list of scene response DTOs, ordered by id ascending
     * @throws ResponseStatusException with status 403 if the caller is a member, not an owner
     */
    @Transactional(readOnly = true)
    public List<SceneResponse> getScenes(String email) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        return sceneRepository.findByUserOrderByIdAsc(user)
                .stream()
                .map(SceneService::toResponse)
                .toList();
    }

    /**
     * Creates a new scene for the authenticated user.
     *
     * @param email   the email of the authenticated user
     * @param request the scene creation request
     * @return the newly created scene as a response DTO
     * @throws ResponseStatusException with status 400 if the name or entries are missing
     * @throws ResponseStatusException with status 403 if the caller is a member, not an owner
     * @throws ResponseStatusException with status 404 if a referenced device is not found or not owned
     */
    @Transactional
    public SceneResponse createScene(String email, SceneRequest request) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        validateRequest(request);

        Scene scene = new Scene();
        scene.setUser(user);
        applyRequest(scene, request, user);

        Scene saved = sceneRepository.save(scene);
        if (log.isInfoEnabled()) {
            log.info("Scene {} created by {}", saved.getId(), email);
        }
        wsHandler.broadcastSceneUpdate(email);
        return toResponse(saved);
    }

    /**
     * Fully replaces an existing scene with new values.
     *
     * @param email   the email of the authenticated user
     * @param sceneId the primary key of the scene to update
     * @param request the replacement request
     * @return the updated scene as a response DTO
     * @throws ResponseStatusException with status 400 if the name or entries are missing
     * @throws ResponseStatusException with status 403 if the caller is a member, not an owner
     * @throws ResponseStatusException with status 404 if the scene or a device is not found or not owned
     */
    @Transactional
    public SceneResponse updateScene(String email, Long sceneId, SceneRequest request) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        validateRequest(request);

        Scene scene = resolveOwnedScene(user, sceneId);
        applyRequest(scene, request, user);

        Scene saved = sceneRepository.save(scene);
        wsHandler.broadcastSceneUpdate(email);
        return toResponse(saved);
    }

    /**
     * Deletes a scene owned by the authenticated user.
     *
     * @param email   the email of the authenticated user
     * @param sceneId the primary key of the scene to delete
     * @throws ResponseStatusException with status 403 if the caller is a member, not an owner
     * @throws ResponseStatusException with status 404 if the scene is not found or not owned
     */
    @Transactional
    public void deleteScene(String email, Long sceneId) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        Scene scene = resolveOwnedScene(user, sceneId);
        sceneRepository.delete(scene);
        if (log.isInfoEnabled()) {
            log.info("Scene {} deleted by {}", sceneId, email);
        }
        wsHandler.broadcastSceneUpdate(email);
    }

    /**
     * Activates a scene by applying all its device-action entries.
     *
     * <p>Each entry is applied via {@link DeviceService#updateStateAsActor} so that
     * activity log entries are created and WebSocket updates are broadcast.
     * Failures on individual entries are logged but do not abort the remaining entries.</p>
     *
     * @param email   the email of the authenticated user
     * @param sceneId the primary key of the scene to activate
     * @throws ResponseStatusException with status 403 if the caller is a member, not an owner
     * @throws ResponseStatusException with status 404 if the scene is not found or not owned
     */
    @Transactional
    public void activateScene(String email, Long sceneId) {
        memberService.requireOwnerRole(email);
        User user = memberService.resolveEffectiveOwner(email);
        Scene scene = resolveOwnedScene(user, sceneId);
        String actorName = "Scene (" + scene.getName() + ")";

        for (SceneEntry entry : scene.getEntries()) {
            try {
                DeviceStateRequest stateRequest = buildStateRequest(entry);
                deviceService.updateStateAsActor(entry.getDevice().getId(), stateRequest, user, actorName);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Scene {} entry for device {} failed: {}",
                            sceneId, entry.getDevice().getId(), e.getMessage());
                }
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Scene {} activated by {}", sceneId, email);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void validateRequest(SceneRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scene name is required.");
        }
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A scene must contain at least one device entry.");
        }
    }

    private void applyRequest(Scene scene, SceneRequest request, User user) {
        scene.setName(request.getName());
        scene.setIcon(request.getIcon() != null ? request.getIcon() : "auto_awesome");

        List<SceneEntry> newEntries = request.getEntries().stream()
                .map(er -> buildEntry(scene, er, user))
                .toList();
        scene.setEntries(newEntries);
    }

    private SceneEntry buildEntry(Scene scene, SceneEntryRequest entryRequest, User user) {
        Device device = deviceRepository.findById(entryRequest.getDeviceId())
                .filter(d -> Objects.equals(d.getRoom().getUser().getId(), user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Device " + entryRequest.getDeviceId() + " not found."));

        SceneEntry entry = new SceneEntry();
        entry.setScene(scene);
        entry.setDevice(device);
        entry.setActionValue(entryRequest.getActionValue());
        return entry;
    }

    private DeviceStateRequest buildStateRequest(SceneEntry entry) {
        DeviceStateRequest req = new DeviceStateRequest();
        String action = entry.getActionValue();

        if (entry.getDevice().getType() == DeviceType.COVER) {
            boolean open = "open".equalsIgnoreCase(action);
            req.setStateOn(open);
            req.setCoverPosition(open ? 100 : 0);
        } else {
            req.setStateOn("true".equalsIgnoreCase(action));
        }
        return req;
    }

    private Scene resolveOwnedScene(User user, Long sceneId) {
        return sceneRepository.findByIdAndUser(sceneId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found."));
    }

    private static SceneResponse toResponse(Scene scene) {
        List<SceneEntryResponse> entries = scene.getEntries().stream()
                .map(e -> new SceneEntryResponse(
                        e.getDevice().getId(),
                        e.getDevice().getName(),
                        e.getActionValue()))
                .toList();
        return new SceneResponse(scene.getId(), scene.getName(), scene.getIcon(), entries);
    }
}
