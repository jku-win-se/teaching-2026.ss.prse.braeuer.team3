# Units of Work — US-012

**Feature**: US-012 — Rules Frontend-Backend Integration (TIME / THRESHOLD / EVENT)
**Datum**: 2026-04-28

---

## Unit 1: Backend — TIME Trigger Erweiterung

**Beschreibung**: Erweiterung des bestehenden Rule-Engine-Backends (FR-10) um den `TIME` Trigger-Typ, analog zur Schedule-Implementierung (FR-09).

**Verantwortlichkeiten**:
- `TriggerType` Enum um `TIME` ergänzen
- `Rule` Entity: `triggerDevice` optional machen, neue Felder `triggerHour`, `triggerMinute`, `triggerDaysOfWeek`
- `RuleRequest` / `RuleResponse` DTOs: neue Felder, `triggerDeviceId` optional
- `RuleService`: `createRule`/`updateRule`/`applyRequest` TIME-kompatibel machen; neue Methode `evaluateTimeRules()`
- `RuleScheduler` (neu): `@Scheduled(cron="0 * * * * *")` — evaluiert TIME-Regeln jede Minute
- Flyway V8 Migration: neue Spalten, `trigger_device_id` nullable
- Unit Tests für `RuleScheduler` und TIME-Logik in `RuleService`

**Betroffene Dateien**:
- `domain/TriggerType.java`
- `domain/Rule.java`
- `dto/RuleRequest.java`
- `dto/RuleResponse.java`
- `service/RuleService.java`
- `service/RuleScheduler.java` (neu)
- `resources/db/migration/V8__add_rule_time_trigger.sql` (neu)
- Bestehende Tests erweitern / neue Tests

**Abhängigkeiten**: Keine externen Abhängigkeiten — in sich geschlossen

---

## Unit 2: Frontend — Rules Integration

**Beschreibung**: Ersetzen der Mock-Daten im Frontend durch echte Backend-API-Calls. Vollständige CRUD-Unterstützung (Create, Read, Toggle, Edit, Delete) für Rules.

**Verantwortlichkeiten**:
- `rule.service.ts` (neu): Angular HTTP-Service für alle `/api/rules`-Endpoints
- `models.ts`: `TriggerType` (Uppercase), `RuleDto`, `RuleRequest` Interface, `hasConflict` entfernen
- `rules.component.ts`: Mock-Import entfernen, `RuleService` verwenden; Delete + Edit Buttons
- `new-rule-dialog.component.ts`: echte Geräteliste (Raum → Gerät), Operatoren GT/LT, TIME-Format anpassen, API-Call; Edit-Modus

**Abhängigkeiten**: Setzt Unit 1 voraus (neue TIME-Felder in `RuleResponse`)

---

## Update-Reihenfolge

1. Unit 1 (Backend) — vollständig implementieren und testen
2. Unit 2 (Frontend) — auf Basis der finalen API implementieren
