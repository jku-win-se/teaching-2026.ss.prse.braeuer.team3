# Execution Plan — US-012: Rules Frontend-Backend Integration

**Datum**: 2026-04-28
**Feature**: US-012 — Trigger-Typen (Zeit / Schwellenwert / Ereignis)

---

## Detailed Analysis Summary

### Transformation Scope
- **Transformation Type**: Multi-Component (Backend-Erweiterung + Frontend-Integration)
- **Primary Changes**: TIME-TriggerType im Backend; Frontend-Mock durch echte API ersetzen
- **Related Components**: TriggerType Enum, Rule Entity, RuleRequest/RuleResponse DTOs, RuleService, neuer RuleScheduler, Flyway Migration, models.ts, rule.service.ts (neu), RulesComponent, NewRuleDialogComponent

### Change Impact Assessment
- **User-facing changes**: Ja — Regellist zeigt echte Daten; TIME/THRESHOLD/EVENT wirklich konfigurierbar
- **Structural changes**: Ja — neue `trigger_device_id` nullable, neue Spalten in DB
- **Data model changes**: Ja — Flyway Migration (neue Spalten, nullable constraint change)
- **API changes**: Ja — `RuleRequest`/`RuleResponse` um TIME-Felder erweitert
- **NFR impact**: Gering — bestehende PMD/Javadoc/Test-Standards gelten

### Risk Assessment
- **Risk Level**: Medium
- **Rollback Complexity**: Moderat (DB-Migration muss rückgängig gemacht werden)
- **Testing Complexity**: Moderat (RuleScheduler braucht Zeitsteuerungs-Tests)

---

## Workflow Visualization

```mermaid
flowchart TD
    Start(["US-012 Start"])

    subgraph INCEPTION["🔵 INCEPTION PHASE"]
        WD["Workspace Detection<br/><b>COMPLETED</b>"]
        RE["Reverse Engineering<br/><b>COMPLETED (reuse)</b>"]
        RA["Requirements Analysis<br/><b>COMPLETED</b>"]
        US["User Stories<br/><b>SKIP</b>"]
        WP["Workflow Planning<br/><b>IN PROGRESS</b>"]
        AD["Application Design<br/><b>SKIP</b>"]
        UG["Units Generation<br/><b>EXECUTE</b>"]
    end

    subgraph CONSTRUCTION["🟢 CONSTRUCTION PHASE"]
        FD["Functional Design<br/><b>EXECUTE (Unit 1)</b>"]
        NFRA["NFR Requirements<br/><b>SKIP</b>"]
        NFRD["NFR Design<br/><b>SKIP</b>"]
        ID["Infrastructure Design<br/><b>SKIP</b>"]
        CG1["Code Generation<br/><b>Unit 1: Backend</b>"]
        CG2["Code Generation<br/><b>Unit 2: Frontend</b>"]
        BT["Build and Test<br/><b>EXECUTE</b>"]
    end

    subgraph OPERATIONS["🟡 OPERATIONS PHASE"]
        OPS["Operations<br/><b>PLACEHOLDER</b>"]
    end

    Start --> WD --> RE --> RA --> WP
    WP --> UG
    UG --> FD
    FD --> CG1
    CG1 --> CG2
    CG2 --> BT
    BT --> End(["Complete"])

    style WD fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style RE fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style RA fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style WP fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style UG fill:#FFA726,stroke:#E65100,stroke-width:3px,stroke-dasharray:5 5,color:#000
    style FD fill:#FFA726,stroke:#E65100,stroke-width:3px,stroke-dasharray:5 5,color:#000
    style CG1 fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style CG2 fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style BT fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style US fill:#BDBDBD,stroke:#424242,stroke-width:2px,stroke-dasharray:5 5,color:#000
    style AD fill:#BDBDBD,stroke:#424242,stroke-width:2px,stroke-dasharray:5 5,color:#000
    style NFRA fill:#BDBDBD,stroke:#424242,stroke-width:2px,stroke-dasharray:5 5,color:#000
    style NFRD fill:#BDBDBD,stroke:#424242,stroke-width:2px,stroke-dasharray:5 5,color:#000
    style ID fill:#BDBDBD,stroke:#424242,stroke-width:2px,stroke-dasharray:5 5,color:#000
    style OPS fill:#BDBDBD,stroke:#424242,stroke-width:2px,stroke-dasharray:5 5,color:#000
    style INCEPTION fill:#BBDEFB,stroke:#1565C0,stroke-width:3px,color:#000
    style CONSTRUCTION fill:#C8E6C9,stroke:#2E7D32,stroke-width:3px,color:#000
    style OPERATIONS fill:#FFF59D,stroke:#F57F17,stroke-width:3px,color:#000
    style Start fill:#CE93D8,stroke:#6A1B9A,stroke-width:3px,color:#000
    style End fill:#CE93D8,stroke:#6A1B9A,stroke-width:3px,color:#000
    linkStyle default stroke:#333,stroke-width:2px
```

