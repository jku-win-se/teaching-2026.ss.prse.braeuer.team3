# Business Rules — US-012 Backend (Unit 1)

## BR-01: Trigger-Typ-Konsistenz

**Regel**: Für `TIME`-Regeln darf `triggerDeviceId` im Request null sein; für `THRESHOLD` und `EVENT` ist er Pflicht.

**Implementierung**: `RuleService.createRule()` / `updateRule()` prüfen `request.getTriggerType()` — `resolveOwnedDevice` wird für TIME nicht aufgerufen.

---

## BR-02: TIME-Felder Pflicht bei TIME-Trigger

**Regel**: Wenn `triggerType == TIME`, müssen `triggerHour` (0–23), `triggerMinute` (0–59) und `triggerDaysOfWeek` (mind. 1 Tag) gesetzt sein.

**Implementierung**: Validierung in `RuleService.applyRequest()` — `ResponseStatusException(BAD_REQUEST)` wenn Felder fehlen.

---

## BR-03: Keine Endlosschleifen

**Regel**: `RuleScheduler.runDueTimeRules()` ruft `deviceService.updateStateAsActor()` auf (nicht `updateState()`), damit TIME-Rules keine weiteren Rules triggern.

**Implementierung**: Analog zu bestehenden EVENT/THRESHOLD-Rules — `executeRule()` verwendet `updateStateAsActor`.

---

## BR-04: Skipped Executions

**Regel**: Wenn der Server während einer geplanten Uhrzeit nicht läuft, wird die Ausführung **nicht** nachgeholt (fire-and-forget, identisch mit FR-09 Schedule).

---

## BR-05: Enabled-Flag

**Regel**: Deaktivierte Rules (`enabled = false`) werden weder durch den Scheduler noch durch Device-State-Updates evaluiert.

**Implementierung**: Repository-Query filtert `enabled = true`.

---

## BR-06: DayOfWeek-Format

**Regel**: `triggerDaysOfWeek` wird als kommaseparierter String mit Java `DayOfWeek`-Namen (Uppercase) gespeichert, z.B. `"MONDAY,WEDNESDAY,FRIDAY"`. Identisches Format wie `Schedule.daysOfWeek` (FR-09).

---

## BR-07: toResponse — nullable Felder

**Regel**: `RuleResponse` liefert `triggerDeviceId`/`triggerDeviceName` als `null` für TIME-Regeln; `triggerHour`/`triggerMinute`/`triggerDaysOfWeek` als `null` für THRESHOLD/EVENT-Regeln. Keine Fehler wenn null — Jackson serialisiert sie als JSON `null`.
