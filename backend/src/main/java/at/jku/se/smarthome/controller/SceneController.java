package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.SceneRequest;
import at.jku.se.smarthome.dto.SceneResponse;
import at.jku.se.smarthome.service.SceneService;
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
 * REST controller for scene management.
 *
 * <p>All endpoints require a valid JWT Bearer token. Scenes are always scoped
 * to devices owned by the authenticated user. Scene management is owner-only.</p>
 *
 * <p>Implements US-018: Szenen erstellen, aktivieren, bearbeiten und löschen.</p>
 *
 * <pre>
 * GET    /api/scenes
 * POST   /api/scenes
 * PUT    /api/scenes/{id}
 * DELETE /api/scenes/{id}
 * POST   /api/scenes/{id}/activate
 * </pre>
 */
@RestController
@RequestMapping("/api/scenes")
public class SceneController {

    private final SceneService sceneService;

    /**
     * Constructs a SceneController with the required service.
     *
     * @param sceneService the service handling scene business logic
     */
    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    /**
     * Returns all scenes owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @return 200 OK with a list of {@link SceneResponse} DTOs
     */
    @GetMapping
    public ResponseEntity<List<SceneResponse>> getScenes(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(sceneService.getScenes(principal.getUsername()));
    }

    /**
     * Creates a new scene for the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param request   the scene creation request
     * @return 201 Created with the new {@link SceneResponse}
     */
    @PostMapping
    public ResponseEntity<SceneResponse> createScene(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody SceneRequest request) {
        SceneResponse response = sceneService.createScene(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fully replaces a scene owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the scene to update
     * @param request   the replacement request
     * @return 200 OK with the updated {@link SceneResponse}, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<SceneResponse> updateScene(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody SceneRequest request) {
        SceneResponse response = sceneService.updateScene(principal.getUsername(), id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a scene owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the scene to delete
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScene(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        sceneService.deleteScene(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a scene, applying all its device-action entries.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the scene to activate
     * @return 204 No Content on success, or 404 if not found
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateScene(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        sceneService.activateScene(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
