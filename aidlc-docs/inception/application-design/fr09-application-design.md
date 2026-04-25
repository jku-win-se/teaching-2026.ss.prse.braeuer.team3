# Application Design — FR-09: Zeitpläne konfigurieren

> Consolidated design document. See linked detail files for full specifications.

---

## Overview

FR-09 adds recurring time-based device schedules to the SmartHome Orchestrator. Users configure schedules via a new Angular Schedules page and a per-device shortcut. The backend uses **Quartz Scheduler** (PostgreSQL-persisted) for reliable execution. Each execution reuses the existing `DeviceService.updateState()` and `ActivityLogService.log()` paths.

---

## New Components

### Backend (`backend/src/main/java/at/jku/se/smarthome/`)

| Component | Type | Package | Purpose |
|---|---|---|---|
| `Schedule` | JPA Entity | `domain` | Schedule data: name, device, days, time, action, enabled |
| `ScheduleRepository` | Spring Data Interface | `repository` | DB queries for schedules |
| `ScheduleRequest` | DTO (inbound) | `dto` | Create/update payload from client |
| `ScheduleResponse` | DTO (outbound) | `dto` | Enriched schedule data returned to client |
| `ScheduleService` | Spring Service | `service` | CRUD + Quartz lifecycle + execution callback |
| `ScheduleJobExecutor` | Quartz Job | `scheduler` | Quartz entry point; delegates to `ScheduleService.executeSchedule()` |
| `QuartzConfig` | Spring Config | `config` | Quartz Scheduler bean with PostgreSQL JobStore |
| `ScheduleController` | REST Controller | `controller` | 5 endpoints under `/api/schedules` |

### Backend — Flyway Migrations

| Migration | Purpose |
|---|---|
| `V6__create_schedules.sql` | `schedules` table |
| `V7__quartz_schema.sql` | Quartz system tables (QRTZ_*) |

### Frontend (`frontend/src/app/`)

| Component | Type | Location | Purpose |
|---|---|---|---|
| `ScheduleDto` | TS Interface | `core/models.ts` | Mirrors `ScheduleResponse` |
| `ScheduleRequest` | TS Interface | `core/models.ts` | Create/update payload |
| `ScheduleService` | Angular Service | `core/schedule.service.ts` | HTTP facade for schedule API |
| `SchedulesComponent` | Routed Component | `features/schedules/` | Global schedule list page (`/schedules`) |
| `ScheduleDialogComponent` | MatDialog Component | `features/schedules/` | Create/edit form as modal |
| Device Panel (existing) | Modified Component | `features/devices/` | Add schedule shortcut section |

---

## Key Design Decisions

| Decision | Choice | Reason |
|---|---|---|
| Days of week storage | VARCHAR comma-separated | Simple, avoids join table |
| Schedule form UI | MatDialog modal | Consistent with existing device add dialog pattern |
| Quartz management | Inside `ScheduleService` | Simpler, avoids extra class |
| Action payload | JSON string (serialized `DeviceStateRequest`) | Flexible, reuses existing DTO |
| Execution actor name | `"Scheduler ({scheduleName})"` | Distinguishes automated vs manual in activity log |
| Quartz persistence | PostgreSQL JobStore | Survives restarts — Quartz tables via Flyway V7 |
| Authorization | Device ownership via Room→User chain | Consistent with existing device access pattern |

---

## Component Interfaces Summary

See `fr09-component-methods.md` for full method signatures.

### `ScheduleService` (key methods)
- `getSchedules(userEmail, deviceId?)` → `List<ScheduleResponse>`
- `createSchedule(userEmail, request)` → `ScheduleResponse`
- `updateSchedule(userEmail, scheduleId, request)` → `ScheduleResponse`
- `setEnabled(userEmail, scheduleId, enabled)` → `ScheduleResponse`
- `deleteSchedule(userEmail, scheduleId)` → `void`
- `executeSchedule(scheduleId)` → `void` (Quartz callback)
- `registerAllSchedulesOnStartup()` → `void`

### `ScheduleController` (endpoints)
```
GET    /api/schedules[?deviceId=]     → 200 List<ScheduleResponse>
POST   /api/schedules                 → 201 ScheduleResponse
PUT    /api/schedules/{id}            → 200 ScheduleResponse
PATCH  /api/schedules/{id}/enabled    → 200 ScheduleResponse
DELETE /api/schedules/{id}            → 204 No Content
```

---

## Execution Flow Summary

**User creates schedule → Quartz fires → Device state applied → Activity logged**

```
POST /api/schedules
  → persist Schedule → register Quartz CronTrigger

[At configured time]
  Quartz fires ScheduleJobExecutor
    → ScheduleService.executeSchedule(id)
      → DeviceService.updateState() → Device state applied + WebSocket broadcast
      → ActivityLogService.log() → Entry created
```

---

## Reused Components (no changes required)

- `DeviceService.updateState()` — execution reuses this unchanged
- `ActivityLogService.log()` — logging reuses this unchanged
- `DeviceWebSocketHandler.broadcast()` — WebSocket broadcast reused via DeviceService
- Angular `AuthInterceptor` — JWT auto-attached to all HTTP calls

---

## Detail Documents
- [`fr09-components.md`](fr09-components.md) — full component descriptions
- [`fr09-component-methods.md`](fr09-component-methods.md) — method signatures
- [`fr09-services.md`](fr09-services.md) — service definitions and orchestration
- [`fr09-component-dependency.md`](fr09-component-dependency.md) — dependency graph and data flows
