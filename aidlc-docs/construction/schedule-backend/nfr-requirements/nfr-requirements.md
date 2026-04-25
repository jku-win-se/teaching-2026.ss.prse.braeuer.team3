# NFR Requirements — schedule-backend (FR-09)

## NFR-03: Test Coverage (JaCoCo ≥ 75%)

**Requirement**: Project-wide line coverage must remain ≥ 75% after adding FR-09 backend code.

**Target classes requiring tests**:
| Class | Test Class | Min Scenarios |
|---|---|---|
| `ScheduleService` | `ScheduleServiceTest` | create, get, update, setEnabled, delete, executeSchedule (success + failure + orphan), startupRegistration |
| `ScheduleController` | `ScheduleControllerTest` | GET 200, GET 401, POST 201, POST 400, PUT 200, PUT 404, PATCH 200, DELETE 204, DELETE 404, DELETE 401 |
| `ScheduleJobExecutor` | Covered via `ScheduleServiceTest` (mock Quartz context) | execute delegates to service |

**Exemptions**: `QuartzConfig` — Spring configuration, not unit-testable; excluded from coverage target.

---

## NFR-04: PMD Compliance

**Requirement**: 0 critical and 0 high PMD violations. Build fails on violation (`mvn pmd:check`).

**PMD rules most relevant to new code**:
| Rule | Risk Area | Mitigation |
|---|---|---|
| `AvoidCatchingGenericException` | `executeSchedule()` catches `Exception` | Catch `Exception` but re-throw as `JobExecutionException` or log — acceptable for executor |
| `AvoidPrintStackTrace` | Exception handling | Use SLF4J logger, never `e.printStackTrace()` |
| `EmptyCatchBlock` | Orphan-job silent return | Log a warning before returning |
| `UnusedImports` | Generated code | Remove all unused imports |
| `TooManyMethods` | `ScheduleService` has many methods | Acceptable for a service class; PMD threshold typically 10+ |
| `UseVarargs` | Not applicable | N/A |

**PMD-safe pattern for `executeSchedule` exception catch**:
```java
} catch (Exception e) {
    log.warn("Schedule {} execution failed: {}", scheduleId, e.getMessage());
    // log to activity log, do not rethrow (Quartz will not retry)
}
```

---

## NFR-06: Javadoc

**Requirement**: All `public` classes and methods in `domain/`, `service/`, `repository/`, `controller/`, `config/`, `scheduler/` packages must have Javadoc.

**Minimum per element**:
- Class: one descriptive sentence
- Method: one sentence + `@param` for every parameter + `@return` for non-void + `@throws` if documented exceptions
- `private` methods: exempt

**New classes requiring Javadoc**:
- `Schedule` (entity + all public methods/getters/setters)
- `ScheduleRequest` (DTO + all getters/setters)
- `ScheduleResponse` (DTO + all getters/setters)
- `ScheduleRepository` (interface + all declared methods)
- `ScheduleService` (class + all public methods)
- `ScheduleJobExecutor` (class + `execute()`)
- `QuartzConfig` (class + `@Bean` method)
- `ScheduleController` (class + all handler methods)

---

## NFR-01: Security

**Requirement**: All `/api/schedules` endpoints require a valid JWT Bearer token.

**Enforcement**: Existing Spring Security configuration — no changes needed. JWT filter already applied globally. All `@RestController` endpoints are protected unless explicitly permitted.

**Authorization**: Ownership validation inside `ScheduleService` (not at controller layer) — consistent with existing `DeviceService` and `ActivityLogService` patterns.

---

## NFR-05: Reliability

**Requirement**: Scheduled jobs survive application restarts.

**Enforcement**: Quartz `JobStoreTX` persists all job and trigger state to PostgreSQL. On startup, `ScheduleService.registerAllSchedulesOnStartup()` re-registers jobs with `replaceExisting=true` to handle any drift between application schedules table and Quartz tables.

**Misfire handling**: Quartz default misfire threshold (60 seconds). If the application was down when a trigger was due, the job fires once immediately on resume (`MISFIRE_INSTRUCTION_FIRE_NOW` via `CronScheduleBuilder`).

---

## NFR: Logging

**Requirement**: No `System.out.println()` in production code (PMD enforced).

**Implementation**: Use `org.slf4j.Logger` via `LoggerFactory.getLogger(ScheduleService.class)`.

**Log levels**:
- `INFO`: Schedule created, updated, deleted, executed
- `WARN`: Orphan job encountered in `executeSchedule()`, execution failure
- `DEBUG`: Quartz job registration details

---

## NFR: Transactionality

**Requirement**: All DB-mutating service methods must be `@Transactional`. Read-only queries should use `@Transactional(readOnly = true)` for performance.

| Method | Annotation |
|---|---|
| `createSchedule` | `@Transactional` |
| `getSchedules` | `@Transactional(readOnly = true)` |
| `updateSchedule` | `@Transactional` |
| `setEnabled` | `@Transactional` |
| `deleteSchedule` | `@Transactional` |
| `executeSchedule` | `@Transactional` (applies device state) |
| `registerAllSchedulesOnStartup` | `@Transactional(readOnly = true)` |
| `removeAllJobsForDevice` | none (no DB writes — Quartz handles its own transactions) |
