package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Rule;
import at.jku.se.smarthome.domain.TriggerType;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceStateRequest;
import at.jku.se.smarthome.dto.RuleNotificationDto;
import at.jku.se.smarthome.dto.RuleRequest;
import at.jku.se.smarthome.dto.RuleResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RuleRepository;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.websocket.DeviceWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing IF-THEN automation rules and evaluating them reactively
 * after device state changes.
 *
 * <p>Provides CRUD operations for {@link Rule} entities and the core rule
 * evaluation pipeline called by
 * {@link DeviceService#updateState} after every {@code PATCH /state} request.
 * Rule evaluation is intentionally skipped inside
 * {@link DeviceService#updateStateAsActor} to prevent infinite trigger chains.</p>
 *
 * <p>Implements FR-10: Rule Engine (IF-THEN).</p>
 */
@Service
public class RuleService {

    private static final Logger log = LoggerFactory.getLogger(RuleService.class);

    private final RuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final DeviceWebSocketHandler wsHandler;

    /**
     * Constructs a {@code RuleService} with all required dependencies.
     *
     * @param ruleRepository   the repository for rule persistence
     * @param deviceRepository the repository for device lookups and ownership checks
     * @param userRepository   the repository for user lookups
     * @param deviceService    the service used to apply device state when a rule fires
     * @param wsHandler        the WebSocket handler used to push rule notifications to the frontend
     */
    public RuleService(RuleRepository ruleRepository,
                       DeviceRepository deviceRepository,
                       UserRepository userRepository,
                       DeviceService deviceService,
                       DeviceWebSocketHandler wsHandler) {
        this.ruleRepository = ruleRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.deviceService = deviceService;
        this.wsHandler = wsHandler;
    }

    /**
     * Returns all rules owned by the authenticated user, optionally filtered by device.
     *
     * @param email    the email of the authenticated user
     * @param deviceId optional device ID filter; {@code null} returns all rules for the user
     * @return list of rule response DTOs
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 404 if the specified device is not found
     *                                 or not owned by the user
     */
    @Transactional(readOnly = true)
    public List<RuleResponse> getRules(String email, Long deviceId) {
        User user = resolveUser(email);
        List<Rule> rules;
        if (deviceId != null) {
            Device device = resolveOwnedDevice(user, deviceId);
            rules = ruleRepository.findByEnabledTrueAndTriggerDevice(device);
        } else {
            rules = ruleRepository.findByUser(user);
        }
        return rules.stream().map(RuleService::toResponse).toList();
    }

    /**
     * Creates a new IF-THEN rule and persists it.
     *
     * @param email   the email of the authenticated user
     * @param request the rule creation request
     * @return the created rule as a response DTO
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 400 if required TIME fields are missing
     * @throws ResponseStatusException with status 404 if either device is not found or not owned
     */
    @Transactional
    public RuleResponse createRule(String email, RuleRequest request) {
        User user = resolveUser(email);
        Device triggerDevice = resolveTriggerDevice(user, request);
        Device actionDevice = resolveOwnedDevice(user, request.getActionDeviceId());

        Rule rule = new Rule();
        rule.setUser(user);
        applyRequest(rule, request, triggerDevice, actionDevice);

        rule = ruleRepository.save(rule);
        if (log.isInfoEnabled()) {
            log.info("Rule {} created by {}", rule.getId(), email);
        }
        return toResponse(rule);
    }

    /**
     * Fully replaces an existing rule with new values.
     *
     * @param email   the email of the authenticated user
     * @param ruleId  the primary key of the rule to update
     * @param request the replacement request
     * @return the updated rule as a response DTO
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 400 if required TIME fields are missing
     * @throws ResponseStatusException with status 404 if the rule, trigger device,
     *                                 or action device is not found or not owned
     */
    @Transactional
    public RuleResponse updateRule(String email, Long ruleId, RuleRequest request) {
        User user = resolveUser(email);
        Rule rule = resolveOwnedRule(user, ruleId);
        Device triggerDevice = resolveTriggerDevice(user, request);
        Device actionDevice = resolveOwnedDevice(user, request.getActionDeviceId());

        applyRequest(rule, request, triggerDevice, actionDevice);
        return toResponse(ruleRepository.save(rule));
    }

    /**
     * Evaluates all enabled TIME rules whose scheduled hour and minute match the current time.
     * Called once per minute by {@link RuleScheduler}.
     *
     * <p>Only rules whose {@code triggerDaysOfWeek} contains the current day of the week
     * are executed.</p>
     */
    @Transactional
    public void evaluateTimeRules() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String today = now.getDayOfWeek().name();
        List<Rule> candidates = ruleRepository
                .findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
                        TriggerType.TIME, now.getHour(), now.getMinute());
        for (Rule rule : candidates) {
            try {
                List<String> days = Arrays.asList(rule.getTriggerDaysOfWeek().split(","));
                if (days.contains(today)) {
                    executeRule(rule);
                }
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("TIME rule {} evaluation failed: {}", rule.getId(), e.getMessage());
                }
                wsHandler.broadcastRuleNotification(rule.getUser().getEmail(),
                        new RuleNotificationDto(rule.getName(), false, toUserMessage(e)));
            }
        }
    }

    /**
     * Toggles the enabled flag of a rule.
     *
     * @param email   the email of the authenticated user
     * @param ruleId  the primary key of the rule
     * @param enabled the new enabled state
     * @return the updated rule as a response DTO
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 404 if the rule is not found or not owned
     */
    @Transactional
    public RuleResponse setEnabled(String email, Long ruleId, boolean enabled) {
        User user = resolveUser(email);
        Rule rule = resolveOwnedRule(user, ruleId);
        rule.setEnabled(enabled);
        return toResponse(ruleRepository.save(rule));
    }

    /**
     * Deletes a rule.
     *
     * @param email  the email of the authenticated user
     * @param ruleId the primary key of the rule to delete
     * @throws ResponseStatusException with status 401 if the user is not found
     * @throws ResponseStatusException with status 404 if the rule is not found or not owned
     */
    @Transactional
    public void deleteRule(String email, Long ruleId) {
        User user = resolveUser(email);
        Rule rule = resolveOwnedRule(user, ruleId);
        ruleRepository.delete(rule);
        if (log.isInfoEnabled()) {
            log.info("Rule {} deleted by {}", ruleId, email);
        }
    }

    /**
     * Evaluates all enabled rules whose trigger device matches the given device.
     * Called by {@link DeviceService#updateState} after every device state update.
     *
     * <p>THRESHOLD rules fire when the device's numeric sensor value satisfies the
     * configured operator and threshold on every matching state update.
     * EVENT rules fire when {@code stateOnChanged} is {@code true}.</p>
     *
     * <p>This method must only be called from the user-triggered update path, never
     * from {@link DeviceService#updateStateAsActor}, to prevent infinite rule chains.</p>
     *
     * @param device         the device that was just updated
     * @param request        the state update request that was applied
     * @param stateOnChanged {@code true} if the device's {@code stateOn} field changed
     *                       in this update
     */
    @Transactional
    public void evaluateRulesForDevice(Device device, DeviceStateRequest request, boolean stateOnChanged) {
        List<Rule> rules = ruleRepository.findByEnabledTrueAndTriggerDevice(device);
        for (Rule rule : rules) {
            try {
                if (shouldFire(rule, device, stateOnChanged)) {
                    executeRule(rule);
                }
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Rule {} evaluation failed: {}", rule.getId(), e.getMessage());
                }
                wsHandler.broadcastRuleNotification(rule.getUser().getEmail(),
                        new RuleNotificationDto(rule.getName(), false, toUserMessage(e)));
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private boolean shouldFire(Rule rule, Device device, boolean stateOnChanged) {
        if (rule.getTriggerType() == TriggerType.THRESHOLD) {
            double value = (device.getType() == DeviceType.SENSOR)
                    ? device.getSensorValue()
                    : device.getTemperature();
            double threshold = rule.getTriggerThresholdValue();
            return switch (rule.getTriggerOperator()) {
                case GT -> value > threshold;
                case LT -> value < threshold;
            };
        }
        // EVENT
        return stateOnChanged;
    }

    private void executeRule(Rule rule) {
        DeviceStateRequest actionRequest = buildActionRequest(rule);
        String actorName = "Rule (" + rule.getName() + ")";
        deviceService.updateStateAsActor(rule.getActionDevice().getId(), actionRequest, rule.getUser(), actorName);
        if (log.isInfoEnabled()) {
            log.info("Rule {} fired — actor: {}", rule.getId(), actorName);
        }
        String userEmail = rule.getUser().getEmail();
        wsHandler.broadcastRuleNotification(userEmail,
                new RuleNotificationDto(rule.getName(), true, buildSuccessMessage(rule)));
    }

    private String buildSuccessMessage(Rule rule) {
        String deviceName = rule.getActionDevice().getName();
        return switch (rule.getActionValue().toLowerCase()) {
            case "true"  -> deviceName + " eingeschaltet";
            case "false" -> deviceName + " ausgeschaltet";
            case "open"  -> deviceName + " geöffnet";
            case "close" -> deviceName + " geschlossen";
            default      -> rule.getActionValue() + ": " + deviceName;
        };
    }

    private String toUserMessage(Exception e) {
        if (e instanceof ResponseStatusException rse
                && rse.getStatusCode().value() == 404) {
            return "Gerät nicht verfügbar";
        }
        if (e instanceof ResponseStatusException) {
            return "Regelausführung fehlgeschlagen";
        }
        return "Unbekannter Fehler";
    }

    private DeviceStateRequest buildActionRequest(Rule rule) {
        DeviceStateRequest req = new DeviceStateRequest();
        if (rule.getActionDevice().getType() == DeviceType.COVER) {
            boolean open = "open".equalsIgnoreCase(rule.getActionValue());
            req.setStateOn(open);
            req.setCoverPosition(open ? 100 : 0);
        } else {
            req.setStateOn("true".equalsIgnoreCase(rule.getActionValue()));
        }
        return req;
    }

    private Device resolveTriggerDevice(User user, RuleRequest request) {
        if (request.getTriggerType() == TriggerType.TIME) {
            if (request.getTriggerHour() == null || request.getTriggerMinute() == null
                    || request.getTriggerDaysOfWeek() == null
                    || request.getTriggerDaysOfWeek().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "TIME rules require triggerHour, triggerMinute and triggerDaysOfWeek.");
            }
            return null;
        }
        return resolveOwnedDevice(user, request.getTriggerDeviceId());
    }

    private void applyRequest(Rule rule, RuleRequest request, Device triggerDevice, Device actionDevice) {
        rule.setName(request.getName());
        rule.setTriggerType(request.getTriggerType());
        if (request.getTriggerType() == TriggerType.TIME) {
            rule.setTriggerDevice(null);
            rule.setTriggerHour(request.getTriggerHour());
            rule.setTriggerMinute(request.getTriggerMinute());
            rule.setTriggerDaysOfWeek(request.getTriggerDaysOfWeek());
            rule.setTriggerOperator(null);
            rule.setTriggerThresholdValue(null);
        } else {
            rule.setTriggerDevice(triggerDevice);
            rule.setTriggerHour(null);
            rule.setTriggerMinute(null);
            rule.setTriggerDaysOfWeek(null);
            rule.setTriggerOperator(request.getTriggerOperator());
            rule.setTriggerThresholdValue(request.getTriggerThresholdValue());
        }
        rule.setActionDevice(actionDevice);
        rule.setActionValue(request.getActionValue());
        rule.setEnabled(request.getEnabled() == null || request.getEnabled());
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private Device resolveOwnedDevice(User user, Long deviceId) {
        return deviceRepository.findById(deviceId)
                .filter(d -> Objects.equals(d.getRoom().getUser().getId(), user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."));
    }

    private Rule resolveOwnedRule(User user, Long ruleId) {
        return ruleRepository.findByIdAndUser(ruleId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found."));
    }

    private static RuleResponse toResponse(Rule rule) {
        Device trigger = rule.getTriggerDevice();
        return new RuleResponse(
                rule.getId(),
                rule.getName(),
                rule.isEnabled(),
                rule.getTriggerType(),
                trigger != null ? trigger.getId() : null,
                trigger != null ? trigger.getName() : null,
                rule.getTriggerOperator(),
                rule.getTriggerThresholdValue(),
                rule.getTriggerHour(),
                rule.getTriggerMinute(),
                rule.getTriggerDaysOfWeek(),
                rule.getActionDevice().getId(),
                rule.getActionDevice().getName(),
                rule.getActionValue()
        );
    }
}
