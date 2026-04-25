# Unit of Work Dependency — FR-09: Zeitpläne konfigurieren

## Dependency Matrix

| Unit | Depends On | Type | Notes |
|---|---|---|---|
| `schedule-backend` | Existing `DeviceService` | Runtime | Reused for state application |
| `schedule-backend` | Existing `ActivityLogService` | Runtime | Reused for execution logging |
| `schedule-backend` | Existing `DeviceRepository` | Runtime | Ownership resolution |
| `schedule-backend` | Existing `UserRepository` | Runtime | User lookup |
| `schedule-backend` | Quartz (new dependency) | Compile/Runtime | Job scheduling |
| `schedule-frontend` | `schedule-backend` (REST API) | HTTP | All 5 endpoints |
| `schedule-frontend` | Existing Angular `DeviceService` | Runtime | Device dropdown in form |
| `schedule-frontend` | Existing Angular Material | Compile | MatTable, MatDialog, etc. |

## Development Sequence

```
[Existing backend services] (no change)
        │
        ▼
schedule-backend ──► REST API available at /api/schedules
        │
        ▼
schedule-frontend ──► Schedules page live at /schedules
```

## Integration Points

| Integration | Contract | Notes |
|---|---|---|
| `schedule-frontend` → `schedule-backend` | REST JSON over HTTP, JWT Bearer | Angular `AuthInterceptor` handles auth |
| `schedule-backend` → Quartz | Quartz Java API | Internal — no network boundary |
| `schedule-backend` → PostgreSQL | JDBC via Spring Data + Quartz JobStoreTX | Shared DB connection pool |
| `schedule-backend` → `DeviceService` | Direct Spring bean call | Same JVM |
| `schedule-backend` → `ActivityLogService` | Direct Spring bean call | Same JVM |

## No Circular Dependencies
- `ScheduleService` → `DeviceService` (one-way, no reverse call)
- `ScheduleService` → `ActivityLogService` (one-way, no reverse call)
- `schedule-frontend` → `schedule-backend` (one-way HTTP, no backend callback to frontend)
