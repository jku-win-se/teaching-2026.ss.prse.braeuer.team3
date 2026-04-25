# Requirements — FR-10: Rule Engine (IF-THEN)

## Intent Analysis

| Field | Value |
|---|---|
| User request | Implement IF-THEN rule engine (issue #17) |
| Request type | New Feature |
| Scope | Backend only (frontend stays as mock) |
| Complexity | Moderate — reactive evaluation pipeline, 2 trigger types, REST API |

---

## Decisions from Requirements Analysis

| Question | Decision |
|---|---|
| Trigger types | **THRESHOLD** + **EVENT** only — TIME_BASED removed (covered by FR-09 schedules) |
| EVENT fires when | `stateOn` changes only (turned on / turned off) |
| THRESHOLD operators | `>` (above) and `<` (below) only |
| Action targets | Switch and Shutter only (`stateOn` true/false, `coverPosition` 0/100) |
| Conflict detection | Not in scope |
| Frontend | Backend only — frontend stays as mock for now |

---

## Functional Requirements

### FR-10-01 — Create Rule
A user can create a rule by providing:
- `name` — display name (required, max 100 chars)
- `triggerType` — `THRESHOLD` or `EVENT`
- `triggerDeviceId` — the device whose state triggers the rule
- `triggerOperator` — `GT` (greater than) or `LT` (less than); only for THRESHOLD
- `triggerThresholdValue` — numeric value to compare; only for THRESHOLD
- `actionDeviceId` — the device to control when the rule fires
- `actionValue` — `"true"` / `"false"` for Switch, `"open"` / `"close"` for Shutter
- `enabled` — whether the rule is active (default: true)

### FR-10-02 — THRESHOLD Trigger
When any device state update arrives (`PATCH /state`), the backend evaluates all enabled THRESHOLD rules whose `triggerDevice` matches the updated device. A rule fires if:
- `triggerDevice` is a Sensor
- The sensor's current `sensorValue` or `temperature` satisfies `[value] [operator] [threshold]`
- Operator `GT`: fires if value `>` threshold
- Operator `LT`: fires if value `<` threshold
- Rule fires on every update where the condition is true (not edge-only)

### FR-10-03 — EVENT Trigger
When any device state update arrives and `stateOn` changed, the backend evaluates all enabled EVENT rules whose `triggerDevice` matches. A rule fires if:
- `triggerDevice.stateOn` changed in the current update
- Condition matches the rule (e.g. rule says "when stateOn = true" and device was turned on)

### FR-10-04 — Rule Action Execution
When a rule fires:
- Parse `actionValue` for the action device type:
  - Switch: `"true"` → `stateOn = true`, `"false"` → `stateOn = false`
  - Shutter: `"open"` → `coverPosition = 100`, `"close"` → `coverPosition = 0`
- Call `DeviceService.updateStateAsActor(deviceId, request, owner, actorName)`
  where `actorName = "Rule (" + rule.name + ")"`
- Execution is logged in ActivityLog and broadcast via WebSocket (FR-08 / FR-07)

### FR-10-05 — Get Rules
User can retrieve all their rules via `GET /api/rules`.
Optionally filtered by device: `GET /api/rules?deviceId={id}`.

### FR-10-06 — Update Rule
User can replace a rule via `PUT /api/rules/{id}` (full replacement).

### FR-10-07 — Toggle Enabled
User can activate/deactivate a rule via `PATCH /api/rules/{id}/enabled` with `{ "enabled": true/false }`.

### FR-10-08 — Delete Rule
User can delete a rule via `DELETE /api/rules/{id}`. Returns 204.

### FR-10-09 — Ownership Scoping
All operations are scoped to the authenticated user. Trigger and action devices must belong to the authenticated user. Returns 404 if device not found or not owned.

---

## Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-04 | PMD compliance — no critical/high violations |
| NFR-06 | Javadoc on all public classes and methods in domain, service, repository, controller layers |

---

## Technical Design (Backend)

### New components
| Component | Type | Notes |
|---|---|---|
| `Rule` | JPA Entity | triggerType, triggerDevice, triggerOperator, triggerThresholdValue, actionDevice, actionValue, enabled, user |
| `RuleRepository` | Spring Data | `findByEnabledTrueAndTriggerDevice(Device)` |
| `RuleRequest` | DTO | Request body for POST/PUT |
| `RuleResponse` | DTO | Response body |
| `RuleService` | Service | CRUD + `evaluateRulesForDevice()` |
| `RuleController` | REST Controller | `/api/rules` |
| Flyway migration `V7` | SQL | `CREATE TABLE rules` |

### Modified components
| Component | Change |
|---|---|
| `DeviceService.updateState()` | Call `ruleService.evaluateRulesForDevice(device, request, stateOnChanged)` after saving |
| `DeviceService.updateStateAsActor()` | Same rule evaluation call (rules can also react to schedule-triggered changes) |

### Rule evaluation integration point
```
DeviceService.updateState() / updateStateAsActor()
  → applyStateFields(device, request)
  → deviceRepository.save(device)
  → webSocketHandler.broadcast(...)
  → activityLogService.log(...)
  → ruleService.evaluateRulesForDevice(device, request, stateOnChanged)  ← NEW
```

### Flyway V7 — rules table
```sql
CREATE TABLE rules (
  id               BIGSERIAL PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  trigger_type     VARCHAR(20)  NOT NULL,
  trigger_device_id BIGINT REFERENCES devices(id) ON DELETE CASCADE,
  trigger_operator VARCHAR(5),
  trigger_threshold_value DOUBLE PRECISION,
  action_device_id BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  action_value     VARCHAR(50)  NOT NULL,
  enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
  user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
```
