package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.dto.RuleRequest;
import at.jku.se.smarthome.dto.RuleResponse;
import at.jku.se.smarthome.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for IF-THEN rule management.
 *
 * <p>All endpoints require a valid JWT Bearer token. Rules are always scoped to
 * devices owned by the authenticated user. The trigger and action devices must
 * both belong to the authenticated user.</p>
 *
 * <p>Implements FR-10: Rule Engine (IF-THEN).</p>
 *
 * <pre>
 * GET    /api/rules[?deviceId={id}]
 * POST   /api/rules
 * PUT    /api/rules/{id}
 * PATCH  /api/rules/{id}/enabled
 * DELETE /api/rules/{id}
 * </pre>
 */
@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    /**
     * Constructs a {@code RuleController} with the required service.
     *
     * @param ruleService the service handling rule business logic
     */
    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Returns all rules for the authenticated user, optionally filtered by trigger device.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param deviceId  optional trigger device ID filter; omit to get all rules
     * @return 200 OK with a list of {@link RuleResponse} DTOs
     */
    @GetMapping
    public ResponseEntity<List<RuleResponse>> getRules(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long deviceId) {
        List<RuleResponse> result = ruleService.getRules(principal.getUsername(), deviceId);
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a new IF-THEN rule for the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param request   the rule creation request
     * @return 201 Created with the new {@link RuleResponse}
     */
    @PostMapping
    public ResponseEntity<RuleResponse> createRule(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody RuleRequest request) {
        RuleResponse response = ruleService.createRule(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fully replaces an existing rule owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the rule to update
     * @param request   the replacement request
     * @return 200 OK with the updated {@link RuleResponse}, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> updateRule(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody RuleRequest request) {
        RuleResponse response = ruleService.updateRule(principal.getUsername(), id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggles the enabled flag of a rule owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the rule
     * @param body      a JSON object with a single {@code "enabled"} boolean field
     * @return 200 OK with the updated {@link RuleResponse}, or 404 if not found
     */
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<RuleResponse> setEnabled(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        RuleResponse response = ruleService.setEnabled(principal.getUsername(), id, enabled);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a rule owned by the authenticated user.
     *
     * @param principal the authenticated user injected by Spring Security
     * @param id        the primary key of the rule to delete
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        ruleService.deleteRule(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
