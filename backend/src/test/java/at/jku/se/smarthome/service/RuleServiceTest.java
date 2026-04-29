package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.Device;
import at.jku.se.smarthome.domain.DeviceType;
import at.jku.se.smarthome.domain.Room;
import at.jku.se.smarthome.domain.Rule;
import at.jku.se.smarthome.domain.TriggerOperator;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock private RuleRepository ruleRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeviceService deviceService;
    @Mock private DeviceWebSocketHandler wsHandler;

    private RuleService ruleService;

    private User user;
    private Room room;
    private Device sensorDevice;
    private Device switchDevice;
    private Device coverDevice;

    private static final String EMAIL = "user@test.com";

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(ruleRepository, deviceRepository, userRepository, deviceService, wsHandler);

        user = new User("Test User", EMAIL, "hashed");
        ReflectionTestUtils.setField(user, "id", 1L);
        room = new Room(user, "Living Room", "weekend");
        ReflectionTestUtils.setField(room, "id", 2L);

        sensorDevice = new Device(room, "Temperature Sensor", DeviceType.SENSOR);
        ReflectionTestUtils.setField(sensorDevice, "id", 10L);

        switchDevice = new Device(room, "AC", DeviceType.SWITCH);
        ReflectionTestUtils.setField(switchDevice, "id", 11L);

        coverDevice = new Device(room, "Blind", DeviceType.COVER);
        ReflectionTestUtils.setField(coverDevice, "id", 12L);
    }

    // --- createRule ---

    @Test
    void createRule_success_persistsAndReturnsResponse() {
        RuleRequest req = buildThresholdRequest();
        Rule saved = buildThresholdRule();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(sensorDevice));
        when(deviceRepository.findById(11L)).thenReturn(Optional.of(switchDevice));
        when(ruleRepository.save(any(Rule.class))).thenReturn(saved);

        RuleResponse response = ruleService.createRule(EMAIL, req);

        assertThat(response.getName()).isEqualTo("Cool Down");
        assertThat(response.getTriggerType()).isEqualTo(TriggerType.THRESHOLD);
        verify(ruleRepository).save(any(Rule.class));
    }

    @Test
    void createRule_triggerDeviceNotOwned_throws404() {
        RuleRequest req = buildThresholdRequest();
        Device otherDevice = new Device(new Room(new User("Other", "other@test.com", "x"), "Room", "icon"), "Sensor", DeviceType.SENSOR);
        ReflectionTestUtils.setField(otherDevice, "id", 10L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(otherDevice));

        assertThatThrownBy(() -> ruleService.createRule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void createRule_actionDeviceNotOwned_throws404() {
        RuleRequest req = buildThresholdRequest();
        Device otherSwitch = new Device(new Room(new User("Other", "other@test.com", "x"), "Room", "icon"), "Switch", DeviceType.SWITCH);
        ReflectionTestUtils.setField(otherSwitch, "id", 11L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(sensorDevice));
        when(deviceRepository.findById(11L)).thenReturn(Optional.of(otherSwitch));

        assertThatThrownBy(() -> ruleService.createRule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // --- getRules ---

    @Test
    void getRules_noFilter_returnsAllForUser() {
        Rule rule = buildThresholdRule();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByUser(user)).thenReturn(List.of(rule));

        List<RuleResponse> result = ruleService.getRules(EMAIL, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cool Down");
    }

    // --- updateRule ---

    @Test
    void updateRule_success_replacesFields() {
        Rule existing = buildThresholdRule();
        RuleRequest req = buildThresholdRequest();
        req.setName("Updated Rule");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(existing));
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(sensorDevice));
        when(deviceRepository.findById(11L)).thenReturn(Optional.of(switchDevice));
        when(ruleRepository.save(existing)).thenReturn(existing);

        RuleResponse response = ruleService.updateRule(EMAIL, 1L, req);

        assertThat(response.getName()).isEqualTo("Updated Rule");
    }

    @Test
    void updateRule_notFound_throws404() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.updateRule(EMAIL, 99L, buildThresholdRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // --- setEnabled ---

    @Test
    void setEnabled_disablesRule() {
        Rule rule = buildThresholdRule();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(rule)).thenReturn(rule);

        RuleResponse response = ruleService.setEnabled(EMAIL, 1L, false);

        assertThat(response.isEnabled()).isFalse();
    }

    // --- deleteRule ---

    @Test
    void deleteRule_success_callsRepositoryDelete() {
        Rule rule = buildThresholdRule();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(rule));

        ruleService.deleteRule(EMAIL, 1L);

        verify(ruleRepository).delete(rule);
    }

    // --- evaluateRulesForDevice: THRESHOLD ---

    @Test
    void evaluateRules_threshold_GT_fires_whenValueAboveThreshold() {
        Rule rule = buildThresholdRule(); // GT, threshold 25.0
        sensorDevice.setSensorValue(30.0);
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(sensorDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(sensorDevice, new DeviceStateRequest(), false);

        verify(deviceService).updateStateAsActor(eq(11L), any(DeviceStateRequest.class), eq(user), anyString());
    }

    @Test
    void evaluateRules_threshold_GT_noFire_whenValueBelowThreshold() {
        Rule rule = buildThresholdRule(); // GT, threshold 25.0
        sensorDevice.setSensorValue(20.0);
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(sensorDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(sensorDevice, new DeviceStateRequest(), false);

        verify(deviceService, never()).updateStateAsActor(anyLong(), any(), any(), any());
    }

    @Test
    void evaluateRules_threshold_LT_fires_whenValueBelowThreshold() {
        Rule rule = buildThresholdRule();
        rule.setTriggerOperator(TriggerOperator.LT);
        sensorDevice.setSensorValue(10.0);
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(sensorDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(sensorDevice, new DeviceStateRequest(), false);

        verify(deviceService).updateStateAsActor(eq(11L), any(DeviceStateRequest.class), eq(user), anyString());
    }

    // --- evaluateRulesForDevice: EVENT ---

    @Test
    void evaluateRules_event_fires_whenStateOnChanged() {
        Rule rule = buildEventRule();
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(switchDevice)).thenReturn(List.of(rule));

        DeviceStateRequest req = new DeviceStateRequest();
        req.setStateOn(true);
        ruleService.evaluateRulesForDevice(switchDevice, req, true);

        verify(deviceService).updateStateAsActor(eq(11L), any(DeviceStateRequest.class), eq(user), anyString());
    }

    @Test
    void evaluateRules_event_noFire_whenStateOnNotChanged() {
        Rule rule = buildEventRule();
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(switchDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(switchDevice, new DeviceStateRequest(), false);

        verify(deviceService, never()).updateStateAsActor(anyLong(), any(), any(), any());
    }

    @Test
    void evaluateRules_cover_action_setsCorrectCoverPosition() {
        Rule rule = buildThresholdRule();
        rule.setActionDevice(coverDevice);
        rule.setActionValue("open");
        sensorDevice.setSensorValue(30.0);
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(sensorDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(sensorDevice, new DeviceStateRequest(), false);

        ArgumentCaptor<DeviceStateRequest> captor = ArgumentCaptor.forClass(DeviceStateRequest.class);
        verify(deviceService).updateStateAsActor(eq(12L), captor.capture(), eq(user), anyString());
        assertThat(captor.getValue().getCoverPosition()).isEqualTo(100);
        assertThat(captor.getValue().getStateOn()).isTrue();
    }

    // --- createRule: TIME ---

    @Test
    void createRule_time_success_noTriggerDevice() {
        RuleRequest req = buildTimeRequest();
        Rule saved = buildTimeRule();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(deviceRepository.findById(11L)).thenReturn(Optional.of(switchDevice));
        when(ruleRepository.save(any(Rule.class))).thenReturn(saved);

        RuleResponse response = ruleService.createRule(EMAIL, req);

        assertThat(response.getTriggerType()).isEqualTo(TriggerType.TIME);
        assertThat(response.getTriggerHour()).isEqualTo(7);
        assertThat(response.getTriggerMinute()).isEqualTo(30);
        assertThat(response.getTriggerDaysOfWeek()).isEqualTo("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        assertThat(response.getTriggerDeviceId()).isNull();
    }

    @Test
    void createRule_time_missingHour_throws400() {
        RuleRequest req = buildTimeRequest();
        req.setTriggerHour(null);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> ruleService.createRule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void createRule_time_missingDays_throws400() {
        RuleRequest req = buildTimeRequest();
        req.setTriggerDaysOfWeek(null);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> ruleService.createRule(EMAIL, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    // --- evaluateTimeRules ---

    @Test
    void evaluateTimeRules_fires_whenDayMatches() {
        Rule rule = buildTimeRule();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        when(ruleRepository.findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
                TriggerType.TIME, now.getHour(), now.getMinute()))
                .thenReturn(List.of(rule));

        ruleService.evaluateTimeRules();

        // Rule has "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY" — always fires
        verify(deviceService).updateStateAsActor(eq(11L), any(DeviceStateRequest.class), eq(user), anyString());
    }

    @Test
    void evaluateTimeRules_noFire_whenDayNotMatches() {
        Rule rule = buildTimeRule();
        // Set days to a day that is definitely not today: use an impossible day string
        rule.setTriggerDaysOfWeek("NEVERDAY");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        when(ruleRepository.findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
                TriggerType.TIME, now.getHour(), now.getMinute()))
                .thenReturn(List.of(rule));

        ruleService.evaluateTimeRules();

        verify(deviceService, never()).updateStateAsActor(anyLong(), any(), any(), any());
    }

    // --- notifications (US-013) ---

    @Test
    void evaluateRules_threshold_fires_broadcastsSuccessNotification() {
        Rule rule = buildThresholdRule(); // actionValue "true", actionDevice "AC"
        sensorDevice.setSensorValue(30.0);
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(sensorDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(sensorDevice, new DeviceStateRequest(), false);

        ArgumentCaptor<RuleNotificationDto> captor = ArgumentCaptor.forClass(RuleNotificationDto.class);
        verify(wsHandler).broadcastRuleNotification(eq(EMAIL), captor.capture());
        assertThat(captor.getValue().isSuccess()).isTrue();
        assertThat(captor.getValue().getRuleName()).isEqualTo("Cool Down");
        assertThat(captor.getValue().getMessage()).isEqualTo("AC eingeschaltet");
        assertThat(captor.getValue().getMessageType()).isEqualTo("ruleNotification");
    }

    @Test
    void evaluateRules_event_fires_broadcastsSuccessNotification() {
        Rule rule = buildEventRule(); // actionValue "true", actionDevice "AC"
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(switchDevice)).thenReturn(List.of(rule));

        ruleService.evaluateRulesForDevice(switchDevice, new DeviceStateRequest(), true);

        ArgumentCaptor<RuleNotificationDto> captor = ArgumentCaptor.forClass(RuleNotificationDto.class);
        verify(wsHandler).broadcastRuleNotification(eq(EMAIL), captor.capture());
        assertThat(captor.getValue().isSuccess()).isTrue();
    }

    @Test
    void evaluateTimeRules_fires_broadcastsSuccessNotification() {
        Rule rule = buildTimeRule(); // actionValue "true", actionDevice "AC"
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        when(ruleRepository.findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
                TriggerType.TIME, now.getHour(), now.getMinute()))
                .thenReturn(List.of(rule));

        ruleService.evaluateTimeRules();

        ArgumentCaptor<RuleNotificationDto> captor = ArgumentCaptor.forClass(RuleNotificationDto.class);
        verify(wsHandler).broadcastRuleNotification(eq(EMAIL), captor.capture());
        assertThat(captor.getValue().isSuccess()).isTrue();
        assertThat(captor.getValue().getRuleName()).isEqualTo("Morning Lights");
    }

    @Test
    void evaluateRules_executionFails_broadcastsFailureNotification() {
        Rule rule = buildThresholdRule();
        sensorDevice.setSensorValue(30.0);
        when(ruleRepository.findByEnabledTrueAndTriggerDevice(sensorDevice)).thenReturn(List.of(rule));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found."))
                .when(deviceService).updateStateAsActor(anyLong(), any(), any(), anyString());

        ruleService.evaluateRulesForDevice(sensorDevice, new DeviceStateRequest(), false);

        ArgumentCaptor<RuleNotificationDto> captor = ArgumentCaptor.forClass(RuleNotificationDto.class);
        verify(wsHandler).broadcastRuleNotification(eq(EMAIL), captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getMessage()).isEqualTo("Gerät nicht verfügbar");
    }

    @Test
    void evaluateTimeRules_executionFails_broadcastsFailureNotification() {
        Rule rule = buildTimeRule();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        when(ruleRepository.findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
                TriggerType.TIME, now.getHour(), now.getMinute()))
                .thenReturn(List.of(rule));
        doThrow(new RuntimeException("unexpected"))
                .when(deviceService).updateStateAsActor(anyLong(), any(), any(), anyString());

        ruleService.evaluateTimeRules();

        ArgumentCaptor<RuleNotificationDto> captor = ArgumentCaptor.forClass(RuleNotificationDto.class);
        verify(wsHandler).broadcastRuleNotification(eq(EMAIL), captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getMessage()).isEqualTo("Unbekannter Fehler");
    }

    // --- Helpers ---

    private RuleRequest buildThresholdRequest() {
        RuleRequest req = new RuleRequest();
        req.setName("Cool Down");
        req.setTriggerType(TriggerType.THRESHOLD);
        req.setTriggerDeviceId(10L);
        req.setTriggerOperator(TriggerOperator.GT);
        req.setTriggerThresholdValue(25.0);
        req.setActionDeviceId(11L);
        req.setActionValue("true");
        req.setEnabled(true);
        return req;
    }

    private Rule buildThresholdRule() {
        Rule rule = new Rule();
        ReflectionTestUtils.setField(rule, "id", 1L);
        rule.setName("Cool Down");
        rule.setUser(user);
        rule.setTriggerType(TriggerType.THRESHOLD);
        rule.setTriggerDevice(sensorDevice);
        rule.setTriggerOperator(TriggerOperator.GT);
        rule.setTriggerThresholdValue(25.0);
        rule.setActionDevice(switchDevice);
        rule.setActionValue("true");
        rule.setEnabled(true);
        return rule;
    }

    private RuleRequest buildTimeRequest() {
        RuleRequest req = new RuleRequest();
        req.setName("Morning Lights");
        req.setTriggerType(TriggerType.TIME);
        req.setTriggerHour(7);
        req.setTriggerMinute(30);
        req.setTriggerDaysOfWeek("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        req.setActionDeviceId(11L);
        req.setActionValue("true");
        req.setEnabled(true);
        return req;
    }

    private Rule buildTimeRule() {
        Rule rule = new Rule();
        ReflectionTestUtils.setField(rule, "id", 3L);
        rule.setName("Morning Lights");
        rule.setUser(user);
        rule.setTriggerType(TriggerType.TIME);
        rule.setTriggerHour(7);
        rule.setTriggerMinute(30);
        rule.setTriggerDaysOfWeek("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        rule.setActionDevice(switchDevice);
        rule.setActionValue("true");
        rule.setEnabled(true);
        return rule;
    }

    private Rule buildEventRule() {
        Rule rule = new Rule();
        ReflectionTestUtils.setField(rule, "id", 2L);
        rule.setName("On Alert");
        rule.setUser(user);
        rule.setTriggerType(TriggerType.EVENT);
        rule.setTriggerDevice(switchDevice);
        rule.setActionDevice(switchDevice);
        rule.setActionValue("true");
        rule.setEnabled(true);
        return rule;
    }
}
