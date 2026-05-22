# Requirements Analysis — FR-21: Vacation Mode

## Intent Analysis

- **User Request**: Implement FR-21 Vacation Mode (GitHub issue #28, branch `28-fr-21-vacation-mode`)
- **Request Type**: New Feature
- **Scope Estimate**: Multiple Components — new domain entity, service, scheduler, controller, Flyway migration, frontend component, service, route
- **Complexity Estimate**: Moderate — well-defined acceptance criteria, integrates with existing Schedule system

---

## Decisions (confirmed by user)

| # | Decision |
|---|---|
| 1 | **Schedule toggling (Option A)**: vacation mode sets `schedule.enabled = true` on startDate, `false` on endDate+1 or deactivation |
| 2 | **Singleton**: only one vacation mode record can exist per user at a time (service-level enforcement, 409 if one already exists) |
| 3 | **GET returns all**: includes past/deactivated vacation modes |
| 4 | **Deactivation is permanent**: no reactivation; once deactivated the schedule is disabled and the record stays as history |
| 5 | **Pattern**: mirrors `RuleScheduler` — a dedicated `VacationModeScheduler` component runs daily (`0 0 0 * * *`) |

---

## Functional Requirements

### FR-21.1 — Configure Vacation Mode
The owner can create a vacation mode with:
- A user-defined name (max 100 characters)
- A start date (inclusive, ISO date)
- An end date (inclusive, must be >= start date)
- An assigned Schedule (from the user's existing schedules)

Only one vacation mode record may exist per user. Creating a second one while one already exists → **409 Conflict**.

### FR-21.2 — Automatic Schedule Toggling
A daily background job (`VacationModeScheduler`, cron `0 0 0 * * *`) runs on `VacationModeService.checkAndUpdateStates()`:
- If `today >= startDate` AND `today <= endDate` AND `!deactivated` → set `schedule.enabled = true` (activate)
- If `today > endDate` AND `!deactivated` → set `schedule.enabled = false`, set `deactivated = true` (auto-expire)

On **manual deactivation**: immediately set `schedule.enabled = false`, set `deactivated = true`.
On **creation** (if `startDate <= today`): immediately set `schedule.enabled = true`.

### FR-21.3 — Early Deactivation
Owner can PATCH deactivate at any time. Sets `deactivated = true` and `schedule.enabled = false`. Permanent — no reactivation.

### FR-21.4 — List & Delete
- GET returns all vacation modes for the owner (past and present)
- DELETE removes the record (if deactivated or future); also sets `schedule.enabled = false` if the vacation was active

### FR-21.5 — Owner-Only
All endpoints restricted to home owner via `MemberService.requireOwnerRole()`.

---

## Non-Functional Requirements

### NFR-04 — PMD Compliance
All Java code must be free of PMD critical/high violations per `ruleset.xml`.

### NFR-06 — Javadoc
Every public class and method in `domain/`, `service/`, `controller/` must have full Javadoc with `@param`, `@return`, `@throws`.

### NFR — Consistency
Follow existing patterns: constructor injection, `ResponseStatusException`, `@Transactional`, `MemberService.requireOwnerRole()` + `resolveEffectiveOwner()`, Flyway V14, `@Component` scheduler (like `RuleScheduler`).

---

## Acceptance Criteria (from issue #28)

- [x] Urlaubsmodus mit Start- und Enddatum konfigurierbar
- [x] Einem Zeitplan zuweisbar
- [x] Urlaubsmodus lässt sich vorzeitig deaktivieren

---

## Technical Context

- **Schedule entity**: `domain/Schedule.java` — has `enabled` flag managed by this feature
- **Schedule execution**: `ScheduleService.runDueSchedules()` already respects `enabled` — no changes needed there
- **Scheduler pattern**: `RuleScheduler` (@Component + @Scheduled) delegates to service — replicate for `VacationModeScheduler`
- **Owner auth**: `MemberService.requireOwnerRole()` + `resolveEffectiveOwner()`
- **Latest Flyway migration**: V13 → next is **V14**
- **Frontend patterns**: Angular 19 standalone components, Angular Material, MatDialog for forms, `ownerGuard`

---

## Data Model

```sql
CREATE TABLE vacation_modes (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id),
  schedule_id BIGINT NOT NULL REFERENCES schedules(id),
  name        VARCHAR(100) NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  deactivated BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## API Design

```
GET    /api/vacation-modes                      — list all (owner-only)
POST   /api/vacation-modes                      — create (409 if one already exists)
PATCH  /api/vacation-modes/{id}/deactivate      — permanently deactivate
DELETE /api/vacation-modes/{id}                 — delete record
```

### VacationModeRequest
```json
{ "name": "Summer Holiday", "scheduleId": 3, "startDate": "2026-07-01", "endDate": "2026-07-21" }
```

### VacationModeResponse
```json
{
  "id": 1,
  "name": "Summer Holiday",
  "scheduleId": 3,
  "scheduleName": "Evening Routine",
  "startDate": "2026-07-01",
  "endDate": "2026-07-21",
  "deactivated": false,
  "active": true
}
```
`active` is computed: `!deactivated && !today.isBefore(startDate) && !today.isAfter(endDate)`

---

## Edge Cases

| Case | Behavior |
|---|---|
| `endDate < startDate` | 400 Bad Request |
| Schedule not owned by user | 404 Not Found |
| Create when one already exists | 409 Conflict |
| Deactivate already-deactivated | Idempotent (200, no error) |
| `startDate` is today on create | Immediately enable schedule |
| Delete active vacation mode | Also sets `schedule.enabled = false` |
