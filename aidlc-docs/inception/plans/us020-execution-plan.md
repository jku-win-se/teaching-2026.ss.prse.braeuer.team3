# Execution Plan — US-020: Day Simulation (Zeitraffer)

## Summary
Simulate a full 24-hour day in-memory: user picks start conditions per device + day of week, backend evaluates all TIME/THRESHOLD/EVENT rules without touching real device state, returns ordered event timeline. Frontend renders results as a time-lapse event list.

## Units of Work
- **Unit 1 — Backend**: SimulationRequest/Response DTOs, SimulationService, SimulationController, Flyway (none needed — no DB changes), Tests
- **Unit 2 — Frontend**: SimulationService, SimulationComponent, route + nav entry

## Phases

### INCEPTION
- [x] Workspace Detection — REUSE
- [x] Requirements Analysis — us020-requirements.md
- [x] Workflow Planning — this file
- [ ] Functional Design — embedded below

### CONSTRUCTION
- [ ] Code Generation Unit 1: Backend
- [ ] Code Generation Unit 2: Frontend
- [ ] Build and Test

---

## Functional Design

### Backend — Simulation Engine

#### DTOs
```
SimulationRequest
  dayOfWeek: String          // "MONDAY".."SUNDAY"
  startConditions: List<DeviceStartCondition>

DeviceStartCondition
  deviceId: Long
  stateOn: boolean
  brightness: int            // 0-100
  temperature: double
  sensorValue: double
  coverPosition: int         // 0-100

SimulationResponse
  events: List<SimulationEvent>

SimulationEvent
  hour: int                  // 0-23
  minute: int                // 0-59
  deviceId: Long
  deviceName: String
  roomName: String
  actionValue: String        // "true","false","open","close"
  ruleName: String
  ruleId: Long
```

#### SimulationService — Algorithm
```
run(email, request):
  1. Resolve owner user
  2. Load all enabled rules for user from DB (read-only)
  3. Load all devices for user from DB (read-only)
  4. Build in-memory state map: deviceId → SimDeviceState (mutable copy)
  5. Apply startConditions overrides to state map
  6. events = []
  7. For minute in 0..1439:
       hour = minute / 60, min = minute % 60
       timeRules = rules where type==TIME && hour matches && min matches && dayOfWeek in triggerDays
       For each timeRule:
         apply action to state map
         record event
         evaluate cascading THRESHOLD/EVENT rules (BFS, max depth 10 to prevent infinite loops)
  8. Return SimulationResponse(events)
```

#### Key design: no DB writes
- `SimDeviceState` is a plain POJO — never a JPA entity
- `SimulationService` never calls `deviceRepository.save()` or `activityLogService`
- Owner-only (reuse `memberService.requireOwnerRole`)

### Frontend — SimulationComponent
- Route: `/simulation` (owner-only)
- Nav entry: "Simulation" under Automation group, icon: `play_circle`
- Layout:
  - Left card: day-of-week selector + device start-condition list (stateOn toggle per device)
  - Right card: event timeline (scrollable list, grouped by hour)
  - "Run Simulation" button (disabled while loading)
- Service: `SimulationService` → `POST /api/simulation/run`
