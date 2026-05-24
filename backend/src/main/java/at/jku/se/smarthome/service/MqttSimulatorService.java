package at.jku.se.smarthome.service;

import at.jku.se.smarthome.domain.MqttConfig;
import at.jku.se.smarthome.domain.User;
import at.jku.se.smarthome.dto.MqttConfigRequest;
import at.jku.se.smarthome.dto.MqttConfigResponse;
import at.jku.se.smarthome.dto.MqttMessageDto;
import at.jku.se.smarthome.repository.MqttConfigRepository;
import at.jku.se.smarthome.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates MQTT broker connectivity for the SmartHome Orchestrator (US-019).
 *
 * <p>No real MQTT broker is involved. The service maintains:
 * <ul>
 *   <li>A DB-backed {@link MqttConfig} row per user (broker URL, topic, connection flag)</li>
 *   <li>An in-memory rolling message log (max {@value #MAX_MESSAGES} entries) per user,
 *       holding simulated PUBLISH and RECEIVE entries</li>
 * </ul>
 * </p>
 *
 * <p>Acceptance criteria satisfied:
 * <ul>
 *   <li>AC-1: MQTT-Verbindung konfigurierbar (Broker-URL, Topic) — via {@link #saveConfig}</li>
 *   <li>AC-2: Gerätezustände werden über MQTT synchronisiert — via {@link #publish}</li>
 *   <li>AC-3: System bleibt funktionsfähig, wenn MQTT nicht konfiguriert ist — {@link #publish}
 *       is a no-op when not connected</li>
 * </ul>
 * </p>
 */
@Service
public class MqttSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(MqttSimulatorService.class);

    /** Maximum number of simulated messages stored per user in the in-memory log. */
    static final int MAX_MESSAGES = 50;

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private final MqttConfigRepository mqttConfigRepository;
    private final UserRepository userRepository;
    private final MemberService memberService;

    /** In-memory message log: userId → deque of messages (newest at tail). */
    private final Map<Long, Deque<MqttMessageDto>> messageLog = new ConcurrentHashMap<>();

    /**
     * Constructs the simulator with its required dependencies.
     *
     * @param mqttConfigRepository the repository for persisting MQTT configuration
     * @param userRepository       the repository for resolving the authenticated user
     * @param memberService        the service for owner-role enforcement
     */
    public MqttSimulatorService(MqttConfigRepository mqttConfigRepository,
                                 UserRepository userRepository,
                                 MemberService memberService) {
        this.mqttConfigRepository = mqttConfigRepository;
        this.userRepository = userRepository;
        this.memberService = memberService;
    }

    // ── Config management ──────────────────────────────────────────────────────

    /**
     * Returns the current MQTT configuration for the authenticated user, or a
     * default (empty, disconnected) response if no configuration exists yet.
     *
     * @param email the email of the authenticated user
     * @return the current config response (never {@code null})
     */
    @Transactional(readOnly = true)
    public MqttConfigResponse getConfig(String email) {
        User user = resolveUser(email);
        return mqttConfigRepository.findByUser(user)
                .map(c -> new MqttConfigResponse(c.getBrokerUrl(), c.getTopic(), c.isConnected()))
                .orElseGet(() -> new MqttConfigResponse("", "smarthome", false));
    }

    /**
     * Saves (creates or updates) the MQTT broker configuration for the authenticated user.
     *
     * <p>Saving a new configuration while the connection is active does <em>not</em>
     * disconnect the simulator — the caller should call {@link #disconnect} first if
     * needed.</p>
     *
     * @param email   the email of the authenticated owner
     * @param request the new broker URL and topic
     * @return the updated config response
     */
    @Transactional
    public MqttConfigResponse saveConfig(String email, MqttConfigRequest request) {
        memberService.requireOwnerRole(email, "configure MQTT");
        User user = resolveUser(email);
        MqttConfig config = mqttConfigRepository.findByUser(user)
                .orElseGet(() -> new MqttConfig(user, request.getBrokerUrl(), request.getTopic()));
        config.setBrokerUrl(request.getBrokerUrl());
        config.setTopic(request.getTopic());
        MqttConfig saved = mqttConfigRepository.save(config);
        return new MqttConfigResponse(saved.getBrokerUrl(), saved.getTopic(), saved.isConnected());
    }

    // ── Connect / Disconnect ───────────────────────────────────────────────────

    /**
     * Simulates establishing an MQTT connection for the authenticated user.
     *
     * <p>The broker URL must have been saved before calling connect. The method
     * sets {@code connected = true} in the DB, adds a connection notice to the
     * in-memory message log, and returns the updated config.</p>
     *
     * @param email the email of the authenticated owner
     * @return the updated config response with {@code connected = true}
     * @throws ResponseStatusException with status 400 if no config or broker URL is empty
     */
    @Transactional
    public MqttConfigResponse connect(String email) {
        memberService.requireOwnerRole(email, "connect MQTT");
        User user = resolveUser(email);
        MqttConfig config = mqttConfigRepository.findByUser(user)
                .filter(c -> c.getBrokerUrl() != null && !c.getBrokerUrl().isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please save a broker URL before connecting."));
        config.setConnected(true);
        mqttConfigRepository.save(config);
        appendMessage(user.getId(), "SYSTEM", config.getTopic(),
                "{\"event\":\"connected\",\"broker\":\"" + config.getBrokerUrl() + "\"}");
        if (log.isInfoEnabled()) {
            log.info("MQTT simulated connect for user {} to broker {}", email, config.getBrokerUrl());
        }
        return new MqttConfigResponse(config.getBrokerUrl(), config.getTopic(), true);
    }

    /**
     * Simulates disconnecting the MQTT connection for the authenticated user.
     *
     * <p>If no active connection exists the call is silently ignored.</p>
     *
     * @param email the email of the authenticated owner
     * @return the updated config response with {@code connected = false}
     */
    @Transactional
    public MqttConfigResponse disconnect(String email) {
        memberService.requireOwnerRole(email, "disconnect MQTT");
        User user = resolveUser(email);
        mqttConfigRepository.findByUser(user).ifPresent(config -> {
            config.setConnected(false);
            mqttConfigRepository.save(config);
            appendMessage(user.getId(), "SYSTEM", config.getTopic(),
                    "{\"event\":\"disconnected\"}");
        });
        MqttConfigResponse current = getConfig(email);
        return new MqttConfigResponse(current.getBrokerUrl(), current.getTopic(), false);
    }

    // ── Publish (called by DeviceService) ─────────────────────────────────────

    /**
     * Simulates publishing a device state change to the MQTT broker.
     *
     * <p>This method is called by {@link DeviceService} after every state update.
     * It is a no-op when the user has no active MQTT connection (AC-3).</p>
     *
     * @param owner      the user who owns the device
     * @param deviceId   the ID of the updated device
     * @param deviceName the name of the updated device
     * @param payload    the JSON payload representing the new device state
     */
    public void publish(User owner, Long deviceId, String deviceName, String payload) {
        if (owner == null) {
            return;
        }
        mqttConfigRepository.findByUser(owner).ifPresent(config -> {
            if (!config.isConnected()) {
                return;
            }
            String fullTopic = config.getTopic() + "/devices/" + deviceId;
            appendMessage(owner.getId(), "PUBLISH", fullTopic, payload);
            if (log.isDebugEnabled()) {
                log.debug("MQTT PUBLISH {} → {} [{}]", fullTopic, deviceName, payload);
            }
        });
    }

    // ── Message log ────────────────────────────────────────────────────────────

    /**
     * Returns the in-memory simulated message log for the authenticated user,
     * ordered from oldest to newest.
     *
     * @param email the email of the authenticated user
     * @return an ordered list of simulated MQTT messages (may be empty)
     */
    public List<MqttMessageDto> getMessages(String email) {
        User user = resolveUser(email);
        Deque<MqttMessageDto> deque = messageLog.get(user.getId());
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    /**
     * Clears the in-memory message log for the authenticated user.
     *
     * @param email the email of the authenticated user
     */
    public void clearMessages(String email) {
        User user = resolveUser(email);
        messageLog.remove(user.getId());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void appendMessage(Long userId, String direction, String topic, String payload) {
        Deque<MqttMessageDto> deque = messageLog.computeIfAbsent(userId, id -> new ArrayDeque<>());
        MqttMessageDto msg = new MqttMessageDto(
                TS_FMT.format(Instant.now()),
                direction,
                topic,
                payload
        );
        synchronized (deque) {
            deque.addLast(msg);
            while (deque.size() > MAX_MESSAGES) {
                deque.pollFirst();
            }
        }
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
