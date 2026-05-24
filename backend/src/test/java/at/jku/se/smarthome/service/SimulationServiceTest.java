package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.Rule;
import at.jku.se.smarthome.domain.TriggerOperator;
import at.jku.se.smarthome.domain.TriggerType;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.DeviceStartCondition;
import at.jku.se.smarthome.dto.SimulationRequest;
import at.jku.se.smarthome.dto.SimulationResponse;
import at.jku.se.smarthome.repository.DeviceRepository;
import at.jku.se.smarthome.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SimulationService}.
 *
 * <p>Verifies that the simulation engine correctly evaluates TIME rules,
 * cascades THRESHOLD/EVENT rules, respects start conditions, and never
 * writes to the database (US-020).</p>
 */
@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock private RuleRepository ruleRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private MemberService memberService;

    private SimulationService simulationService;

    private User owner;
    private Room room;
    private Device switchDevice;
    private Device sensorDevice;

    private static final String EMAIL = "owner@test.com";

    @BeforeEach
    void setUp() {
        simulationService = new SimulationService(ruleRepository, deviceRepository, memberService);

        owner = new User("Owner", EMAIL, "hashed");
        ReflectionTestUtils.setField(owner, "id", 1L);

        room = new Room(owner, "Living Room", "weekend");
        ReflectionTestUtils.setField(room, "id", 2L);

        switchDevice = new Device(room, "Ceiling Light", DeviceType.SWITCH);
        ReflectionTestUtils.setField(switchDevice, "id", 10L);

        sensorDevice = new Device(room, "Temp Sensor", DeviceType.SENSOR);
        ReflectionTestUtils.setField(sensorDevice, "id", 20L);
    }

    // ── TIME rule fires at correct minute ──────────────────────────────────────

    @Test
    @DisplayName("TIME rule fires at matching hour/minute/day — event recorded")
    void run_timeRuleMatchesDay_eventRecorded() {
        Rule rule = buildTimeRule(7, 0, "MONDAY", switchDevice, "true");

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(rule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().getFirst().getHour()).isEqualTo(7);
        assertThat(response.getEvents().getFirst().getMinute()).isEqualTo(0);
        assertThat(response.getEvents().getFirst().getDeviceName()).isEqualTo("Ceiling Light");
        assertThat(response.getEvents().getFirst().getActionValue()).isEqualTo("true");
    }

    @Test
    @DisplayName("TIME rule with different day — no event produced")
    void run_timeRuleWrongDay_noEvent() {
        Rule rule = buildTimeRule(7, 0, "TUESDAY", switchDevice, "true");

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(rule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).isEmpty();
    }

    @Test
    @DisplayName("Multiple TIME rules fire at different hours — events in order")
    void run_multipleTimeRules_eventsOrdered() {
        Rule rule1 = buildTimeRule(7, 0, "MONDAY", switchDevice, "true");
        Rule rule2 = buildTimeRule(22, 30, "MONDAY", switchDevice, "false");

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(rule1, rule2));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).hasSize(2);
        assertThat(response.getEvents().get(0).getHour()).isEqualTo(7);
        assertThat(response.getEvents().get(1).getHour()).isEqualTo(22);
        assertThat(response.getEvents().get(1).getMinute()).isEqualTo(30);
    }

    // ── THRESHOLD rule fires at simulation start ───────────────────────────────

    @Test
    @DisplayName("THRESHOLD rule fires at start when sensor value already exceeds threshold")
    void run_thresholdAlreadyMet_firesAtMinuteZero() {
        // Sensor starts at 30 — rule fires when sensorValue > 25
        ReflectionTestUtils.setField(sensorDevice, "sensorValue", 30.0);
        Rule thresholdRule = buildThresholdRule(sensorDevice, switchDevice);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(thresholdRule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice, sensorDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().getFirst().getHour()).isEqualTo(0);
        assertThat(response.getEvents().getFirst().getMinute()).isEqualTo(0);
        assertThat(response.getEvents().getFirst().getDeviceName()).isEqualTo("Ceiling Light");
        assertThat(response.getEvents().getFirst().getActionValue()).isEqualTo("true");
    }

    @Test
    @DisplayName("THRESHOLD rule does NOT fire when sensor value is below threshold at start")
    void run_thresholdNotMet_noEventAtStart() {
        // Sensor starts at 20 — rule fires when sensorValue > 25 → should NOT fire
        ReflectionTestUtils.setField(sensorDevice, "sensorValue", 20.0);
        Rule thresholdRule = buildThresholdRule(sensorDevice, switchDevice);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(thresholdRule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice, sensorDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).isEmpty();
    }

    @Test
    @DisplayName("THRESHOLD rule fires via start-condition override — user sets high sensor value")
    void run_thresholdMetViaStartCondition_fires() {
        // Sensor live-state is 10 (below threshold), but user overrides to 30 via start condition
        ReflectionTestUtils.setField(sensorDevice, "sensorValue", 10.0);
        Rule thresholdRule = buildThresholdRule(sensorDevice, switchDevice);

        DeviceStartCondition cond = new DeviceStartCondition();
        cond.setDeviceId(20L); // sensorDevice id
        cond.setSensorValue(30.0); // override above threshold

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(thresholdRule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice, sensorDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");
        req.setStartConditions(List.of(cond));

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().getFirst().getDeviceName()).isEqualTo("Ceiling Light");
    }

    // ── Start conditions override ──────────────────────────────────────────────

    @Test
    @DisplayName("Start condition overrides device's initial state")
    void run_startConditionApplied_overridesLiveState() {
        // Rule fires when sensor value > 25 (threshold)
        Rule thresholdRule = buildThresholdRule(sensorDevice, switchDevice);

        // TIME rule at 08:00 sets sensor value via start condition
        Rule timeRule = buildTimeRule(8, 0, "MONDAY", switchDevice, "true");

        DeviceStartCondition cond = new DeviceStartCondition();
        cond.setDeviceId(10L);
        cond.setStateOn(true); // light starts on

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(timeRule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice, sensorDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");
        req.setStartConditions(List.of(cond));

        SimulationResponse response = simulationService.run(EMAIL, req);

        // timeRule fires — event produced
        assertThat(response.getEvents()).hasSize(1);
    }

    // ── Cascade: EVENT rule ────────────────────────────────────────────────────

    @Test
    @DisplayName("EVENT cascade: when TIME rule turns on switch, EVENT rule triggers")
    void run_timeRuleCausesEventCascade_twoEventsRecorded() {
        Device coverDevice = new Device(room, "Blind", DeviceType.COVER);
        ReflectionTestUtils.setField(coverDevice, "id", 30L);

        // TIME rule turns on the switch at 07:00
        Rule timeRule = buildTimeRule(7, 0, "MONDAY", switchDevice, "true");

        // EVENT rule: when switchDevice turns on → open cover
        Rule eventRule = buildEventRule(switchDevice, coverDevice);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(timeRule, eventRule));
        when(deviceRepository.findAllByRoomUserId(1L))
                .thenReturn(List.of(switchDevice, coverDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).hasSize(2);
        assertThat(response.getEvents().get(0).getDeviceName()).isEqualTo("Ceiling Light");
        assertThat(response.getEvents().get(1).getDeviceName()).isEqualTo("Blind");
        assertThat(response.getEvents().get(1).getActionValue()).isEqualTo("open");
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Null dayOfWeek — throws 400 BAD REQUEST")
    void run_nullDayOfWeek_throws400() {
        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek(null);

        assertThatThrownBy(() -> simulationService.run(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("Invalid dayOfWeek string — throws 400 BAD REQUEST")
    void run_invalidDayOfWeek_throws400() {
        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("FUNDAY");

        assertThatThrownBy(() -> simulationService.run(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("Owner role check fails — 403 propagated")
    void run_memberRole_throws403() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner only"))
                .when(memberService).requireOwnerRole(EMAIL);

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        assertThatThrownBy(() -> simulationService.run(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("Disabled rule is skipped — no event")
    void run_disabledRule_noEvent() {
        Rule rule = buildTimeRule(9, 0, "MONDAY", switchDevice, "true");
        rule.setEnabled(false);

        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of(rule));
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("MONDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).isEmpty();
    }

    @Test
    @DisplayName("No rules — empty event list returned")
    void run_noRules_returnsEmptyList() {
        when(memberService.resolveEffectiveOwner(EMAIL)).thenReturn(owner);
        when(ruleRepository.findByUser(owner)).thenReturn(List.of());
        when(deviceRepository.findAllByRoomUserId(1L)).thenReturn(List.of(switchDevice));

        SimulationRequest req = new SimulationRequest();
        req.setDayOfWeek("FRIDAY");

        SimulationResponse response = simulationService.run(EMAIL, req);

        assertThat(response.getEvents()).isEmpty();
    }

    // ── Builder helpers ────────────────────────────────────────────────────────

    private Rule buildTimeRule(int hour, int minute, String days, Device actionDevice, String actionValue) {
        Rule rule = new Rule();
        ReflectionTestUtils.setField(rule, "id", (long) (Math.random() * 10000));
        rule.setName("TimeRule@" + hour + ":" + minute);
        rule.setTriggerType(TriggerType.TIME);
        rule.setTriggerHour(hour);
        rule.setTriggerMinute(minute);
        rule.setTriggerDaysOfWeek(days);
        rule.setActionDevice(actionDevice);
        rule.setActionValue(actionValue);
        rule.setEnabled(true);
        rule.setUser(owner);
        return rule;
    }

    private Rule buildThresholdRule(Device triggerDevice,
                                    Device actionDevice) {
        Rule rule = new Rule();
        ReflectionTestUtils.setField(rule, "id", (long) (Math.random() * 10000));
        rule.setName("ThresholdRule");
        rule.setTriggerType(TriggerType.THRESHOLD);
        rule.setTriggerDevice(triggerDevice);
        rule.setTriggerOperator(TriggerOperator.GT);
        rule.setTriggerThresholdValue(25.0);
        rule.setActionDevice(actionDevice);
        rule.setActionValue("true");
        rule.setEnabled(true);
        rule.setUser(owner);
        return rule;
    }

    private Rule buildEventRule(Device triggerDevice, Device actionDevice) {
        Rule rule = new Rule();
        ReflectionTestUtils.setField(rule, "id", (long) (Math.random() * 10000));
        rule.setName("EventRule");
        rule.setTriggerType(TriggerType.EVENT);
        rule.setTriggerDevice(triggerDevice);
        rule.setActionDevice(actionDevice);
        rule.setActionValue("open");
        rule.setEnabled(true);
        rule.setUser(owner);
        return rule;
    }
}
