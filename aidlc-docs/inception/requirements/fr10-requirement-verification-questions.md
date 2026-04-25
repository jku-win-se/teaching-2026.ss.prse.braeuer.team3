# Requirement Verification Questions — FR-10: Rule Engine (IF-THEN)

**Issue**: https://github.com/jku-win-se/teaching-2026.ss.prse.braeuer.team3/issues/17
**User Story**: US-011 — Bedingungs-Aktions-Regeln der Form „WENN <Auslöser> DANN <Aktion>"

**Architecture baseline**:
- `THRESHOLD` + `EVENT` rules → evaluated reactively inside `DeviceService.updateState()` after every `PATCH /state` call
- `TIME_BASED` rules → `@Scheduled(cron = "0 * * * * *")` polling, same pattern as FR-09
- Rule entity: `triggerType`, `triggerDevice`, `triggerThresholdValue`, `actionDevice`, `actionValue` (string)
- Rule execution logs to `ActivityLogService` and broadcasts via `DeviceWebSocketHandler`

---

## Q1 — Trigger Types in Scope
[Answer]: A (revised by Q5-B) — **Sensor Threshold + Device Event only** (TIME_BASED removed — covered by FR-09 schedules)

---

## Q2 — Device Event Trigger: What counts as an "event"?

Concrete examples to clarify the options:

- **Option A — Any state change fires the rule:**
  User dims a light → rule evaluates. Schedule turns on thermostat → rule evaluates. Risk: chain reactions (rule A fires → changes device → rule B fires).

- **Option B — Only `stateOn` changes fire the rule:**
  User turns switch ON → rule evaluates. User adjusts brightness → rule does NOT evaluate. Covers most common "IF turned on/off THEN …" scenarios. Simpler.

- **Option C — Only manual changes (not from schedules or other rules):**
  User turns switch ON → rule evaluates. Schedule turns device on → rule does NOT evaluate. Prevents chain reactions but requires tracking the source of each state change.

A) Any state change (any field)
B) Only on/off changes (`stateOn` only)
C) Only manual user changes — not from schedules or rules

[Answer]: B — Only stateOn changes fire EVENT rules

---

## Q3 — Sensor Threshold Trigger: Operators
[Answer]: A — `>` and `<` only

---

## Q4 — Action Scope
[Answer]: A — Switch and Shutter only (`stateOn` true/false, `coverPosition` 0/100)

---

## Q5 — TIME_BASED Rules (follow-up needed)

Your answer was: *"rules are purely with conditions"*

Please confirm which interpretation is correct:

- **Option A — Time triggers a condition check:**
  A TIME_BASED rule fires at a specific time AND checks a condition. Example: "at 08:00, IF sensor temperature > 25, THEN turn on AC". Time = when to evaluate, condition = what to check.

- **Option B — TIME_BASED is out of scope:**
  Schedules (FR-09) already cover time-based automation. Rules only cover THRESHOLD and EVENT. (This would change Q1 from all-three to THRESHOLD + EVENT only.)

A) Time triggers a condition check (time + condition combined)
B) TIME_BASED is out of scope — rules are THRESHOLD and EVENT only

[Answer]: B — TIME_BASED out of scope, rules are THRESHOLD + EVENT only

---

## Q6 — Conflict Detection
[Answer]: B — No conflict detection this sprint

---

## Q7 — Frontend
[Answer]: C — Backend only, frontend stays as mock for now
