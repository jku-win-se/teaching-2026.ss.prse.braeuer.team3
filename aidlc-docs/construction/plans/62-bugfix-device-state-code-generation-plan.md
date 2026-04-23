# Code Generation Plan — Bugfix #62: Device State Null-Safety Per Type

## Unit Context

- **Unit Name**: bugfix-device-state
- **Branch**: `62-bugfix-devices-state`
- **Bug Reference**: Issue #62
- **Source Plan**: `localDocs/62-bugfix-device-state-plan.md`
- **Scope**: Backend only — `DeviceResponse.java` + `DeviceService.java`
- **Type**: Brownfield modification (modify existing files, no new files created)

## Stories / Acceptance Criteria

- [ ] **AC-1**: A SWITCH device response contains only `stateOn` — all other state fields are `null`
- [ ] **AC-2**: A DIMMER device response contains `stateOn` and `brightness` — all others `null`
- [ ] **AC-3**: A THERMOSTAT device response contains `stateOn` and `temperature` — all others `null`
- [ ] **AC-4**: A SENSOR device response contains `temperature` and `sensorValue` — all others `null`
- [ ] **AC-5**: A COVER device response contains `stateOn` and `coverPosition` — all others `null`

## Dependencies

- No dependency on other units
- No DB migration required
- No frontend changes required

## Affected Files

| File | Action |
|------|--------|
| `backend/src/main/java/at/jku/se/smarthome/dto/DeviceResponse.java` | **Modify** — change primitive state fields to nullable wrapper types |
| `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java` | **Modify** — add type-aware null filtering in `toResponse()` |
| `backend/src/test/java/at/jku/se/smarthome/service/DeviceServiceTest.java` | **Modify** — add/update test cases for each device type |

---

## Step-by-Step Plan

### Step 1 — Modify `DeviceResponse.java` [x]
Change all state fields from primitive to wrapper types:
- `boolean stateOn` → `Boolean stateOn`
- `int brightness` → `Integer brightness`
- `double temperature` → `Double temperature`
- `double sensorValue` → `Double sensorValue`
- `int coverPosition` → `Integer coverPosition`

Update constructor(s) and getters/setters accordingly.
Ensure Javadoc is present on the class and all public methods.

### Step 2 — Modify `DeviceService.toResponse()` [x]
Replace the unconditional field copy with a `switch` on `device.getType()` that only populates applicable fields per type, leaving all others as `null`. See plan for exact mapping.
Ensure Javadoc on `toResponse()` is updated.
PMD review: no unused variables, no empty blocks, specific exception types.

### Step 3 — Add / Update Unit Tests in `DeviceServiceTest.java` [x]
Add or update test cases verifying:
1. `SWITCH` → only `stateOn` non-null
2. `DIMMER` → only `stateOn` + `brightness` non-null
3. `THERMOSTAT` → only `stateOn` + `temperature` non-null
4. `SENSOR` → only `temperature` + `sensorValue` non-null
5. `COVER` → only `stateOn` + `coverPosition` non-null

### Step 4 — PMD & Javadoc Review [x]
- Verify no PMD violations in modified methods
- Verify Javadoc present on all modified public classes/methods

---

## Completion Criteria

- [ ] Step 1 complete — `DeviceResponse` uses wrapper types
- [ ] Step 2 complete — `toResponse()` is type-aware
- [ ] Step 3 complete — unit tests added/updated
- [ ] Step 4 complete — PMD + Javadoc verified
- [ ] AC-1 through AC-5 satisfied
