# Component Dependency — FR-09: Zeitpläne konfigurieren

## Dependency Matrix

| Component | Depends On | Direction |
|---|---|---|
| `ScheduleController` | `ScheduleService` | → |
| `ScheduleService` | `ScheduleRepository` | → |
| `ScheduleService` | `DeviceRepository` | → |
| `ScheduleService` | `UserRepository` | → |
| `ScheduleService` | `DeviceService` (reused) | → |
| `ScheduleService` | `ActivityLogService` (reused) | → |
| `ScheduleService` | `Quartz Scheduler` | → |
| `ScheduleService` | `ObjectMapper` (Jackson) | → |
| `ScheduleJobExecutor` | `ScheduleService` | → |
| `QuartzConfig` | `DataSource` (Spring) | → |
| `SchedulesComponent` | `ScheduleService` (Angular) | → |
| `SchedulesComponent` | `DeviceService` (Angular, for device dropdown) | → |
| `ScheduleDialogComponent` | `ScheduleService` (Angular) | → |
| `ScheduleDialogComponent` | `DeviceService` (Angular, for device list) | → |
| Device Panel Component | `ScheduleService` (Angular) | → |
| Device Panel Component | `SchedulesComponent` / `ScheduleDialogComponent` | → |

---

## Component Relationship Graph

```
REST Client (Angular)
    │
    ▼
ScheduleController
    │
    ▼
ScheduleService ──────────────────────┐
    │                                 │
    ├──► ScheduleRepository            │
    │       │                         │
    │       ▼                         │
    │    schedules table (PostgreSQL)  │
    │                                 │
    ├──► DeviceRepository             │
    ├──► UserRepository               │
    │                                 │
    ├──► DeviceService ──► devices table
    │                                 │
    ├──► ActivityLogService ──► activity_logs table
    │                                 │
    └──► Quartz Scheduler ────────────┘
              │
              ▼
         Quartz Tables (PostgreSQL)
              │
              ▼  (cron trigger fires)
    ScheduleJobExecutor
              │
              ▼
    ScheduleService.executeSchedule()
```

---

## Data Flow — Schedule Creation

```
1. POST /api/schedules  (Angular → Spring)
2. ScheduleController.createSchedule()
3. ScheduleService.createSchedule()
   a. resolveOwnedDevice() → DeviceRepository
   b. UserRepository → User entity
   c. ScheduleRepository.save() → schedules table
   d. buildCronExpression() → "0 30 07 ? * MON,WED"
   e. Quartz Scheduler.scheduleJob() → Quartz tables
4. Return ScheduleResponse (201 Created)
```

---

## Data Flow — Schedule Execution (Quartz fires)

```
1. Quartz cron trigger fires at configured time
2. ScheduleJobExecutor.execute()
   a. Read scheduleId from JobDataMap
3. ScheduleService.executeSchedule(scheduleId)
   a. ScheduleRepository.findById(scheduleId)
   b. ObjectMapper.readValue(actionPayload) → DeviceStateRequest
   c. DeviceService.updateState(deviceId, request, "Scheduler ({name})")
      → applies state to Device entity
      → broadcasts via WebSocket
      → internally calls ActivityLogService.log() (already wired)
   d. On exception: ActivityLogService.log() with failure message
```

---

## Data Flow — Angular Schedule UI

```
User opens /schedules page
    → SchedulesComponent.loadSchedules()
    → ScheduleService.getSchedules()
    → GET /api/schedules
    → displays in MatTable

User clicks "Add Schedule"
    → SchedulesComponent.openCreateDialog()
    → ScheduleDialogComponent opens (MatDialog)
    → User fills form (name, device dropdown, days, time, action)
    → ScheduleDialogComponent.submit()
    → ScheduleService.createSchedule()
    → POST /api/schedules
    → Dialog closes, table reloads

User clicks toggle on a row
    → SchedulesComponent.toggleEnabled()
    → ScheduleService.setEnabled()
    → PATCH /api/schedules/{id}/enabled

User clicks delete on a row
    → SchedulesComponent.deleteSchedule()
    → ScheduleService.deleteSchedule()
    → DELETE /api/schedules/{id}
```

---

## Communication Patterns

| Pattern | Usage |
|---|---|
| Synchronous REST | All CRUD operations (Angular ↔ Spring) |
| Quartz cron trigger | Asynchronous schedule execution (internal) |
| Spring Security / JWT | All `/api/schedules` endpoints protected |
| Flyway migrations | V6 (schedules table) + V7 (Quartz schema) applied at startup |
| WebSocket broadcast | Reused from `DeviceService.updateState()` — no change needed |
