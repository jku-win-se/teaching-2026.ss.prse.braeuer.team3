# Unit of Work — FR-09: Zeitpläne konfigurieren

## Unit 1: `schedule-backend`

**Type**: Backend module within existing Spring Boot monolith
**Location**: `backend/src/main/java/at/jku/se/smarthome/`

### Responsibility
All backend logic for FR-09: domain entity, persistence, business logic, Quartz scheduler integration, REST API, and activity log integration.

### Components
| Component | Package | Description |
|---|---|---|
| `Schedule` | `domain` | JPA entity — schedule data model |
| `ScheduleRepository` | `repository` | Spring Data JPA repository |
| `ScheduleRequest` | `dto` | Inbound DTO (create/update) |
| `ScheduleResponse` | `dto` | Outbound DTO (response) |
| `ScheduleService` | `service` | Business logic + Quartz lifecycle |
| `ScheduleJobExecutor` | `scheduler` | Quartz `Job` implementation |
| `QuartzConfig` | `config` | Quartz Spring configuration |
| `ScheduleController` | `controller` | REST endpoints `/api/schedules` |

### Database Migrations
| File | Purpose |
|---|---|
| `V6__create_schedules.sql` | `schedules` table |
| `V7__quartz_schema.sql` | Quartz system tables (QRTZ_*) |

### Test Classes
| Class | Location |
|---|---|
| `ScheduleServiceTest` | `test/java/.../service/` |
| `ScheduleControllerTest` | `test/java/.../controller/` |

### Dependencies on Existing Code
- `DeviceService.updateState()` — reused for execution (no modification)
- `ActivityLogService.log()` — reused for execution logging (no modification)
- `DeviceRepository`, `UserRepository` — reused for ownership resolution
- Spring Security / JWT — no change

### Deliverable
All files compile, `mvn test` passes, PMD 0 violations, Javadoc complete.

---

## Unit 2: `schedule-frontend`

**Type**: Frontend module within existing Angular SPA
**Location**: `frontend/src/app/`

### Responsibility
Angular UI for schedule management: global schedules list page, create/edit dialog, per-device schedule shortcut, and Angular HTTP service.

### Components
| Component | Location | Description |
|---|---|---|
| `ScheduleDto` (interface) | `core/models.ts` | TypeScript mirror of `ScheduleResponse` |
| `ScheduleRequest` (interface) | `core/models.ts` | TypeScript create/update payload |
| `ScheduleService` | `core/schedule.service.ts` | HTTP facade for schedule API |
| `SchedulesComponent` | `features/schedules/` | Routed page at `/schedules` |
| `ScheduleDialogComponent` | `features/schedules/` | Create/edit MatDialog form |
| Device panel (existing) | `features/devices/` | Add schedule section (modification) |
| Navigation (existing) | app routing/nav | Add "Schedules" nav entry |

### Dependencies on Existing Code
- `AuthInterceptor` — JWT auto-attached (no change)
- Angular Material — MatTable, MatDialog, MatCheckbox, MatTimepicker/MatInput
- Existing `DeviceService` (Angular) — device dropdown in schedule form

### Deliverable
Schedule page functional at `/schedules`, device panel shows device schedules, all Angular unit tests pass.

---

## Development Order

1. **Unit 1 first** (`schedule-backend`) — API must exist before frontend can call it
2. **Unit 2 second** (`schedule-frontend`) — depends on backend endpoints
