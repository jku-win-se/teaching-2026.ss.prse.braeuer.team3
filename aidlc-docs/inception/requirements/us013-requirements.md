# Requirements — US-013: In-App-Benachrichtigungen bei Regelausführung

## Intent Analysis

- **User Request**: Als Benutzer möchte ich In-App-Benachrichtigungen erhalten, wenn eine Regel ausgeführt wird oder fehlschlägt, damit ich stets informiert bin.
- **Request Type**: New Feature
- **Scope Estimate**: Multiple Components (Backend `RuleService` + `DeviceWebSocketHandler`, Frontend `RealtimeService` + `ShellComponent`)
- **Complexity Estimate**: Simple-to-Moderate (bestehende WebSocket-Infrastruktur wird erweitert)

---

## Functional Requirements

### FR-US013-01 — Erfolgs-Benachrichtigung
Bei jeder erfolgreichen Regelausführung (Trigger-Typen: EVENT, THRESHOLD, TIME) sendet das Backend eine Benachrichtigung an den verbundenen Frontend-Client des betreffenden Benutzers. Die Benachrichtigung enthält:
- Regelname
- Kurze Aktionsbeschreibung (z. B. „Wohnzimmer-Licht ausgeschaltet")

### FR-US013-02 — Fehler-Benachrichtigung
Bei fehlgeschlagener Regelausführung sendet das Backend eine Fehler-Benachrichtigung. Sie enthält:
- Regelname
- Benutzerfreundlichen Fehlertext (z. B. „Gerät nicht verfügbar")

### FR-US013-03 — Toast/Snackbar
Jede eingehende Benachrichtigung wird sofort als Snackbar am unteren Bildschirmrand angezeigt (auto-dismiss nach 4 Sekunden). Erfolg in Grün, Fehler in Rot/Warn.

### FR-US013-04 — Notification-Panel
Das Glocken-Icon in der Toolbar öffnet bei Klick ein Dropdown-Panel, das alle Benachrichtigungen der aktuellen Browser-Session auflistet (neueste zuerst). Ein Badge zeigt die Anzahl ungelesener Benachrichtigungen.

### FR-US013-05 — Persistenz
Benachrichtigungen werden **nicht** im Backend persistiert. Sie sind nur für die Dauer der Browser-Session sichtbar. Nach Seitenreload ist die Liste leer.

### FR-US013-06 — Übertragungskanal
Benachrichtigungen werden über die bestehende WebSocket-Verbindung (`/ws/devices`) übertragen, mit dem neuen `messageType: "ruleNotification"`. Kein zusätzlicher Endpunkt erforderlich.

---

## Non-Functional Requirements

### NFR-US013-01 — Latenz
Benachrichtigungen erscheinen spätestens 1 Sekunde nach Regelausführung im Frontend (abhängig von WebSocket-Latenz).

### NFR-US013-02 — Kein neuer Infrastrukturaufwand
Die Lösung darf keine neue WebSocket-Verbindung, kein neues Backend-Endpoint und keine neue Datenbankmigrationen erfordern.

### NFR-US013-03 — PMD/Javadoc (NFR-04/NFR-06)
Alle neuen Backend-Klassen und öffentlichen Methoden müssen PMD-konform und vollständig javadoc-dokumentiert sein.

---

## User Scenarios

### Szenario 1: TIME-Regel feuert erfolgreich
- Benutzer hat eine TIME-Regel konfiguriert: „Jeden Montag um 08:00 Licht einschalten"
- Um 08:00 führt `RuleScheduler` die Regel aus → Gerät schaltet sich ein
- Frontend empfängt WebSocket-Nachricht `messageType: "ruleNotification"`, `success: true`
- Snackbar erscheint: „Morgen-Routine ausgeführt: Schlafzimmer-Licht eingeschaltet"
- Benachrichtigung erscheint im Notification-Panel, Badge-Zähler erhöht sich

### Szenario 2: EVENT-Regel fehlschlägt
- Benutzer hat eine Regel konfiguriert, deren Aktionsgerät inzwischen gelöscht wurde
- Gerätezustand ändert sich → Regel wird evaluiert → Ausführung schlägt fehl
- Frontend empfängt `success: false`, `errorMessage: "Gerät nicht verfügbar"`
- Snackbar erscheint in Warn-Farbe: „Nacht-Modus fehlgeschlagen: Gerät nicht verfügbar"
- Benachrichtigung erscheint im Panel

### Szenario 3: Benutzer öffnet Notification-Panel
- Mehrere Regeln haben in der Session gefeuert
- Benutzer klickt auf Glocken-Icon → Panel öffnet sich mit Liste aller Benachrichtigungen
- Nach Öffnen des Panels wird der Badge-Zähler auf 0 zurückgesetzt

---

## Technical Context

### Backend
- `RuleService.executeRule()` wird nach erfolgreicher Ausführung erweitert: `DeviceWebSocketHandler.broadcastRuleNotification()` aufrufen
- `RuleService.evaluateRulesForDevice()` und `evaluateTimeRules()` catch-Blöcke senden Fehler-Benachrichtigungen
- Neues DTO: `RuleNotificationDto` (kein Entity, kein Repository, kein Flyway-Script)

### Frontend
- `RealtimeService`: neues `ruleNotifications$` Observable
- `ShellComponent`: abonniert `ruleNotifications$`, zeigt Snackbar, verwaltet Notification-Liste (in-memory)
- Badge-Zähler: Anzahl ungelesener Benachrichtigungen seit letztem Panel-Öffnen

---

## Acceptance Criteria (aus Issue)

- [x] Benachrichtigung bei erfolgreicher Regelausführung sichtbar (Toast + Panel)
- [x] Benachrichtigung bei fehlgeschlagener Regelausführung mit Fehlergrund sichtbar (Toast + Panel)
