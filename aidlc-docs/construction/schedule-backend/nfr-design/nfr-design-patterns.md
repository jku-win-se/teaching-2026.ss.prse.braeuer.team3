# NFR Design Patterns — schedule-backend (FR-09)

## Pattern 1: Persistent Job Store (Reliability)

**NFR addressed**: NFR-05 — scheduled jobs survive application restarts.

**Pattern**: Quartz `JdbcJobStore` with PostgreSQL.

**Implementation**:
- All `JobDetail` and `CronTrigger` objects are persisted in Quartz system tables (V7 migration)
- `spring.quartz.job-store-type=jdbc` enables database-backed persistence
- On restart, Quartz reloads all persisted jobs automatically — no data loss
- `registerAllSchedulesOnStartup()` re-registers application-side schedules with `replaceExisting=true` to synchronize the `schedules` table with the Quartz store

**Misfire pattern**: `CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionFireAndProceed()` — fires once immediately if the server was down when a trigger was due, then resumes normal schedule.

---

## Pattern 2: Strategy — Job Execution Delegation

**NFR addressed**: NFR-04 (PMD maintainability), NFR-06 (Javadoc), NFR-03 (testability).

**Pattern**: Thin `ScheduleJobExecutor` (Quartz `Job`) delegates all logic to `ScheduleService.executeSchedule()`.

**Rationale**:
- `ScheduleJobExecutor` contains no business logic — only reads `scheduleId` from `JobDataMap` and calls the service
- All business logic stays in the Spring-managed `ScheduleService`, which is easily unit-testable with mocks
- `@DisallowConcurrentExecution` annotation prevents the same job from running in parallel if a previous execution is still running

**Test strategy**: `ScheduleJobExecutor` is tested by verifying that `execute()` calls `scheduleService.executeSchedule(id)` — no need to test business logic in the executor.

---

## Pattern 3: Fail-Safe Execution (Resilience)

**NFR addressed**: NFR-05 (reliability), NFR-04 (no empty catch blocks).

**Pattern**: Catch-and-log in `executeSchedule()` — execution failures are absorbed and recorded, never propagated back to Quartz as `JobExecutionException` (which would cause Quartz to retry and potentially flood the activity log).

```java
try {
    deviceService.updateStateAsActor(deviceId, request, owner, actorName);
} catch (Exception e) {
    log.warn("Schedule {} execution failed: {}", scheduleId, e.getMessage());
    activityLogService.log(device, owner, actorName, "Execution failed: " + e.getMessage());
}
```

**Orphan guard**: If `scheduleRepository.findById(scheduleId)` returns empty (schedule deleted between trigger registration and firing), log a warning at `WARN` level and return without action — no exception thrown, no activity log entry (device is gone or schedule was cleaned up).

---

## Pattern 4: Template Method — `updateStateAsActor` (Reuse)

**NFR addressed**: NFR-03 (coverage), NFR-04 (no duplication).

**Pattern**: New internal method in `DeviceService` follows the same structure as `updateState()` but skips the ownership lookup and accepts a caller-supplied `actorName`.

**Structure**:
```
updateStateAsActor(deviceId, request, owner, actorName):
  1. load Device
  2. apply state fields (same null-check pattern as updateState)
  3. save
  4. WebSocket broadcast
  5. build action description
  6. activityLogService.log(device, owner, actorName, action)
  7. return DeviceResponse
```

This keeps the single path for device state mutation in `DeviceService` and avoids duplicating the state-application and broadcast logic in `ScheduleService`.

---

## Pattern 5: Guard Clause — Ownership Validation

**NFR addressed**: NFR-01 (security), NFR-04 (PMD — early return preferred over nested ifs).

**Pattern**: Each mutating service method starts with an ownership check that throws `404 Not Found` immediately if the resource is not found or not owned. No business logic executes unless ownership is confirmed.

```java
Schedule schedule = scheduleRepository.findById(scheduleId)
    .filter(s -> s.getDevice().getRoom().getUser().getEmail().equals(userEmail))
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found."));
```

Consistent with the existing pattern in `ActivityLogService.getLogs()` and `DeviceService`.

---

## Pattern 6: SLF4J Structured Logging

**NFR addressed**: NFR-04 (no `System.out.println`).

**Pattern**: All log statements use parameterized SLF4J logging.

```java
private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

log.info("Schedule {} created for device {}", schedule.getId(), schedule.getDevice().getId());
log.warn("Schedule {} not found during execution — skipping", scheduleId);
log.warn("Schedule {} execution failed: {}", scheduleId, e.getMessage());
```

No string concatenation in log calls (avoids unnecessary object creation when log level is disabled).
