# Unit of Work Story Map — FR-09: Zeitpläne konfigurieren

## User Story

| ID | Story | Source |
|---|---|---|
| US-010 | As a user, I want to configure unconditional time-based schedules for device actions, so devices are automatically controlled at defined times | Issue #16 |

## Acceptance Criteria → Unit Mapping

| Acceptance Criterion | Unit(s) | Components |
|---|---|---|
| A schedule with time and action can be created | `schedule-backend` + `schedule-frontend` | `ScheduleService.createSchedule()`, `ScheduleController POST`, `SchedulesComponent`, `ScheduleDialogComponent` |
| The schedule is reliably executed at the configured time | `schedule-backend` | `QuartzConfig`, `ScheduleJobExecutor`, `ScheduleService.executeSchedule()`, Flyway V6+V7 |
| Schedules can be edited and deleted | `schedule-backend` + `schedule-frontend` | `ScheduleService.updateSchedule/deleteSchedule()`, `ScheduleController PUT/DELETE`, `SchedulesComponent`, `ScheduleDialogComponent` |

## Functional Requirements → Unit Mapping

| Requirement | Unit | Key Components |
|---|---|---|
| FR-09.1 Schedule data model | `schedule-backend` | `Schedule` entity, `V6__create_schedules.sql` |
| FR-09.2 Schedule CRUD | `schedule-backend` + `schedule-frontend` | All CRUD components |
| FR-09.3 Enable/disable toggle | `schedule-backend` + `schedule-frontend` | `ScheduleService.setEnabled()`, PATCH endpoint, toggle in table |
| FR-09.4 Scheduled execution (Quartz) | `schedule-backend` | `QuartzConfig`, `ScheduleJobExecutor`, `V7__quartz_schema.sql` |
| FR-09.5 Action specification (DeviceStateRequest) | `schedule-backend` | `ScheduleService.executeSchedule()`, `ObjectMapper` deserialization |
| FR-09.6 Conflict resolution (none needed) | N/A | By design — no code needed |
| FR-09.7 Activity log integration | `schedule-backend` | `ActivityLogService.log()` call in `executeSchedule()` |
| FR-09.8 Global Schedules page | `schedule-frontend` | `SchedulesComponent`, `/schedules` route |
| FR-09.9 Per-device schedule shortcut | `schedule-frontend` | Device panel modification |
| FR-09.10 Schedule form fields | `schedule-frontend` | `ScheduleDialogComponent` reactive form |

## Story Coverage

| Story | Covered By | Status |
|---|---|---|
| US-010 | Unit 1 (`schedule-backend`) + Unit 2 (`schedule-frontend`) | ✅ Fully covered |

All acceptance criteria are addressed across the two units. No stories are unassigned.
