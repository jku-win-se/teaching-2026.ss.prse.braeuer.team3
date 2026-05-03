# Code Generation Plan — US-014 Conflict Detection

## Unit Context
- **User Story**: US-014 — Regelkonflikte erkennen und warnen
- **Acceptance Criteria**:
  - AC1: System erkennt konkurrierende Regeln für dasselbe Gerät
  - AC2: Benutzer wird beim Erstellen einer konfliktierenden Regel gewarnt
  - AC3: Konfliktdetails werden verständlich angezeigt
- **Workspace Root**: /Users/oliversugic/Studium/PR_SE/teaching-2026.ss.prse.braeuer.team3
- **Project Type**: Brownfield (Spring Boot 3.3.5, Java 21 / Angular 19)

## Conflict Definition
Two rules conflict when they:
1. Both target the **same action device** (`actionDeviceId`)
2. Have **opposite action values**: `"true"` ↔ `"false"` (Switch/Sensor) or `"open"` ↔ `"close"` (Cover)
3. Are both **enabled**

---

## Steps

### Backend

- [x] Step 1: Add `findByEnabledTrueAndUserAndActionDevice` to `RuleRepository`
  - File: `backend/src/main/java/at/jku/se/smarthome/repository/RuleRepository.java`
  - New derived query method: returns all enabled rules for a user targeting a specific action device

- [x] Step 2: Add `checkConflicts` to `RuleService`
  - File: `backend/src/main/java/at/jku/se/smarthome/service/RuleService.java`
  - Public method: `checkConflicts(String email, Long actionDeviceId, String actionValue, Long excludeRuleId)`
  - Logic: find all enabled rules for user + action device, filter to those with opposite action value, exclude `excludeRuleId` (used when editing)
  - Returns: `List<RuleResponse>`

- [x] Step 3: Add `GET /api/rules/conflicts` to `RuleController`
  - File: `backend/src/main/java/at/jku/se/smarthome/controller/RuleController.java`
  - Params: `actionDeviceId` (required), `actionValue` (required), `excludeRuleId` (optional)
  - Returns 200 with list of conflicting rules (empty = no conflicts)

- [x] Step 4: Add tests to `RuleServiceTest`
  - File: `backend/src/test/java/at/jku/se/smarthome/service/RuleServiceTest.java`
  - `checkConflicts_oppositeActionValue_returnsConflict()`
  - `checkConflicts_sameActionValue_returnsEmpty()`
  - `checkConflicts_noRulesForDevice_returnsEmpty()`
  - `checkConflicts_excludesEditedRule()`

- [x] Step 5: Add tests to `RuleControllerTest`
  - File: `backend/src/test/java/at/jku/se/smarthome/controller/RuleControllerTest.java`
  - `checkConflicts_withConflicts_returns200WithList()`
  - `checkConflicts_noConflicts_returns200EmptyList()`

### Frontend

- [x] Step 6: Add `checkConflicts` to `rule.service.ts`
  - File: `frontend/src/app/core/rule.service.ts`
  - Method: `checkConflicts(actionDeviceId: number, actionValue: string, excludeRuleId?: number): Observable<RuleDto[]>`

- [x] Step 7: Update `RuleConflictInfo` model in `models.ts` (if needed)
  - Use existing `RuleDto` — no new model required

- [x] Step 8: Modify `new-rule-dialog.component.ts`
  - In the Review step: when the user reaches Step 4, call `checkConflicts`
  - Display a warning panel if conflicts are found (AC2, AC3)
  - Conflict panel shows: conflicting rule name + summary
  - User can still proceed (save) or go back — it's a non-blocking warning

---

## Story Traceability
- AC1 → Steps 1, 2 (backend detection logic)
- AC2 → Steps 3, 6, 8 (API + frontend warning trigger)
- AC3 → Step 8 (conflict details displayed in UI)
