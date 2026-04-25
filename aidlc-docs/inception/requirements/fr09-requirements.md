# FR-09: Zeitpläne konfigurieren — Requirements

## Intent Analysis

| Field | Value |
|-------|-------|
| **User Request** | Implement FR-09: Zeitpläne konfigurieren (issue #16) |
| **Request Type** | New Feature |
| **Scope Estimate** | Multiple Components (backend entity/repo/service/scheduler + frontend schedule page + device panel integration) |
| **Complexity Estimate** | Complex (Quartz Scheduler integration, dynamic job registration, dual-entry UI) |

---

## User Story

**US-010**: As a user, I want to configure unconditional time-based schedules for device actions, so devices are automatically controlled at defined times.

### Acceptance Criteria (from issue #16)
1. A schedule with time and action can be created
2. The schedule is reliably executed at the configured time
3. Schedules can be edited and deleted

---

## Functional Requirements

### FR-09.1 — Schedule Data Model
- A schedule has: **name**, **device** (FK), **days of week** (subset of Mon–Sun), **time** (hour + minute), **action** (device state payload), **enabled** flag
- Schedules belong to a **device** — all users with access to a device can view its schedules
- Schedules are **recurring** only (no one-time execution)

### FR-09.2 — Schedule CRUD
- **Create**: User can create a schedule for any of their devices, specifying name, device, days-of-week, time, and action
- **Read**: User can list all schedules for a device, and view a global list of all schedules across all devices
- **Update**: User can edit an existing schedule (all fields including enable/disable toggle)
- **Delete**: User can delete a schedule permanently

### FR-09.3 — Enable/Disable Toggle
- Each schedule has an `enabled` boolean field
- Disabled schedules are stored but **not executed**
- The toggle can be changed without editing the full schedule

### FR-09.4 — Scheduled Execution
- Execution engine: **Quartz Scheduler** (persisted to DB, survives restarts, supports dynamic job registration)
- On application startup, all enabled schedules are registered as Quartz jobs
- When a schedule is created/updated/deleted, the corresponding Quartz job is created/rescheduled/removed immediately at runtime — no restart required
- Execution fires the existing `DeviceService.updateState()` logic to apply the scheduled action
- Timezone: **server timezone** (no per-user timezone configuration)

### FR-09.5 — Action Specification
- The action payload mirrors the existing `updateState` API: any device state field (on/off, brightness, temperature setpoint, mode, etc.)
- The schedulable action is the full device state that should be applied

### FR-09.6 — Conflict Resolution
- No explicit conflict detection: if multiple schedules for the same device fire at the same time, all execute in natural order (last execution wins)

### FR-09.7 — Activity Log Integration
- Each schedule execution creates an **ActivityLog entry** (FR-08)
- If execution succeeds: action is logged with actorName = "Scheduler (schedule name)"
- If execution fails: a failure entry is recorded in the activity log with failure reason

### FR-09.8 — Frontend: Global Schedules Page
- Dedicated "Schedules" page accessible from the main navigation
- Displays all schedules across all devices of the user's devices
- Columns: **Name**, **Device**, **Room**, **Action**, **Days**, **Time**, **Enabled**
- Actions per row: **Edit**, **Delete**, **Toggle enabled**
- "Create Schedule" button opens a creation dialog/form

### FR-09.9 — Frontend: Per-Device Schedule Shortcut
- In the device panel/detail view, a "Schedules" section or button shows schedules for that specific device
- Shortcut to create a new schedule pre-filled with that device

### FR-09.10 — Schedule Form Fields
- **Name**: free text, required
- **Device**: dropdown (user's devices), required
- **Days of week**: multi-select checkboxes (Mon/Tue/Wed/Thu/Fri/Sat/Sun), at least one required
- **Time**: time picker (hour:minute), required
- **Action**: dynamic form matching selected device type's state fields
- **Enabled**: toggle (default: true)

---

## Non-Functional Requirements

### NFR-03: Test Coverage
- New backend classes must maintain project-wide JaCoCo coverage ≥ 75%
- Unit tests required for: `ScheduleService`, `ScheduleController`, `ScheduleJobExecutor`

### NFR-04: PMD Compliance
- All generated Java code must pass PMD check (0 critical, 0 high violations)

### NFR-06: Javadoc
- All public classes and methods in `domain/`, `service/`, `repository/`, `controller/` must have Javadoc

### NFR-01: Security
- All schedule endpoints require JWT authentication
- Users can only manage schedules for devices they own (via their rooms)

### NFR-05: Reliability
- Quartz persists jobs to PostgreSQL — schedules survive application restarts
- Failed job executions are recorded in the activity log

---

## Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scheduling engine | Quartz Scheduler | Persisted to DB, survives restarts, dynamic job registration at runtime |
| Schedule storage | New `schedules` table (Flyway migration) + Quartz tables | Quartz needs its own schema tables |
| Timezone | Server timezone | No per-user timezone requirement |
| Conflict resolution | None — natural last-wins | User-confirmed: acceptable |
| Activity log | Yes — on every execution (success + failure) | Consistent with FR-08 design |

---

## Data Model

### `Schedule` Entity
| Field | Type | Notes |
|-------|------|-------|
| `id` | Long (PK) | Auto-generated |
| `name` | String | User-defined display name |
| `device` | Device (FK, LAZY) | Target device |
| `daysOfWeek` | Set\<DayOfWeek\> | Stored as comma-separated or element collection |
| `hour` | int | 0–23 |
| `minute` | int | 0–59 |
| `actionPayload` | String (JSON) | Serialized device state to apply |
| `enabled` | boolean | Default true |

### `ScheduleDto` / `ScheduleResponse`
- `id`, `name`, `deviceId`, `deviceName`, `roomName`, `daysOfWeek`, `hour`, `minute`, `actionPayload`, `enabled`

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/schedules` | List all schedules (filtered by user's devices) |
| GET | `/api/schedules?deviceId={id}` | List schedules for a specific device |
| POST | `/api/schedules` | Create a new schedule |
| PUT | `/api/schedules/{id}` | Update an existing schedule |
| PATCH | `/api/schedules/{id}/enabled` | Toggle enabled status |
| DELETE | `/api/schedules/{id}` | Delete a schedule |

---

## Out of Scope
- One-time (non-recurring) schedule execution
- User-configurable timezone
- Cron expression input (users configure via time picker + days of week only)
- Push notifications on execution
- Schedule history / execution audit trail beyond activity log
