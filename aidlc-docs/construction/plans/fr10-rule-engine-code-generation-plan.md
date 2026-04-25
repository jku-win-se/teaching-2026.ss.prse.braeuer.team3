# Code Generation Plan — FR-10: Rule Engine (IF-THEN)

## Unit Context

- **Feature**: FR-10 — Reactive IF-THEN Rule Engine
- **Scope**: Backend only (frontend stays as mock)
- **Base package**: `at.jku.se.smarthome`
- **Workspace root**: `/Users/davidpuehringer/Projects/PR_SE26`
- **Requirements**: `aidlc-docs/inception/requirements/fr10-requirements.md`

## Stories Implemented

| Story | Requirement | Description |
|-------|-------------|-------------|
| US-011-01 | FR-10-01 | Create a rule |
| US-011-02 | FR-10-02 | THRESHOLD trigger evaluation |
| US-011-03 | FR-10-03 | EVENT trigger evaluation |
| US-011-04 | FR-10-04 | Rule action execution (Switch + Shutter) |
| US-011-05 | FR-10-05 | Get rules (all + by device) |
| US-011-06 | FR-10-06 | Update rule (full replacement) |
| US-011-07 | FR-10-07 | Toggle enabled |
| US-011-08 | FR-10-08 | Delete rule |
| US-011-09 | FR-10-09 | Ownership scoping (404 if not owned) |

## Dependencies

- `DeviceService.updateState()` — integration point for rule evaluation
- `DeviceService.updateStateAsActor()` — used by rules to execute actions (rules do NOT evaluate inside this method to prevent loops)
- `ActivityLogService` — reused for logging rule-triggered actions
- `DeviceWebSocketHandler` — reused for broadcasting rule-triggered state changes
- `DeviceRepository`, `UserRepository` — reused for ownership lookups

---

## Step 1 — Flyway Migration: V7__create_rules_table.sql

- [x] **File**: `backend/src/main/resources/db/migration/V7__create_rules_table.sql`
- [x] **Type**: New file
- **Content**: Creates the `rules` table with all FK constraints and CASCADE deletes

```sql
CREATE TABLE rules (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL,
    trigger_type            VARCHAR(20)  NOT NULL,
    trigger_device_id       BIGINT       NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    trigger_operator        VARCHAR(5),
    trigger_threshold_value DOUBLE PRECISION,
    action_device_id        BIGINT       NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    action_value            VARCHAR(50)  NOT NULL,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    user_id                 BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
```

---

## Step 2 — Domain Enum: TriggerType.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/domain/TriggerType.java`
- [x] **Type**: New file
- **Content**: Enum with values `THRESHOLD` and `EVENT`; Javadoc on class and each constant

---

## Step 3 — Domain Enum: TriggerOperator.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/domain/TriggerOperator.java`
- [x] **Type**: New file
- **Content**: Enum with values `GT` (greater than) and `LT` (less than); Javadoc on class and each constant

---

## Step 4 — Domain Entity: Rule.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/domain/Rule.java`
- [x] **Type**: New file
- **Content**: JPA entity mapping to `rules` table
  - `@Id @GeneratedValue IDENTITY` — `id`
  - `@Column(nullable=false, length=100)` — `name`
  - `@Enumerated(STRING) @Column(nullable=false, length=20)` — `triggerType` (TriggerType)
  - `@ManyToOne(LAZY, optional=false) @JoinColumn(name="trigger_device_id")` — `triggerDevice`
  - `@Enumerated(STRING) @Column(length=5, nullable=true)` — `triggerOperator` (TriggerOperator)
  - `@Column(name="trigger_threshold_value", nullable=true)` — `triggerThresholdValue` (Double)
  - `@ManyToOne(LAZY, optional=false) @JoinColumn(name="action_device_id")` — `actionDevice`
  - `@Column(nullable=false, length=50)` — `actionValue`
  - `@Column(nullable=false)` — `enabled` (boolean, default true)
  - `@ManyToOne(LAZY, optional=false) @JoinColumn(name="user_id")` — `user`
  - Default no-arg constructor required by JPA
  - Getters + setters for all fields with full Javadoc

---

## Step 5 — Repository: RuleRepository.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/repository/RuleRepository.java`
- [x] **Type**: New file
- **Content**: `public interface RuleRepository extends JpaRepository<Rule, Long>` with:
  - `List<Rule> findByUser(User user)` — all rules for a user (for GET /api/rules)
  - `List<Rule> findByEnabledTrueAndTriggerDevice(Device device)` — active rules to evaluate on device state change
  - Javadoc on interface and both methods

---

