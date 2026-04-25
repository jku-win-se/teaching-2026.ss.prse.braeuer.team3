# Code Generation Plan — schedule-backend (FR-09)

## Unit Context
- **Story**: US-010 — recurring time-based device schedules
- **Branch**: `16-fr-09-schedule-backend` (create from main)
- **Root**: `backend/`
- **Project type**: Brownfield — Spring Boot 3.3.5 / Java 21

## Dependencies
- Requires: existing `DeviceService`, `ActivityLogService`, `DeviceRepository`, `UserRepository`
- Provides: REST API at `/api/schedules` consumed by Unit 2 (schedule-frontend)

## Scheduler Decision
**Spring `@Scheduled` polling** — fires at second 0 of every minute (`cron = "0 * * * * *"`).
Replaced Quartz Scheduler (2026-04-25) to reduce complexity (~355 lines removed).
Schedule data persists in the `schedules` DB table (V6) as before; the `enabled` flag controls
whether a schedule participates in polling. No external scheduler state tables needed.

---

## Steps

### Step 1: Create git branch
- [x] `git checkout main && git pull && git checkout -b 16-fr-09-schedule-backend`

### Step 2: Add scheduling support to pom.xml
- [x] ~~Add `spring-boot-starter-quartz`~~ — replaced by Spring `@EnableScheduling` (built-in, no extra dependency)

### Step 3: ~~Add Quartz config to application.yml~~ — removed (not needed for @Scheduled)
- [x] Removed `spring.quartz.*` block from `application.yml`

### Step 4: Flyway V6 — schedules table
- [x] **Create** `backend/src/main/resources/db/migration/V6__create_schedules.sql`

### Step 5: ~~Flyway V7 — Quartz schema~~ — removed
- [x] Deleted `backend/src/main/resources/db/migration/V7__quartz_schema.sql`

### Step 6: Schedule domain entity
- [x] **Create** `backend/src/main/java/at/jku/se/smarthome/domain/Schedule.java`

### Step 7: ScheduleRepository
- [x] **Create** `backend/src/main/java/at/jku/se/smarthome/repository/ScheduleRepository.java`
  - `findByDevice(Device)`
  - `findByEnabledTrueAndHourAndMinute(int hour, int minute)` — used by polling method
  - `findByDeviceIn(List<Device>)`

### Step 8: ScheduleRequest DTO
- [x] **Create** `backend/src/main/java/at/jku/se/smarthome/dto/ScheduleRequest.java`

### Step 9: ScheduleResponse DTO
- [x] **Create** `backend/src/main/java/at/jku/se/smarthome/dto/ScheduleResponse.java`

### Step 10: ~~ScheduleJobExecutor (Quartz Job)~~ — removed
- [x] Deleted `backend/src/main/java/at/jku/se/smarthome/scheduler/ScheduleJobExecutor.java`

### Step 11: ~~QuartzConfig~~ — replaced by @EnableScheduling
- [x] Deleted `backend/src/main/java/at/jku/se/smarthome/config/QuartzConfig.java`
- [x] **Modify** `backend/src/main/java/at/jku/se/smarthome/SmarthomeApplication.java` — add `@EnableScheduling`

### Step 12: ScheduleService
- [x] **Create** `backend/src/main/java/at/jku/se/smarthome/service/ScheduleService.java`
  - `@Scheduled(cron = "0 * * * * *") runDueSchedules()` — polls DB every minute
  - `getSchedules`, `createSchedule`, `updateSchedule`, `setEnabled`, `deleteSchedule`, `executeSchedule`
  - No Quartz dependencies; `enabled` DB flag is the sole on/off control

### Step 13: ScheduleController
- [x] **Create** `backend/src/main/java/at/jku/se/smarthome/controller/ScheduleController.java`

### Step 14: Modify DeviceService
- [x] **Modify** `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java`
  - Add `updateStateAsActor(Long deviceId, DeviceStateRequest, User owner, String actorName)` method
  - ~~`@Lazy ScheduleService` field~~ — removed (no circular dependency with @Scheduled approach)
  - ~~`scheduleService.removeAllJobsForDevice(deviceId)` in `deleteDevice()`~~ — removed (DB cascade handles it)

### Step 15: ScheduleServiceTest
- [x] **Create** `backend/src/test/java/at/jku/se/smarthome/service/ScheduleServiceTest.java`
  - 16 tests covering: getSchedules (2), createSchedule (4), setEnabled (2), deleteSchedule (2),
    runDueSchedules (3), executeSchedule (3)

### Step 16: ScheduleControllerTest
- [x] **Create** `backend/src/test/java/at/jku/se/smarthome/controller/ScheduleControllerTest.java`

### Step 17: Update DeviceServiceTest
- [x] **Modify** `backend/src/test/java/at/jku/se/smarthome/service/DeviceServiceTest.java`
  - ~~`@Mock ScheduleService scheduleService`~~ — removed
  - ~~`ReflectionTestUtils.setField` for scheduleService~~ — removed
  - ~~`scheduleService.removeAllJobsForDevice()` stubs~~ — removed