---

## Phases to Execute

### 🔵 INCEPTION PHASE
- [x] Workspace Detection — COMPLETED (reuse)
- [x] Reverse Engineering — COMPLETED (reuse)
- [x] Requirements Analysis — COMPLETED (2026-04-28)
- [ ] User Stories — **SKIP** — US-012 ist klar definiert, kein zusätzlicher Personas-Aufwand
- [x] Workflow Planning — IN PROGRESS
- [ ] Application Design — **SKIP** — keine neue Komponentenarchitektur; bestehende Grenzen werden nur erweitert
- [ ] Units Generation — **EXECUTE** — zwei unabhängige Units definieren (Backend / Frontend)

### 🟢 CONSTRUCTION PHASE
- [ ] Functional Design — **EXECUTE (Unit 1: Backend)** — TIME-Scheduling-Logik im `RuleScheduler` ist nicht-trivial und braucht Design-Entscheidungen
- [ ] NFR Requirements — **SKIP** — bestehende Standards (PMD, Javadoc, Tests) ausreichend
- [ ] NFR Design — **SKIP** — keine neuen NFR-Pattern nötig
- [ ] Infrastructure Design — **SKIP** — keine Infrastrukturänderungen
- [ ] Code Generation Unit 1 (Backend) — **EXECUTE**
- [ ] Code Generation Unit 2 (Frontend) — **EXECUTE**
- [ ] Build and Test — **EXECUTE**

### 🟡 OPERATIONS PHASE
- [ ] Operations — PLACEHOLDER

---

## Units of Work

### Unit 1: Backend — TIME Trigger Erweiterung
**Betroffene Dateien:**
- `TriggerType.java` — `TIME` hinzufügen
- `Rule.java` — `triggerDevice` optional, neue Felder `triggerHour`, `triggerMinute`, `triggerDaysOfWeek`
- `RuleRequest.java` — neue Felder, `triggerDeviceId` optional
- `RuleResponse.java` — neue Felder
- `RuleService.java` — TIME-Logik in `createRule`/`updateRule`/`applyRequest`; neue Methode `evaluateTimeRules()`
- `RuleScheduler.java` (neu) — `@Scheduled(cron="0 * * * * *")`, ruft `evaluateTimeRules()` auf
- `V8__add_rule_time_trigger.sql` (Flyway) — neue Spalten, nullable constraint

### Unit 2: Frontend — Rules Integration
**Betroffene Dateien:**
- `models.ts` — `TriggerType` (Uppercase), `RuleDto`, `RuleRequest` Interface; `hasConflict` entfernen
- `rule.service.ts` (neu) — HTTP-Service (GET, POST, PUT, PATCH, DELETE)
- `rules.component.ts` — Mock-Daten ersetzen durch `RuleService`; Delete + Edit Buttons
- `new-rule-dialog.component.ts` — echte Geräteliste (Raum → Gerät), Operatoren auf GT/LT, TIME-Format anpassen, API-Call on save; Edit-Modus

**Abhängigkeit**: Unit 2 setzt Unit 1 voraus (neues `triggerHour`/`triggerMinute` im DTO).

---

## Module Update Strategy
- **Update Approach**: Sequential (Backend first, dann Frontend)
- **Critical Path**: Unit 1 (Backend DTO-Änderungen) → Unit 2 (Frontend Service + Components)
- **Testing Checkpoints**: Nach Unit 1: Backend-Tests grün, PMD clean. Nach Unit 2: Frontend kompiliert, E2E-Smoke-Test.

---

## Success Criteria
- **Primary Goal**: US-012 Akzeptanzkriterien vollständig erfüllt
- **Key Deliverables**: rule.service.ts, aktualisierte models.ts, RulesComponent ohne Mock, RuleScheduler, Flyway Migration
- **Quality Gates**: alle bestehenden Tests grün, PMD 0 Violations, Javadoc vollständig auf neuen public Methoden