## Step 6 — DTO: RuleRequest.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/dto/RuleRequest.java`
- [x] **Type**: New file
- **Content**: Plain Java class (no Lombok), with getters and setters, full Javadoc
  - `String name` — display name (required)
  - `TriggerType triggerType` — THRESHOLD or EVENT (required)
  - `Long triggerDeviceId` — id of trigger device (required)
  - `TriggerOperator triggerOperator` — GT or LT (required for THRESHOLD, null for EVENT)
  - `Double triggerThresholdValue` — numeric threshold (required for THRESHOLD, null for EVENT)
  - `Long actionDeviceId` — id of action device (required)
  - `String actionValue` — "true"/"false" for Switch, "open"/"close" for Shutter (required)
  - `Boolean enabled` — defaults to true in service if null

---

## Step 7 — DTO: RuleResponse.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/dto/RuleResponse.java`
- [x] **Type**: New file
- **Content**: Immutable response DTO with all-args constructor, getters only, full Javadoc
  - `Long id`
  - `String name`
  - `TriggerType triggerType`
  - `Long triggerDeviceId`
  - `String triggerDeviceName`
  - `TriggerOperator triggerOperator`
  - `Double triggerThresholdValue`
  - `Long actionDeviceId`
  - `String actionDeviceName`
  - `String actionValue`
  - `boolean enabled`

---

## Step 8 — Service: RuleService.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/service/RuleService.java`
- [x] **Type**: New file
- **Content**: `@Service` class with constructor injection, `@Transactional` on mutating methods, full Javadoc
  - Fields: `RuleRepository`, `DeviceRepository`, `UserRepository`, `DeviceService`
  - **`getRules(String email)`** — returns all rules for the authenticated user
  - **`getRulesByDevice(String email, Long deviceId)`** — returns rules filtered by deviceId (optional query param)
  - **`createRule(String email, RuleRequest req)`** — validates devices owned by user, creates and saves Rule
  - **`updateRule(String email, Long ruleId, RuleRequest req)`** — full replacement, validates ownership
  - **`setEnabled(String email, Long ruleId, boolean enabled)`** — toggles enabled
  - **`deleteRule(String email, Long ruleId)`** — deletes, returns void
  - **`evaluateRulesForDevice(Device device, DeviceStateRequest request, boolean stateOnChanged)`** — core evaluation logic:
    - Loads `findByEnabledTrueAndTriggerDevice(device)`
    - For THRESHOLD rules: reads `device.getSensorValue()` or `device.getTemperature()` (use sensorValue for SENSOR type, temperature otherwise), applies GT/LT comparison, fires if true
    - For EVENT rules: fires only if `stateOnChanged == true` and `request.getStateOn()` matches the configured actionValue interpretation (rule fires when device turned ON if actionValue not "false")
    - On fire: parses `actionValue` → builds `DeviceStateRequest`, calls `deviceService.updateStateAsActor(rule.getActionDevice().getId(), stateReq, rule.getUser(), "Rule (" + rule.getName() + ")")`
  - **`toResponse(Rule rule)`** — private static helper for DTO conversion
  - **`resolveOwnedDevice(User user, Long deviceId)`** — private helper: finds device and verifies room belongs to user (throws 404 if not)

**Rule evaluation design detail:**
```
THRESHOLD:
  double value = (device.getType() == SENSOR) ? device.getSensorValue() : device.getTemperature();
  boolean fires = (operator == GT) ? value > threshold : value < threshold;

EVENT:
  boolean fires = stateOnChanged;

On fire:
  DeviceStateRequest actionReq = buildActionRequest(rule.getActionDevice().getType(), rule.getActionValue());
  deviceService.updateStateAsActor(rule.getActionDevice().getId(), actionReq, rule.getUser(), "Rule (" + rule.getName() + ")");

buildActionRequest:
  SWITCH:  stateOn = "true".equals(actionValue)
  COVER:   stateOn = "open".equals(actionValue), coverPosition = "open".equals(actionValue) ? 100 : 0
```

---

## Step 9 — Controller: RuleController.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/controller/RuleController.java`
- [x] **Type**: New file
- **Content**: `@RestController @RequestMapping("/api/rules")` with constructor injection, full Javadoc
  - `GET /api/rules` — `getRules(Principal)` → 200 + list; optional `?deviceId={id}` query param
  - `POST /api/rules` — `createRule(Principal, @RequestBody RuleRequest)` → 201
  - `PUT /api/rules/{id}` — `updateRule(Principal, @PathVariable Long id, @RequestBody RuleRequest)` → 200
  - `PATCH /api/rules/{id}/enabled` — `setEnabled(Principal, @PathVariable Long id, @RequestBody Map<String,Boolean>)` → 200
  - `DELETE /api/rules/{id}` — `deleteRule(Principal, @PathVariable Long id)` → 204

---

## Step 10 — Service Modification: DeviceService.java

