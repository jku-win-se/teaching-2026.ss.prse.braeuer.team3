# Application Design Plan — FR-09: Zeitpläne konfigurieren

## Design Plan Checklist

- [ ] Generate `components.md` — component definitions and responsibilities
- [ ] Generate `component-methods.md` — method signatures and I/O types
- [ ] Generate `services.md` — service definitions and orchestration
- [ ] Generate `component-dependency.md` — dependency relationships and data flow
- [ ] Generate `application-design.md` — consolidated design document
- [ ] Validate design completeness and consistency

---

## Design Context

**Existing relevant components:**
- `DeviceService.updateState(Long deviceId, DeviceStateRequest req, String actorName)` — already applies device state; Quartz executor will reuse this
- `ActivityLogService.log(Device, User?, String actorName, String action)` — already records log entries
- `DeviceStateRequest` DTO — `stateOn`, `brightness`, `temperature`, `sensorValue`, `coverPosition` (all nullable)
- Flyway migrations: currently V1–V5; next is V6

---

## Clarifying Questions

Please fill in all `[Answer]:` tags directly in this document.

---

### Q1: Days-of-Week Storage

How should `daysOfWeek` be stored in the `schedules` table?

- A) Single VARCHAR column with comma-separated day names (e.g., `"MONDAY,WEDNESDAY,FRIDAY"`) — simple, single column
- B) Separate `schedule_days` element-collection table — normalized, more complex join
- X) Other (please describe)

[Answer]:
A

---

### Q2: Angular Schedule Form — Modal or Page?

Should the schedule create/edit form be presented as:

- A) A **dialog/modal** that opens over the current page (similar to device add dialog) — stays on the schedules list while editing
- B) A **dedicated route/page** (e.g., `/schedules/new`, `/schedules/:id/edit`) — full screen form
- X) Other (please describe)

[Answer]:
A

---

### Q3: Quartz Job Management — Separation of Concerns

Should Quartz job registration/removal be:

- A) **Inside `ScheduleService`** — the service handles both business logic and Quartz scheduler calls (simpler, one class)
- B) **Separate `ScheduleJobManager` component** — wraps Quartz API, `ScheduleService` delegates to it (cleaner separation, more classes)
- X) Other (please describe)

[Answer]:
A