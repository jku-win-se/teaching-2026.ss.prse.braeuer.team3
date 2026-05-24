package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Rule;
import at.jku.se.smarthome.domain.TriggerType;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceStartCondition;
import at.jku.se.smarthome.dto.SimulationEvent;
import at.jku.se.smarthome.dto.SimulationRequest;
import at.jku.se.smarthome.dto.SimulationResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * Service that runs a 24-hour day simulation for a user's automation rules (US-020).
 *
 * <p>The simulation is entirely in-memory: it never writes to the database, never
 * triggers WebSocket broadcasts, and never records activity-log entries. Real device
 * states are loaded once as read-only seeds and copied into mutable
 * {@link SimDeviceState} POJOs. The simulation engine walks through all 1440 minutes
 * of the requested day, fires TIME rules at their scheduled minute, and evaluates
 * cascading THRESHOLD/EVENT rules via a bounded BFS (max depth 10) to avoid
 * infinite trigger chains.</p>
 *
 * <p>Implements acceptance criteria for US-020:
 * <ul>
 *   <li>AC-1: Simulation startet mit benutzerdefinierten Startbedingungen</li>
 *   <li>AC-2: Zustandsänderungen werden im Zeitraffer wiedergegeben</li>
 *   <li>AC-3: Simulation beeinflusst das Live-System nicht</li>
 * </ul>
 * </p>
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    /** Maximum cascade depth for THRESHOLD/EVENT chain evaluation (prevents infinite loops). */
    private static final int MAX_CASCADE_DEPTH = 10;

    /** Total number of minutes in a 24-hour day. */
    private static final int MINUTES_PER_DAY = 1440;

    private final RuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;
    private final MemberService memberService;

    /**
     * Constructs a {@code SimulationService} with all required dependencies.
     *
     * @param ruleRepository   the repository used to load rules (read-only)
     * @param deviceRepository the repository used to load devices (read-only)
     * @param memberService    the service used for owner-role enforcement
     */
    public SimulationService(RuleRepository ruleRepository,
                             DeviceRepository deviceRepository,
                             MemberService memberService) {
        this.ruleRepository = ruleRepository;
        this.deviceRepository = deviceRepository;
        this.memberService = memberService;
    }

    /**
     * Runs the day simulation for the authenticated user.
     *
     * <p>Steps:
     * <ol>
     *   <li>Enforce owner role (FR-13)</li>
     *   <li>Parse and validate the requested {@link DayOfWeek}</li>
     *   <li>Load all enabled rules and devices from DB (read-only)</li>
     *   <li>Build in-memory device state map, applying start-condition overrides</li>
     *   <li>Walk minutes 0–1439, firing TIME rules and cascading THRESHOLD/EVENT rules</li>
     *   <li>Return ordered list of {@link SimulationEvent}</li>
     * </ol>
     * </p>
     *
     * @param email   email of the authenticated caller (must be owner)
     * @param request the simulation parameters (day of week + optional start conditions)
     * @return simulation response containing the ordered event timeline
     * @throws ResponseStatusException with status 403 if the caller is not an owner
     * @throws ResponseStatusException with status 400 if {@code dayOfWeek} is missing or invalid
     */
    @Transactional(readOnly = true)
    public SimulationResponse run(String email, SimulationRequest request) {
        memberService.requireOwnerRole(email);
        User owner = memberService.resolveEffectiveOwner(email);

        DayOfWeek dayOfWeek = parseDayOfWeek(request.getDayOfWeek());

        List<Rule> rules = ruleRepository.findByUser(owner).stream()
                .filter(Rule::isEnabled)
                .toList();

        List<Device> devices = deviceRepository.findAllByRoomUserId(owner.getId());

        Map<Long, SimDeviceState> stateMap = buildStateMap(devices, request.getStartConditions());

        List<SimulationEvent> events = simulate(rules, stateMap, dayOfWeek);

        if (log.isInfoEnabled()) {
            log.info("Simulation run by {} for {}: {} events produced", email, dayOfWeek, events.size());
        }
        return new SimulationResponse(events);
    }

    // ── Private: simulation engine ─────────────────────────────────────────────

    private List<SimulationEvent> simulate(List<Rule> rules,
                                           Map<Long, SimDeviceState> stateMap,
                                           DayOfWeek dayOfWeek) {
        List<SimulationEvent> events = new ArrayList<>();

        // Evaluate THRESHOLD rules once against the initial start conditions (minute 0).
        // A sensor may already satisfy its threshold at the start of the day — without this
        // pass those rules would never fire because the main loop only drives TIME rules.
        evaluateThresholdRulesAtStart(rules, stateMap, events);

        for (int totalMinute = 0; totalMinute < MINUTES_PER_DAY; totalMinute++) {
            int hour = totalMinute / 60;
            int minute = totalMinute % 60;

            for (Rule rule : rules) {
                if (rule.getTriggerType() != TriggerType.TIME) {
                    continue;
                }
                if (!matchesTime(rule, hour, minute, dayOfWeek)) {
                    continue;
                }
                SimDeviceState actionState = stateMap.get(rule.getActionDevice().getId());
                if (actionState == null) {
                    continue;
                }
                boolean prevOn = actionState.stateOn;
                applyActionToState(actionState, rule.getActionValue(), rule.getActionDevice().getType());
                boolean stateOnChanged = prevOn != actionState.stateOn;

                recordEvent(events, hour, minute, rule);

                cascadeRules(rules, stateMap, rule.getActionDevice().getId(),
                        actionState, stateOnChanged, hour, minute, events, 0);
            }
        }
        return events;
    }

    /**
     * Evaluates all THRESHOLD rules once against the initial in-memory state (at minute 0).
     *
     * <p>Without this pass, a sensor that already satisfies its threshold at simulation
     * start would never produce an event, because the main loop only drives TIME rules
     * and THRESHOLD rules are otherwise only evaluated reactively when a device state
     * changes during a cascade.</p>
     *
     * <p>Each THRESHOLD rule fires at most once here. Any resulting action device state
     * change is then cascaded normally so chained EVENT rules are also captured.</p>
     *
     * @param rules    all enabled rules for the user
     * @param stateMap current (initial) in-memory device states
     * @param events   mutable list to append fired events to
     */
    private void evaluateThresholdRulesAtStart(List<Rule> rules,
                                               Map<Long, SimDeviceState> stateMap,
                                               List<SimulationEvent> events) {
        for (Rule rule : rules) {
            if (rule.getTriggerType() != TriggerType.THRESHOLD) {
                continue;
            }
            if (rule.getTriggerDevice() == null
                    || rule.getTriggerThresholdValue() == null
                    || rule.getTriggerOperator() == null) {
                continue;
            }
            SimDeviceState triggerState = stateMap.get(rule.getTriggerDevice().getId());
            if (triggerState == null) {
                continue;
            }
            if (!shouldCascadeFire(rule, triggerState, false)) {
                continue;
            }
            SimDeviceState actionState = stateMap.get(rule.getActionDevice().getId());
            if (actionState == null) {
                continue;
            }
            boolean prevOn = actionState.stateOn;
            applyActionToState(actionState, rule.getActionValue(), rule.getActionDevice().getType());
            boolean stateOnChanged = prevOn != actionState.stateOn;

            recordEvent(events, 0, 0, rule);

            cascadeRules(rules, stateMap, rule.getActionDevice().getId(),
                    actionState, stateOnChanged, 0, 0, events, 0);
        }
    }

    /**
     * BFS cascade: when a device state changes, evaluate THRESHOLD and EVENT rules
     * that watch that device. Bounded by {@link #MAX_CASCADE_DEPTH} to prevent
     * infinite trigger chains.
     *
     * @param rules          all enabled rules for the user
     * @param stateMap       current in-memory device states
     * @param changedDeviceId the device whose state just changed
     * @param changedState   the updated state of that device
     * @param stateOnChanged whether the stateOn field changed
     * @param hour           simulated hour
     * @param minute         simulated minute
     * @param events         mutable list to append new events to
     * @param depth          current cascade depth
     */
    private void cascadeRules(List<Rule> rules, Map<Long, SimDeviceState> stateMap,
                               Long changedDeviceId, SimDeviceState changedState,
                               boolean stateOnChanged, int hour, int minute,
                               List<SimulationEvent> events, int depth) {
        if (depth >= MAX_CASCADE_DEPTH) {
            return;
        }

        // BFS queue: [ruleId to fire, deviceId that was changed]
        Queue<Rule> candidates = new ArrayDeque<>();
        for (Rule rule : rules) {
            if (rule.getTriggerType() == TriggerType.TIME) {
                continue;
            }
            if (rule.getTriggerDevice() == null) {
                continue;
            }
            if (!rule.getTriggerDevice().getId().equals(changedDeviceId)) {
                continue;
            }
            if (shouldCascadeFire(rule, changedState, stateOnChanged)) {
                candidates.add(rule);
            }
        }

        for (Rule cascadeRule : candidates) {
            SimDeviceState actionState = stateMap.get(cascadeRule.getActionDevice().getId());
            if (actionState == null) {
                continue;
            }
            boolean prevOn = actionState.stateOn;
            applyActionToState(actionState, cascadeRule.getActionValue(),
                    cascadeRule.getActionDevice().getType());
            boolean cascadeStateOnChanged = prevOn != actionState.stateOn;

            recordEvent(events, hour, minute, cascadeRule);

            cascadeRules(rules, stateMap,
                    cascadeRule.getActionDevice().getId(), actionState,
                    cascadeStateOnChanged, hour, minute,
                    events, depth + 1);
        }
    }

    // ── Private: helpers ───────────────────────────────────────────────────────

    /**
     * Creates a {@link SimulationEvent} from a fired rule and appends it to the event list.
     *
     * <p>Extracted to eliminate copy-paste duplication across the three call sites
     * ({@link #simulate}, {@link #evaluateThresholdRulesAtStart}, {@link #cascadeRules}).</p>
     *
     * @param events mutable list to append the new event to
     * @param hour   simulated hour (0–23)
     * @param minute simulated minute (0–59)
     * @param rule   the rule that just fired
     */
    private void recordEvent(List<SimulationEvent> events, int hour, int minute, Rule rule) {
        Device actionDevice = rule.getActionDevice();
        events.add(new SimulationEvent(
                hour, minute,
                actionDevice.getId(),
                actionDevice.getName(),
                actionDevice.getRoom().getName(),
                rule.getActionValue(),
                rule.getName(),
                rule.getId()
        ));
    }

    private boolean matchesTime(Rule rule, int hour, int minute, DayOfWeek dayOfWeek) {
        if (!Integer.valueOf(hour).equals(rule.getTriggerHour())) {
            return false;
        }
        if (!Integer.valueOf(minute).equals(rule.getTriggerMinute())) {
            return false;
        }
        if (rule.getTriggerDaysOfWeek() == null || rule.getTriggerDaysOfWeek().isBlank()) {
            return false;
        }
        List<String> days = Arrays.asList(rule.getTriggerDaysOfWeek().split(","));
        return days.contains(dayOfWeek.name());
    }

    private boolean shouldCascadeFire(Rule rule, SimDeviceState state, boolean stateOnChanged) {
        if (rule.getTriggerType() == TriggerType.EVENT) {
            return stateOnChanged;
        }
        if (rule.getTriggerType() == TriggerType.THRESHOLD) {
            if (rule.getTriggerThresholdValue() == null || rule.getTriggerOperator() == null) {
                return false;
            }
            double value = state.sensorValue;
            double threshold = rule.getTriggerThresholdValue();
            return switch (rule.getTriggerOperator()) {
                case GT -> value > threshold;
                case LT -> value < threshold;
            };
        }
        return false;
    }

    private void applyActionToState(SimDeviceState state, String actionValue, DeviceType type) {
        if (type == DeviceType.COVER) {
            boolean open = "open".equalsIgnoreCase(actionValue);
            state.stateOn = open;
            state.coverPosition = open ? 100 : 0;
        } else {
            state.stateOn = "true".equalsIgnoreCase(actionValue);
        }
    }

    private Map<Long, SimDeviceState> buildStateMap(List<Device> devices,
                                                     List<DeviceStartCondition> startConditions) {
        Map<Long, SimDeviceState> map = new HashMap<>();
        for (Device device : devices) {
            SimDeviceState s = new SimDeviceState();
            s.stateOn = device.isStateOn();
            s.brightness = device.getBrightness();
            s.temperature = device.getTemperature();
            s.sensorValue = device.getSensorValue();
            s.coverPosition = device.getCoverPosition();
            map.put(device.getId(), s);
        }
        if (startConditions != null) {
            for (DeviceStartCondition cond : startConditions) {
                SimDeviceState s = map.get(cond.getDeviceId());
                if (s != null) {
                    s.stateOn = cond.isStateOn();
                    s.brightness = cond.getBrightness();
                    s.temperature = cond.getTemperature();
                    s.sensorValue = cond.getSensorValue();
                    s.coverPosition = cond.getCoverPosition();
                }
            }
        }
        return map;
    }

    private DayOfWeek parseDayOfWeek(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dayOfWeek is required (e.g. MONDAY).");
        }
        try {
            return DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid dayOfWeek: " + value + ". Use MONDAY..SUNDAY.", e);
        }
    }

    // ── Inner POJO: mutable in-memory device state ─────────────────────────────

    /**
     * Mutable in-memory snapshot of a device's state during the simulation.
     *
     * <p>Never persisted to the database. Used only inside the simulation engine.</p>
     */
    static final class SimDeviceState {
        boolean stateOn;
        int brightness;
        double temperature;
        double sensorValue;
        int coverPosition;
    }
}
