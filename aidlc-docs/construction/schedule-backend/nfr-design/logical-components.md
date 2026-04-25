# Logical Components — schedule-backend (FR-09)

## Component Map

```
┌─────────────────────────────────────────────────────────────────┐
│                        Spring Boot JVM                          │
│                                                                 │
│  REST Layer          Service Layer         Quartz Subsystem     │
│  ┌─────────────┐    ┌──────────────────┐  ┌─────────────────┐  │
│  │ Schedule-   │───▶│ ScheduleService  │◀─│ QuartzConfig    │  │
│  │ Controller  │    │                  │  │ (SchedulerBean) │  │
│  └─────────────┘    │  ┌────────────┐ │  └────────┬────────┘  │
│                     │  │ObjectMapper│ │           │            │
│                     │  └────────────┘ │  ┌────────▼────────┐  │
│                     └──┬──────────────┘  │ScheduleJobExec  │  │
│                        │                 │ (@Quartz Job)   │  │
│                        │                 └─────────────────┘  │
│  Data Layer            │                                       │
│  ┌─────────────────────▼──────────────────────────────────┐   │
│  │  ScheduleRepository │ DeviceRepository │ UserRepository │   │
│  └──────────────────────────────────────────────────────┬─┘   │
│                                                          │      │
│  ┌───────────────────────────────────────────────────────▼─┐  │
│  │  DeviceService (extended: +updateStateAsActor)           │  │
│  │  ActivityLogService (reused)                             │  │
│  │  DeviceWebSocketHandler (reused)                        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────▼──────────────┐
              │         PostgreSQL            │
              │  ┌──────────┐ ┌───────────┐  │
              │  │schedules │ │QRTZ_* (11)│  │
              │  │  (V6)    │ │  (V7)     │  │
              │  └──────────┘ └───────────┘  │
              └──────────────────────────────┘
```

---

## Logical Components Detail

### `QuartzConfig` (Spring `@Configuration`)
**Role**: Infrastructure — wires Quartz into Spring.

**Key beans**:
- `SchedulerFactoryBean` — creates the Quartz `Scheduler`, configures `JdbcJobStore`, sets `SpringBeanJobFactory` for DI support in `ScheduleJobExecutor`
- Properties sourced from `application.properties` (`spring.quartz.*`)

**Schema responsibility**: `spring.quartz.jdbc.initialize-schema=never` — Flyway owns table creation.

---

### `ScheduleJobExecutor` (Quartz `Job`)
**Role**: Bridge between Quartz thread pool and Spring service layer.

**Lifecycle**: Instantiated per execution by Quartz via `SpringBeanJobFactory` (Spring injects dependencies). Annotated `@DisallowConcurrentExecution` — only one instance of the same job runs at a time.

**Single responsibility**: Read `scheduleId` from `JobDataMap`, call `ScheduleService.executeSchedule(scheduleId)`.

---

### Flyway Migrations

| Migration | Tables Created | Trigger |
|---|---|---|
| `V6__create_schedules.sql` | `schedules` | Application startup (Flyway auto-run) |
| `V7__quartz_schema.sql` | `QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`, `QRTZ_CRON_TRIGGERS`, `QRTZ_FIRED_TRIGGERS`, `QRTZ_SCHEDULER_STATE`, `QRTZ_LOCKS`, + 5 more | Application startup (Flyway auto-run) |

**Order matters**: V7 must run after V6. Flyway's version-ordered execution guarantees this.

---

### `ScheduleService` — Dual Role

The service has two logical responsibilities:

1. **CRUD + Quartz lifecycle** (triggered by REST requests):
   - Persists to `schedules` table
   - Registers/removes/pauses Quartz triggers
   - Runs in caller's HTTP thread

2. **Execution callback** (triggered by Quartz thread pool):
   - `executeSchedule(scheduleId)` runs in a Quartz worker thread
   - Must be `@Transactional` — opens a new Spring transaction in the Quartz thread context
   - Spring's `PlatformTransactionManager` handles transaction boundary (Quartz `JdbcJobStore` uses its own connection, separate from the execution transaction)

---

### `application.properties` — Quartz Configuration Block

```properties
# Quartz
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.scheduler.instanceName=SmartHomeScheduler
spring.quartz.properties.org.quartz.threadPool.threadCount=5
spring.quartz.properties.org.quartz.jobStore.isClustered=false
spring.quartz.properties.org.quartz.jobStore.misfireThreshold=60000
```

---

### `DeviceService` — Modification Summary

| What changes | Why |
|---|---|
| Add `ScheduleService scheduleService` constructor parameter | To call `removeAllJobsForDevice()` before device delete |
| Add `updateStateAsActor(Long deviceId, DeviceStateRequest, User owner, String actorName)` | Internal execution path with custom actor name for scheduler |
| `deleteDevice()` calls `scheduleService.removeAllJobsForDevice(device.getId())` before `deviceRepository.delete(device)` | Clean up orphan Quartz jobs before DB cascade fires |

**Circular dependency risk**: `ScheduleService` → `DeviceService` AND `DeviceService` → `ScheduleService`.

**Resolution**: Break the cycle by injecting `ScheduleService` lazily in `DeviceService`:
```java
@Lazy
private final ScheduleService scheduleService;
```
Or, extract `removeAllJobsForDevice()` into a thin `ScheduleCleanupService` that only `DeviceService` depends on, keeping `ScheduleService` free of `DeviceService` and avoiding the cycle.

**Chosen approach**: `@Lazy` injection of `ScheduleService` in `DeviceService` — minimal code change.
