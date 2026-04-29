# Code Generation Plan — US-012 Backend (Unit 1)

**Datum**: 2026-04-28
**Workspace Root**: /Users/simonfalkner/Documents/Uni/PR_SE/teaching-2026.ss.prse.braeuer.team3
**Typ**: Brownfield — bestehende Dateien modifizieren

## Stories
- FR-A1–A8: Backend TIME Trigger Erweiterung (Enum, Entity, DTOs, Service, Scheduler, Migration)

## Betroffene Dateien (Übersicht)
| Schritt | Datei | Aktion |
|---------|-------|--------|
| 1 | `backend/src/main/resources/db/migration/V8__add_rule_time_trigger.sql` | NEU |
| 2 | `backend/src/main/java/.../domain/TriggerType.java` | MODIFY |
| 3 | `backend/src/main/java/.../domain/Rule.java` | MODIFY |
| 4 | `backend/src/main/java/.../dto/RuleRequest.java` | MODIFY |
| 5 | `backend/src/main/java/.../dto/RuleResponse.java` | MODIFY |
| 6 | `backend/src/main/java/.../repository/RuleRepository.java` | MODIFY |
| 7 | `backend/src/main/java/.../service/RuleService.java` | MODIFY |
| 8 | `backend/src/main/java/.../service/RuleScheduler.java` | NEU |
| 9 | `backend/src/test/java/.../service/RuleServiceTest.java` | MODIFY |
| 10 | `backend/src/test/java/.../service/RuleSchedulerTest.java` | NEU |
| 11 | `backend/src/test/java/.../controller/RuleControllerTest.java` | MODIFY |

---

## Schritte

- [ ] **Step 1** — Flyway Migration V8: neue Spalten, `trigger_device_id` nullable
- [ ] **Step 2** — `TriggerType.java`: `TIME` ergänzen (mit Javadoc)
- [ ] **Step 3** — `Rule.java`: `triggerDevice` optional + neue Felder `triggerHour`, `triggerMinute`, `triggerDaysOfWeek`
- [ ] **Step 4** — `RuleRequest.java`: neue Felder + `triggerDeviceId` optional
- [ ] **Step 5** — `RuleResponse.java`: neue Felder im Konstruktor + Getter
- [ ] **Step 6** — `RuleRepository.java`: neue Query `findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute`
- [ ] **Step 7** — `RuleService.java`: `applyRequest`, `createRule`, `updateRule`, `toResponse` TIME-kompatibel; neue Methode `evaluateTimeRules()`
- [ ] **Step 8** — `RuleScheduler.java` (neu): `@Scheduled(cron="0 * * * * *")` — evaluiert TIME-Regeln
- [ ] **Step 9** — `RuleServiceTest.java`: Tests für TIME-create, TIME-evaluate
- [ ] **Step 10** — `RuleSchedulerTest.java` (neu): Tests für Scheduler-Logik
- [ ] **Step 11** — `RuleControllerTest.java`: Tests für TIME-Regeln via API
