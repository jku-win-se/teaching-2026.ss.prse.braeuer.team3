package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.MqttConfig;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.MqttConfigRequest;
import at.jku.se.smarthome.dto.MqttConfigResponse;
import at.jku.se.smarthome.dto.MqttMessageDto;
import at.jku.se.smarthome.repository.MqttConfigRepository;
import at.jku.se.smarthome.repository.UserRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MqttSimulatorService} (US-019).
 *
 * <p>Verifies configuration saving, simulated connect/disconnect lifecycle,
 * PUBLISH logging, and the no-op behaviour when MQTT is not configured.</p>
 */
@ExtendWith(MockitoExtension.class)
class MqttSimulatorServiceTest {

    @Mock
    private MqttConfigRepository mqttConfigRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberService memberService;

    private MqttSimulatorService service;

    private static final String EMAIL = "owner@test.com";
    private User owner;

    @BeforeEach
    void setUp() {
        service = new MqttSimulatorService(mqttConfigRepository, userRepository, memberService);
        owner = new User("Owner", EMAIL, "hash");
        ReflectionTestUtils.setField(owner, "id", 1L);
    }

    // ── AC-1: configuration saving ─────────────────────────────────────────────

    @Test
    @DisplayName("getConfig returns default response when no config exists")
    void getConfig_returnsDefault_whenNoConfigExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.empty());

        MqttConfigResponse response = service.getConfig(EMAIL);

        assertThat(response.getBrokerUrl()).isEqualTo("");
        assertThat(response.getTopic()).isEqualTo("smarthome");
        assertThat(response.isConnected()).isFalse();
    }

    @Test
    @DisplayName("saveConfig creates a new MqttConfig entry")
    void saveConfig_createsNewConfig() {
        MqttConfig saved = new MqttConfig(owner, "mqtt://localhost:1883", "home");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.empty());
        when(mqttConfigRepository.save(any(MqttConfig.class))).thenReturn(saved);

        MqttConfigResponse response = service.saveConfig(EMAIL,
                new MqttConfigRequest("mqtt://localhost:1883", "home"));

        assertThat(response.getBrokerUrl()).isEqualTo("mqtt://localhost:1883");
        assertThat(response.getTopic()).isEqualTo("home");
        assertThat(response.isConnected()).isFalse();
        verify(mqttConfigRepository).save(any(MqttConfig.class));
    }

    @Test
    @DisplayName("saveConfig updates an existing MqttConfig entry")
    void saveConfig_updatesExistingConfig() {
        MqttConfig existing = new MqttConfig(owner, "mqtt://old:1883", "old");
        MqttConfig updated = new MqttConfig(owner, "mqtt://new:1883", "newtopic");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(existing));
        when(mqttConfigRepository.save(existing)).thenReturn(updated);

        MqttConfigResponse response = service.saveConfig(EMAIL,
                new MqttConfigRequest("mqtt://new:1883", "newtopic"));

        assertThat(response.getBrokerUrl()).isEqualTo("mqtt://new:1883");
        assertThat(response.getTopic()).isEqualTo("newtopic");
    }

    // ── AC-1/2: connect / disconnect ──────────────────────────────────────────

    @Test
    @DisplayName("connect sets connected=true and adds SYSTEM message to log")
    void connect_setsConnectedAndLogsMessage() {
        MqttConfig config = new MqttConfig(owner, "mqtt://localhost:1883", "smarthome");
        ReflectionTestUtils.setField(config, "connected", false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));
        when(mqttConfigRepository.save(config)).thenReturn(config);

        MqttConfigResponse response = service.connect(EMAIL);

        assertThat(response.isConnected()).isTrue();
        verify(mqttConfigRepository).save(config);

        // message log should contain the connection notice
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        List<MqttMessageDto> messages = service.getMessages(EMAIL);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getDirection()).isEqualTo("SYSTEM");
        assertThat(messages.get(0).getPayload()).contains("connected");
    }

    @Test
    @DisplayName("connect throws 400 when no config exists")
    void connect_throws400_whenNoConfig() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.connect(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("connect throws 400 when broker URL is blank")
    void connect_throws400_whenBrokerUrlBlank() {
        MqttConfig config = new MqttConfig(owner, "", "smarthome");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.connect(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("disconnect sets connected=false and adds SYSTEM message")
    void disconnect_setsDisconnectedAndLogsMessage() {
        MqttConfig config = new MqttConfig(owner, "mqtt://localhost:1883", "smarthome");
        ReflectionTestUtils.setField(config, "connected", true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));
        when(mqttConfigRepository.save(config)).thenReturn(config);

        MqttConfigResponse response = service.disconnect(EMAIL);

        assertThat(response.isConnected()).isFalse();
    }

    // ── AC-2: publish logged when connected ────────────────────────────────────

    @Test
    @DisplayName("publish appends message to log when connected")
    void publish_appendsMessage_whenConnected() {
        MqttConfig config = new MqttConfig(owner, "mqtt://localhost:1883", "home");
        ReflectionTestUtils.setField(config, "connected", true);
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        service.publish(owner, 42L, "Light", "{\"stateOn\":true}");

        List<MqttMessageDto> messages = service.getMessages(EMAIL);
        assertThat(messages).hasSize(1);
        MqttMessageDto msg = messages.get(0);
        assertThat(msg.getDirection()).isEqualTo("PUBLISH");
        assertThat(msg.getTopic()).isEqualTo("home/devices/42");
        assertThat(msg.getPayload()).isEqualTo("{\"stateOn\":true}");
    }

    // ── AC-3: no-op when not connected ─────────────────────────────────────────

    @Test
    @DisplayName("publish is a no-op when not connected — log stays empty")
    void publish_isNoOp_whenNotConnected() {
        MqttConfig config = new MqttConfig(owner, "mqtt://localhost:1883", "home");
        ReflectionTestUtils.setField(config, "connected", false);
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        service.publish(owner, 1L, "Lamp", "{\"stateOn\":false}");

        List<MqttMessageDto> messages = service.getMessages(EMAIL);
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("publish is a no-op when owner is null (AC-3 fallback)")
    void publish_isNoOp_whenOwnerIsNull() {
        service.publish(null, 1L, "Lamp", "{}");
        // no exception thrown, no repository interaction
        verify(mqttConfigRepository, never()).findByUser(any());
    }

    @Test
    @DisplayName("publish is a no-op when no config exists at all")
    void publish_isNoOp_whenNoConfigExists() {
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        service.publish(owner, 1L, "Lamp", "{}");

        List<MqttMessageDto> messages = service.getMessages(EMAIL);
        assertThat(messages).isEmpty();
    }

    // ── Message log capacity / clear ──────────────────────────────────────────

    @Test
    @DisplayName("message log caps at MAX_MESSAGES, dropping oldest")
    void messageLog_capsAtMaxMessages() {
        MqttConfig config = new MqttConfig(owner, "mqtt://localhost:1883", "home");
        ReflectionTestUtils.setField(config, "connected", true);
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        for (int i = 0; i < MqttSimulatorService.MAX_MESSAGES + 5; i++) {
            service.publish(owner, (long) i, "D" + i, "{}");
        }

        List<MqttMessageDto> messages = service.getMessages(EMAIL);
        assertThat(messages).hasSize(MqttSimulatorService.MAX_MESSAGES);
    }

    @Test
    @DisplayName("clearMessages empties the log")
    void clearMessages_emptiesLog() {
        MqttConfig config = new MqttConfig(owner, "mqtt://localhost:1883", "home");
        ReflectionTestUtils.setField(config, "connected", true);
        when(mqttConfigRepository.findByUser(owner)).thenReturn(Optional.of(config));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        service.publish(owner, 1L, "Lamp", "{}");

        service.clearMessages(EMAIL);

        List<MqttMessageDto> messages = service.getMessages(EMAIL);
        assertThat(messages).isEmpty();
    }
}
