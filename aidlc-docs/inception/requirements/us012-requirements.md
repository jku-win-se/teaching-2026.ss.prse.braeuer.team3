# Requirements: US-012 — Rules Frontend-Backend Integration

**Datum**: 2026-04-28
**Anfrage-Typ**: Enhancement
**Scope**: Multiple Components (Backend + Frontend)
**Komplexität**: Moderat
**Antworten-Quelle**: us012-requirement-verification-questions.md

---

## Intent Analysis

- **User Request**: US-012 — Benutzer möchte zwischen zeitbasierten, schwellenwertbasierten und ereignisbasierten Auslösern wählen können
- **Entwickler-Kommentar**: "added triggertypes in FR-10, exchange frontendmock with backend rules changes were not made yet"
- **Request Type**: Enhancement (Frontend-Backend Integration + Backend-Erweiterung)
- **Scope Estimate**: Multiple Components — Backend (Rule-Entity, TriggerType, Service, Scheduler, DB-Migration) + Frontend (rule.service.ts, models.ts, RulesComponent, NewRuleDialogComponent)
- **Complexity Estimate**: Moderat

---

## Funktionale Anforderungen

### FR-A: Backend — TIME Trigger-Typ

- **FR-A1**: `TriggerType` Enum um `TIME` erweitern (neben `THRESHOLD` und `EVENT`)
- **FR-A2**: `Rule`-Entity erhält optionale Felder: `triggerHour` (0–23), `triggerMinute` (0–59), `triggerDaysOfWeek` (kommaseparierter String: MONDAY, TUESDAY, ...)
- **FR-A3**: `trigger_device_id` in der `rules`-Tabelle wird nullable (TIME-Regeln haben kein Trigger-Gerät)
- **FR-A4**: `RuleRequest` DTO: `triggerDeviceId` optional; neue Felder `triggerHour`, `triggerMinute`, `triggerDaysOfWeek`
- **FR-A5**: `RuleResponse` DTO: neue Felder `triggerHour`, `triggerMinute`, `triggerDaysOfWeek`
- **FR-A6**: `RuleService.createRule` / `updateRule`: kein `triggerDevice` nötig für TIME-Regeln
- **FR-A7**: Neuer `RuleScheduler` (`@Scheduled`, jede Minute): findet alle aktiven TIME-Regeln, deren `triggerHour`/`triggerMinute` mit der aktuellen Zeit übereinstimmt und der aktuelle Wochentag in `triggerDaysOfWeek` enthalten ist, und führt sie aus
- **FR-A8**: Flyway-Migration (nächste Versionsnummer): ADD COLUMNS `trigger_hour`, `trigger_minute`, `trigger_days_of_week` (alle nullable), ALTER `trigger_device_id` zu nullable

### FR-B: Frontend — rules.service.ts (neu)

- **FR-B1**: Neuer `RuleService` (Angular `@Injectable`) mit HTTP-Methoden:
  - `getRules(): Observable<RuleDto[]>` → `GET /api/rules`
  - `createRule(req: RuleRequest): Observable<RuleDto>` → `POST /api/rules`
  - `updateRule(id: number, req: RuleRequest): Observable<RuleDto>` → `PUT /api/rules/{id}`
  - `setEnabled(id: number, enabled: boolean): Observable<RuleDto>` → `PATCH /api/rules/{id}/enabled`
  - `deleteRule(id: number): Observable<void>` → `DELETE /api/rules/{id}`

### FR-C: Frontend — models.ts aktualisieren

- **FR-C1**: `TriggerType` anpassen: `'TIME' | 'THRESHOLD' | 'EVENT'` (Backend-Enum-Namen, Uppercase)
- **FR-C2**: `Rule`-Interface entfernen oder durch `RuleDto` ersetzen (vollständige Backend-Response-Felder)
- **FR-C3**: `RuleDto` Interface: `id`, `name`, `enabled`, `triggerType`, `triggerDeviceId?`, `triggerDeviceName?`, `triggerOperator?`, `triggerThresholdValue?`, `triggerHour?`, `triggerMinute?`, `triggerDaysOfWeek?`, `actionDeviceId`, `actionDeviceName`, `actionValue`
- **FR-C4**: `RuleRequest` Interface: entspricht `RuleDto` ohne `id`, `*Name`-Felder
- **FR-C5**: `hasConflict`-Flag entfernen (Backend liefert es nicht)

### FR-D: Frontend — RulesComponent

- **FR-D1**: Mock-Daten-Import (`RULES`) entfernen; Regeln per `RuleService.getRules()` laden
- **FR-D2**: Toggle (Enable/Disable) via `RuleService.setEnabled()` an Backend senden
- **FR-D3**: Delete-Button je Regel-Karte mit Bestätigungsdialog (Angular Material `MatDialog` oder `confirm`)
- **FR-D4**: Edit-Button je Regel-Karte — öffnet Dialog mit vorausgefüllten Werten
- **FR-D5**: Nach Erstellen/Bearbeiten/Löschen: Regelliste neu laden

### FR-E: Frontend — NewRuleDialogComponent / EditRuleDialogComponent

- **FR-E1**: Geräteliste aus Backend laden (via `RoomService.getRooms()` + `DeviceService.getDevices(roomId)`) — kein Mock mehr
- **FR-E2**: Raum-Auswahl → Gerät-Auswahl (zweistufig, gemäß Antwort 3B)
- **FR-E3**: Threshold-Operatoren auf `GT` (>) und `LT` (<) beschränken (gemäß Antwort 2A)
- **FR-E4**: TIME-Trigger: Uhrzeit (hour/minute) + Wochentage konfigurierbar, an Backend-Format angepasst
- **FR-E5**: `actionValue` korrekt mappen: `"true"`/`"false"` für Switch/Dimmer/Thermostat; `"open"`/`"close"` für Cover
- **FR-E6**: `save()` ruft `RuleService.createRule()` auf (Create-Modus) oder `RuleService.updateRule()` (Edit-Modus)
- **FR-E7**: Edit-Modus: Dialog-Initialisierung mit bestehenden Regelwerten (Pre-fill)

---

## Nicht-funktionale Anforderungen

- **NFR-01 (PMD)**: Kein neuer Java-Code darf PMD critical/high Violations haben
- **NFR-02 (Javadoc)**: Alle neuen public Klassen und Methoden in Backend erhalten Javadoc (`@param`, `@return`, `@throws`)
- **NFR-03 (Tests)**: `RuleScheduler` und neue `RuleService`-Logik für TIME werden durch Unit Tests abgedeckt

---

## Akzeptanzkriterien (aus US-012)

- [ ] Zeitbasierter Auslöser (Uhrzeit + Wochentage) konfigurierbar und im Backend persistiert
- [ ] Schwellenwertbasierter Auslöser (Sensor GT/LT Grenzwert) konfigurierbar und im Backend persistiert
- [ ] Ereignisbasierter Auslöser (Gerätezustandsänderung) konfigurierbar und im Backend persistiert
- [ ] Regellist lädt echte Daten vom Backend (kein Mock)
- [ ] Neue Regel wird gespeichert und erscheint in der Liste
- [ ] Toggle Enable/Disable wird ans Backend persistiert
- [ ] Edit und Delete funktionieren und persistieren
