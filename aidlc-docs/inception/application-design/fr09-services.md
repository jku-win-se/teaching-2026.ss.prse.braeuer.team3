# Services — FR-09: Zeitpläne konfigurieren

## Backend Services

### `ScheduleService`
**Type**: Spring `@Service`
**Role**: Primary orchestrator for FR-09. Handles all schedule business logic and Quartz job lifecycle.

#### Responsibilities
1. **CRUD Orchestration**: Validates ownership, persists `Schedule` entities via `ScheduleRepository`, converts to/from DTOs
2. **Quartz Job Lifecycle**: Registers, reschedules, pauses, resumes, and removes Quartz triggers inline with every CRUD operation — no separate manager needed (Q3 answer: A)
3. **Startup Registration**: Re-registers all enabled schedules on application startup so jobs survive restarts
4. **Execution Callback**: `executeSchedule(Long scheduleId)` is the single entry point called by `ScheduleJobExecutor` at trigger time
5. **Activity Logging**: Calls `ActivityLogService.log()` after each execution (success) or with a failure note (exception caught)

#### Dependencies (injected)
| Dependency | Purpose |
|---|---|
| `ScheduleRepository` | Persist and query Schedule entities |
| `DeviceRepository` | Resolve device by ID + ownership check |
| `UserRepository` | Resolve User by email for ownership |
| `DeviceService` | Reuse `updateState()` to apply the scheduled device action |
| `ActivityLogService` | Log execution result |
| `org.quartz.Scheduler` | Register/remove/pause Quartz triggers |
| `ObjectMapper` (Jackson) | Deserialize `actionPayload` JSON → `DeviceStateRequest` |

#### Quartz Integration Pattern
- Each schedule maps to one Quartz `JobDetail` + `CronTrigger`
- `JobKey` = `"schedule-{id}"`, group = `"schedules"`
- `CronTrigger` built from: `hour`, `minute`, `daysOfWeek` → cron expression `"0 {minute} {hour} ? * {days}"`
- `JobDataMap` carries `scheduleId` (Long) so `ScheduleJobExecutor` can look it up
- `JobDetail` is durable + `@DisallowConcurrentExecution` to prevent overlap

---

### `QuartzConfig`
**Type**: Spring `@Configuration`
**Role**: Exposes the Quartz `Scheduler` bean with PostgreSQL-backed `JobStoreTX`.

#### Configuration Points
- `org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX`
- `org.quartz.jobStore.dataSource` = Spring datasource (reuse existing PostgreSQL connection)
- `org.quartz.threadPool.threadCount = 5`
- `org.quartz.scheduler.instanceId = AUTO`
- Quartz schema tables created via Flyway migration (V7 — after schedule table V6)

---

### Existing Services (reused, no modification needed)

| Service | How FR-09 uses it |
|---|---|
| `DeviceService.updateState()` | Called by `ScheduleService.executeSchedule()` to apply the stored action payload |
| `ActivityLogService.log()` | Called after each schedule execution to write the activity log entry |
| `UserDetailsServiceImpl` | JWT auth chain — no change |

---

## Frontend Services

### `ScheduleService` (Angular)
**File**: `frontend/src/app/core/schedule.service.ts`
**Type**: Angular `@Injectable({ providedIn: 'root' })`
**Role**: Thin HTTP facade — wraps all schedule REST calls, attaches JWT via the existing `AuthInterceptor`.

#### Interactions
```
SchedulesComponent
    → ScheduleService (HTTP)
        → GET/POST/PUT/PATCH/DELETE /api/schedules
            → ScheduleController (Spring)
                → ScheduleService (Spring)
```

#### Error Handling
- HTTP errors propagate as `Observable` errors to the calling component
- Components display `MatSnackBar` or similar for user-visible errors (detail in Functional Design)