- [x] **File**: `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java`
- [x] **Type**: Modify existing
- **Changes**:
  1. Add `RuleService` field + constructor parameter
  2. In `updateState()`: capture `boolean stateOnChanged = request.getStateOn() != null && request.getStateOn() != device.isStateOn()` **before** `applyStateFields()`, then after `webSocketHandler.broadcastActivityLog(...)` add: `ruleService.evaluateRulesForDevice(device, request, stateOnChanged);`
  3. `updateStateAsActor()` — **no change** (rules must not re-evaluate inside actor-triggered updates to prevent infinite loops)
  4. Update class Javadoc to mention FR-10

---

## Step 11 — Unit Tests: RuleServiceTest.java

- [x] **File**: `backend/src/test/java/at/jku/se/smarthome/service/RuleServiceTest.java`
- [x] **Type**: New file
- **Pattern**: `@ExtendWith(MockitoExtension.class)` with `@Mock` for all dependencies, constructor-inject `RuleService`
- **Test methods**:
  1. `createRule_success` — valid request, owned devices → rule saved, response returned
  2. `createRule_triggerDeviceNotOwned_throws404` — trigger device in another user's room → 404
  3. `createRule_actionDeviceNotOwned_throws404` — action device not owned → 404
  4. `getRules_returnsOnlyOwnedRules` — user has 2 rules → both returned
  5. `updateRule_success` — existing rule replaced with new values
  6. `updateRule_notFound_throws404` — rule id not found → 404
  7. `setEnabled_togglesCorrectly` — rule disabled → enabled
  8. `deleteRule_success` — rule deleted, repository called
  9. `evaluateRules_threshold_GT_fires` — sensor value 30, threshold 25, GT → updateStateAsActor called
  10. `evaluateRules_threshold_GT_noFire` — sensor value 20, threshold 25, GT → updateStateAsActor NOT called
  11. `evaluateRules_threshold_LT_fires` — sensor value 10, threshold 25, LT → fires
  12. `evaluateRules_event_stateOnChanged_fires` — stateOnChanged=true → fires
  13. `evaluateRules_event_noChange_noFire` — stateOnChanged=false → NOT called
  14. `evaluateRules_disabledRule_noFire` — rule disabled → not evaluated (repository returns empty)

---

## Step 12 — Controller Tests: RuleControllerTest.java

- [x] **File**: `backend/src/test/java/at/jku/se/smarthome/controller/RuleControllerTest.java`
- [x] **Type**: New file
- **Pattern**: `@WebMvcTest(RuleController.class)` with `@MockBean RuleService`, `@MockBean JwtUtil`, `@MockBean UserRepository`, `@MockBean JdbcTemplate`, `@WithMockUser`
- **Test methods**:
  1. `getRules_authenticated_returns200` — service returns list → 200 + JSON array
  2. `getRules_byDeviceId_returns200` — `?deviceId=1` passed to service
  3. `createRule_validRequest_returns201` — POST with body → 201
  4. `createRule_serviceThrows404_returns404` — service throws ResponseStatusException 404 → 404
  5. `updateRule_validRequest_returns200` — PUT → 200
  6. `updateRule_notFound_returns404` — service throws 404 → 404
  7. `setEnabled_validRequest_returns200` — PATCH `{"enabled":false}` → 200
  8. `deleteRule_returns204` — DELETE → 204
  9. `deleteRule_notFound_returns404` — service throws 404 → 404
  10. `getRules_unauthenticated_returns401` — no `@WithMockUser` → 401

---

## File Summary

| Step | File | Type |
|------|------|------|
| 1 | `backend/src/main/resources/db/migration/V7__create_rules_table.sql` | New — DB migration |
| 2 | `backend/src/main/java/at/jku/se/smarthome/domain/TriggerType.java` | New — enum |
| 3 | `backend/src/main/java/at/jku/se/smarthome/domain/TriggerOperator.java` | New — enum |
| 4 | `backend/src/main/java/at/jku/se/smarthome/domain/Rule.java` | New — JPA entity |
| 5 | `backend/src/main/java/at/jku/se/smarthome/repository/RuleRepository.java` | New — repository |
| 6 | `backend/src/main/java/at/jku/se/smarthome/dto/RuleRequest.java` | New — DTO |
| 7 | `backend/src/main/java/at/jku/se/smarthome/dto/RuleResponse.java` | New — DTO |
| 8 | `backend/src/main/java/at/jku/se/smarthome/service/RuleService.java` | New — service |
| 9 | `backend/src/main/java/at/jku/se/smarthome/controller/RuleController.java` | New — controller |
| 10 | `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java` | Modify — add RuleService hook |
| 11 | `backend/src/test/java/at/jku/se/smarthome/service/RuleServiceTest.java` | New — unit tests |
| 12 | `backend/src/test/java/at/jku/se/smarthome/controller/RuleControllerTest.java` | New — controller tests |
