# Business Rules — schedule-backend (FR-09)

## BR-01: Device Ownership

A user may only create, read, update, delete, or toggle schedules for devices that belong to rooms owned by that user.

**Enforcement**: `ScheduleService.resolveOwnedDevice(userEmail, deviceId)` — loads device, checks `device.room.user.email == userEmail`. Throws `404 Not Found` if not found or not owned (hides existence, consistent with existing service pattern).

---

## BR-02: Schedule Ownership (Read/Update/Delete)

Only the owner of the device a schedule belongs to may update, toggle, or delete that schedule.

**Enforcement**: On any mutating operation, `ScheduleService` loads the `Schedule` by ID, then verifies `schedule.device.room.user.email == userEmail`. Throws `404 Not Found` if mismatch.

---

## BR-03: Days of Week — At Least One Required

A schedule must have at least one day of week selected.

**Enforcement**: `ScheduleService.createSchedule()` and `updateSchedule()` validate `daysOfWeek` is non-null and non-empty. Throws `400 Bad Request` if violated.

**Storage**: Days stored as comma-separated uppercase Java `DayOfWeek` names: `"MONDAY"`, `"TUESDAY"`, ..., `"SUNDAY"`.

---

## BR-04: Hour and Minute Range

- `hour` must be in range 0–23 (inclusive)
- `minute` must be in range 0–59 (inclusive)

**Enforcement**: Validated in `ScheduleService` before persist. Throws `400 Bad Request` if out of range. DB-level CHECK constraints provide an additional safety net (V6 migration).

---

## BR-05: Schedule Name — Required, Max 100 Characters

`name` must be non-null and non-blank, maximum 100 characters.

**Enforcement**: Validated in `ScheduleService`. Throws `400 Bad Request` if violated.

---

## BR-06: Action Payload — Must Be Valid JSON

`actionPayload` must be a valid JSON string deserializable into `DeviceStateRequest`. At least one field in `DeviceStateRequest` must be non-null (otherwise the schedule does nothing useful).

**Enforcement**: `ScheduleService` deserializes `actionPayload` using `ObjectMapper` on create/update. Throws `400 Bad Request` on parse failure.

---

## BR-07: Enable/Disable Toggle

- Setting `enabled = true` registers (or resumes) the Quartz `CronTrigger`.
- Setting `enabled = false` pauses the Quartz `CronTrigger` without removing it.
- The Quartz job persists in the DB regardless of enabled state.

**Enforcement**: `ScheduleService.setEnabled()` calls `scheduler.pauseTrigger()` / `scheduler.resumeTrigger()` after updating the DB flag.

---

## BR-08: Device Deletion Cascade

When a device is deleted, all of its schedules are automatically deleted (DB `ON DELETE CASCADE`). The corresponding Quartz jobs are **not** automatically cleaned up by the cascade — they become orphans.

**Compensation**: `DeviceService.deleteDevice()` must be extended to first call `ScheduleService.removeAllJobsForDevice(Long deviceId)` before the device is deleted, so Quartz triggers are cleaned up before the DB cascade fires.

---

## BR-09: Quartz Job Identity

- `JobKey`: name = `"schedule-{id}"`, group = `"schedules"`
- `TriggerKey`: name = `"trigger-{id}"`, group = `"schedules"`
- Ensures uniqueness; allows targeted reschedule/delete by schedule ID.

---

## BR-10: Cron Expression Format

Quartz cron expression is derived from `hour`, `minute`, `daysOfWeek`:

```
0 {minute} {hour} ? * {DAYS}
```

Where `{DAYS}` is a comma-separated list of Quartz day abbreviations:
`MON,TUE,WED,THU,FRI,SAT,SUN`

Mapping from `DayOfWeek` to Quartz abbreviation:
| Java `DayOfWeek` | Quartz |
|---|---|
| `MONDAY` | `MON` |
| `TUESDAY` | `TUE` |
| `WEDNESDAY` | `WED` |
| `THURSDAY` | `THU` |
| `FRIDAY` | `FRI` |
| `SATURDAY` | `SAT` |
| `SUNDAY` | `SUN` |

---

## BR-11: Startup Re-registration

On application startup, all schedules where `enabled = true` are re-registered as Quartz jobs. Quartz `JobStoreTX` (PostgreSQL-backed) persists jobs across restarts — re-registration uses `scheduleJob()` with `replaceExisting = true` to avoid duplicate job errors.

**Trigger**: `ApplicationRunner` bean in `ScheduleService` or `@PostConstruct` on `ScheduleService`.

---

## BR-12: Execution — Custom Actor Name

When a Quartz job fires `ScheduleService.executeSchedule(scheduleId)`:

1. Load `Schedule` by ID — if not found (deleted between trigger and execution), log warning and return silently.
2. Deserialize `actionPayload` → `DeviceStateRequest`.
3. Call `DeviceService.updateStateAsActor(deviceId, request, owner, actorName)` where:
   - `owner` = `schedule.device.room.user`
   - `actorName` = `"Scheduler (" + schedule.getName() + ")"`
4. The internal method applies state, broadcasts WebSocket, and calls `activityLogService.log()` with the custom actorName.

**On exception**: Catch any `Exception`, call `activityLogService.log(device, owner, "Scheduler (" + schedule.getName() + ")", "Execution failed: " + e.getMessage())`.

---

## BR-13: No Conflict Resolution

If two schedules for the same device fire at the same time, both execute. The last one to complete determines the final device state. No duplicate detection or rejection is performed.

---

## BR-14: Authorization Response — 404 Not 403

When a user attempts to access a schedule that doesn't exist or belongs to a different user's device, respond with `404 Not Found` — not `403 Forbidden`. This hides schedule existence, consistent with the existing pattern in `DeviceService` and `ActivityLogService`.
