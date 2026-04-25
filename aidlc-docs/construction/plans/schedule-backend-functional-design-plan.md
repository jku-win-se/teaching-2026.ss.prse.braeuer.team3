# Functional Design Plan — schedule-backend (FR-09)

## Checklist
- [x] Generate `business-logic-model.md`
- [x] Generate `business-rules.md`
- [x] Generate `domain-entities.md`

---

## Clarifying Questions

### Q1: Scheduler Execution Path

The existing `DeviceService.updateState()` hardcodes `actorName = resolvedUser.getName()`.
For scheduled execution we need `actorName = "Scheduler (scheduleName)"`.

How should the scheduler apply device state?

- A) **Add an internal `DeviceService` method** `updateStateAsActor(Long deviceId, DeviceStateRequest, User owner, String actorName)` that skips ownership lookup and accepts a custom actor name — `ScheduleService` calls this. Minimal duplication, clean reuse.
- B) **Apply state directly in `ScheduleService.executeSchedule()`** — load device, apply fields, save, broadcast WebSocket, call `activityLogService.log()` with custom actorName. Slightly more code in ScheduleService, no changes to DeviceService.
- X) Other (please describe)

[Answer]:

---

### Q2: Device Deletion Cascade

When a device is deleted, what should happen to its schedules?

- A) **CASCADE DELETE** — schedules are automatically deleted with the device (FK: `ON DELETE CASCADE` in V6 migration). Simplest UX, no orphan schedules.
- B) **RESTRICT** — deleting a device that has schedules is blocked with an error until schedules are manually removed first.
- X) Other (please describe)

[Answer]:
