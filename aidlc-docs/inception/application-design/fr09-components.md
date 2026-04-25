# Components — FR-09: Zeitpläne konfigurieren

## Backend Components

### 1. `Schedule` (Domain Entity)
**Package**: `at.jku.se.smarthome.domain`
**Responsibility**: JPA entity representing a recurring device schedule. Maps to the `schedules` table.
**Fields**: `id`, `name`, `device` (LAZY ManyToOne), `daysOfWeek` (VARCHAR comma-separated), `hour`, `minute`, `actionPayload` (JSON string), `enabled`

---

### 2. `ScheduleRepository`
**Package**: `at.jku.se.smarthome.repository`
**Responsibility**: Spring Data JPA repository for `Schedule`. Provides device-scoped and all-schedule queries.
**Interfaces**:
- Find all schedules for a given device
- Find all enabled schedules (for startup job registration)
- Find schedule by ID
- Standard CRUD (save, delete)

---

### 3. `ScheduleRequest` (DTO)
**Package**: `at.jku.se.smarthome.dto`
**Responsibility**: Inbound DTO for create and update operations. Carries all user-supplied schedule fields.
**Fields**: `name`, `deviceId`, `daysOfWeek` (List\<String\>), `hour`, `minute`, `actionPayload` (JSON string), `enabled`

---

### 4. `ScheduleResponse` (DTO)
**Package**: `at.jku.se.smarthome.dto`
**Responsibility**: Outbound DTO returned to clients. Enriches the entity with device name and room name.
**Fields**: `id`, `name`, `deviceId`, `deviceName`, `roomName`, `daysOfWeek` (List\<String\>), `hour`, `minute`, `actionPayload`, `enabled`

---

### 5. `ScheduleService`
**Package**: `at.jku.se.smarthome.service`
**Responsibility**: Orchestrates schedule CRUD, Quartz job registration/removal, and owns the execution callback. Central business logic component for FR-09.
**Key behaviours**:
- Create/update/delete schedule → persist to DB + register/reschedule/delete Quartz trigger
- Enable/disable toggle → persist flag + pause/resume Quartz trigger
- On startup: register all enabled schedules as Quartz jobs
- `executeSchedule(Long scheduleId)` — called by Quartz, applies `DeviceStateRequest` via `DeviceService`, logs result via `ActivityLogService`

---

### 6. `ScheduleJobExecutor`
**Package**: `at.jku.se.smarthome.scheduler`
**Responsibility**: Quartz `Job` implementation. Reads `scheduleId` from the `JobDataMap` and delegates to `ScheduleService.executeSchedule()`.
**Interface**: Implements `org.quartz.Job`

---

### 7. `QuartzConfig`
**Package**: `at.jku.se.smarthome.config`
**Responsibility**: Spring `@Configuration` that exposes the Quartz `Scheduler` bean and configures it to use the PostgreSQL `JobStore` (persisted). Sets thread pool and misfire threshold.

---

### 8. `ScheduleController`
**Package**: `at.jku.se.smarthome.controller`
**Responsibility**: REST controller exposing 6 endpoints under `/api/schedules`. All endpoints require JWT authentication.
**Endpoints**:
- `GET /api/schedules` — list all schedules (optionally filtered by `deviceId`)
- `POST /api/schedules` — create a schedule
- `PUT /api/schedules/{id}` — update a schedule
- `PATCH /api/schedules/{id}/enabled` — toggle enabled flag
- `DELETE /api/schedules/{id}` — delete a schedule

---

## Frontend Components

### 9. `ScheduleDto` (Interface)
**File**: `frontend/src/app/core/models.ts` (extend existing)
**Responsibility**: TypeScript interface mirroring `ScheduleResponse`. Used throughout Angular services and components.

---

### 10. `ScheduleRequest` (Interface)
**File**: `frontend/src/app/core/models.ts`
**Responsibility**: TypeScript interface for schedule create/update payload.

---

### 11. `ScheduleService` (Angular)
**File**: `frontend/src/app/core/schedule.service.ts`
**Responsibility**: Angular HTTP service for all schedule API calls. Returns `Observable` wrappers.

---

### 12. `SchedulesComponent`
**File**: `frontend/src/app/features/schedules/schedules.component.ts`
**Responsibility**: Global Schedules page. Displays all schedules in an Angular Material table. Hosts the create/edit dialog and provides enable-toggle and delete actions per row.
**Route**: `/schedules`

---

### 13. `ScheduleDialogComponent`
**File**: `frontend/src/app/features/schedules/schedule-dialog.component.ts`
**Responsibility**: Angular Material dialog for creating and editing a schedule. Contains the reactive form with fields: name, device dropdown, days-of-week checkboxes, time (hour:minute), action payload fields, enabled toggle.
**Opens from**: `SchedulesComponent` (global create) and device panel (device-scoped create)

---

### 14. Device Panel — Schedule Section
**File**: `frontend/src/app/features/devices/` (existing device component)
**Responsibility**: Add a "Schedules" section or button to the existing device detail panel that lists device-specific schedules and opens `ScheduleDialogComponent` pre-filled with that device.
