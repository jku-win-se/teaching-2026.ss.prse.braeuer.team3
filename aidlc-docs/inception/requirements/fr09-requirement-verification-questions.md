# FR-09: Zeitpläne konfigurieren — Clarifying Questions

**Feature**: FR-09 Zeitpläne konfigurieren (Configure Schedules)
**Issue**: #16
**User Story (US-010)**: As a user, I want to configure unconditional time-based schedules for device actions, so devices are automatically controlled at defined times.

**Acceptance Criteria (from issue)**:
- A schedule with time and action can be created
- The schedule is reliably executed at the configured time
- Schedules can be edited and deleted

---

Please fill in all `[Answer]:` tags directly in this document.

---

## Q1: Recurrence — One-time vs Recurring

Should schedules support only recurring execution (e.g., every day at 07:00) or also one-time execution (e.g., run once on 2026-05-01 at 08:00)?

- A) Recurring only (e.g., daily at a fixed time, optionally on specific weekdays)
- B) One-time only (execute once at a specific date and time, then expire)
- C) Both recurring and one-time
- X) Other (please describe after [Answer]: tag below)

[Answer]:
A

---

## Q2: Schedule Granularity

How fine-grained should schedule timing be?

- A) Hour + minute only (e.g., "07:30 every day")
- B) Hour + minute + days of week (e.g., "07:30 on Mon/Wed/Fri")
- C) Full cron expression (e.g., "0 30 7 * * MON,WED,FRI") — requires cron knowledge from user
- X) Other (please describe after [Answer]: tag below)

[Answer]:
B
---

## Q3: Supported Device Actions

Which device actions should be schedulable? Select all that apply:

- A) Turn on/off (lights, sockets, generic switches)
- B) Set brightness level (dimmable lights)
- C) Set temperature setpoint (thermostats)
- D) Set thermostat mode (heating/cooling/off)
- E) All device state fields that can be set via the existing updateState API
- X) Other (please describe after [Answer]: tag below)

[Answer]:
E
---

## Q4: Scope — Per User or Per Device

Should schedules be scoped per authenticated user (each user manages their own schedules) or are schedules global/per-room?

- A) Per user — each user sees and manages only their own schedules
- B) Per device — schedules belong to devices, visible to all users who have access to the device
- X) Other (please describe after [Answer]: tag below)

[Answer]:
B
---

## Q5: Timezone Handling

How should schedule times be interpreted?

- A) Server timezone (fixed, no user configuration needed)
- B) User's browser/local timezone (passed from frontend)
- C) UTC always
- X) Other (please describe after [Answer]: tag below)

[Answer]:
A
---

## Q6: Conflict Resolution

What happens if two schedules conflict (e.g., two schedules for the same device fire at the same time)?

- A) Last-write wins — both execute in order of schedule ID, last one takes effect
- B) First-created wins — newer conflicting schedules are rejected on creation
- C) No conflict resolution needed — all scheduled actions execute, last state wins naturally
- X) Other (please describe after [Answer]: tag below)

[Answer]:
C
---

## Q7: Enable/Disable Schedules

Should individual schedules be temporarily disableable without deleting them?

- A) Yes — schedules have an enabled/disabled toggle
- B) No — delete and recreate is sufficient
- X) Other (please describe after [Answer]: tag below)

[Answer]:
A
---

## Q8: Frontend UI Location

Where should schedule management appear in the frontend?

- A) Dedicated "Schedules" page in the navigation (similar to the existing Logs page)
- B) Per-device detail panel/dialog — schedules shown when you open a device
- C) Both — global schedules page + shortcut from device panel
- X) Other (please describe after [Answer]: tag below)

[Answer]:
C
---

## Q9: Backend Scheduling Technology

Which scheduling approach should be used on the backend?

- A) Spring `@Scheduled` with `fixedDelay`/`cron` — simple, no extra dependencies, but requires restart to pick up new schedules
- B) Quartz Scheduler — persisted to DB, survives restarts, supports dynamic job registration; adds dependency
- C) Spring `TaskScheduler` — programmatic scheduling at runtime, no extra dependency, schedules lost on restart
- X) Other (please describe after [Answer]: tag below)

[Answer]:
B
---

## Q10: Activity Log Integration

Should schedule executions be recorded in the Activity Log (FR-08)?

- A) Yes — each automatic schedule execution creates an activity log entry
- B) No — only manual user actions are logged
- X) Other (please describe after [Answer]: tag below)

[Answer]:
A
---

## Q11: Error Handling for Failed Executions

If a scheduled action fails (e.g., device unreachable), how should the system handle it?

- A) Log the failure silently (no user notification)
- B) Mark the schedule execution as failed in the activity log
- C) Not applicable — devices are always local/in-memory, failures not expected
- X) Other (please describe after [Answer]: tag below)

[Answer]:
B
---

## Q12: Schedule Listing / Overview

When listing schedules, what information should be displayed?

- A) Device name, action, next execution time
- B) Device name, room name, action, schedule time/pattern, enabled status
- C) Minimal — just device name and time
- X) Other (please describe after [Answer]: tag below)

[Answer]:
B + add a name for the scheduled action
