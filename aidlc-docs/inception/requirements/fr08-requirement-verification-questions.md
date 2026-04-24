# FR-08 Requirement Verification Questions

**Feature**: FR-08 Aktivitätsprotokoll (Activity Log)
**Issue**: #15

Please fill in all `[Answer]:` tags below.

---

## Q1: Actor / Auslöser
What should be recorded as the actor of a state change?

- A) The user's email address
- B) The user's display name
- C) A label like "Manual" (for user actions) vs "System" (for automated rules)
- D) Both user name AND a type label (e.g. "Alice — Manual")
- E) Other (please describe)

[Answer]:
If it is a manual state of change log the users Name, if it is a automated rule log System and the rules name
---

## Q2: Automated Changes
FR-09 (schedules) and FR-10/11 (rule engine) are not yet implemented. How should the log handle automated changes for now?

- A) Only log manual state changes for now — extend later when FR-09/10/11 are implemented
- B) Design the log entry model to support automated actors now, but only populate it from manual triggers for now
- C) Other (please describe)

[Answer]:
A
---

## Q3: Log Scope
Should the activity log be global (all devices across all rooms) or scoped?

- A) Global — one log for all the user's devices across all rooms
- B) Per-room — a separate log per room
- C) Per-device — accessible from each device card
- D) Other (please describe)

[Answer]:
A
---

## Q4: Filtering
The acceptance criteria say "filterable by date/device". What exactly should be supported?

- A) Filter by date range (from/to) AND by specific device (dropdown)
- B) Filter by single date AND by device name (text search)
- C) Filter by date range only
- D) Filter by device only
- E) Other (please describe)

[Answer]:
A
---

## Q5: Pagination
How many log entries should be shown?

- A) Show all entries (no pagination)
- B) Paginated — configurable page size (e.g. 20 per page)
- C) Show latest N entries (e.g. last 50), no pagination
- D) Other (please describe)

[Answer]:
B
---

## Q6: Real-time Updates
Should new log entries appear in real-time in the UI (via WebSocket, like device state updates)?

- A) Yes — new entries appear automatically without page refresh
- B) No — user refreshes manually or the log reloads on next page visit
- C) Other (please describe)

[Answer]:
A
---

## Q7: Log Retention
Should old log entries ever be deleted?

- A) No — keep all entries forever
- B) Yes — keep only the last N entries (specify how many)
- C) Yes — delete entries older than X days (specify how many)
- D) Other (please describe)

[Answer]:
log entries should be kept until it is manually deleted by the owner