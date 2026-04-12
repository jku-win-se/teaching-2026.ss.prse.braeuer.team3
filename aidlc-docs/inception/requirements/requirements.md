# Requirements — FR-07: Echtzeit-Zustandsanzeige

## Intent Analysis

| Feld | Inhalt |
|------|--------|
| **User Request** | FR-07 / US-008: Gerätezustände sollen in Echtzeit in der UI angezeigt werden, ohne manuelles Neuladen |
| **Request Type** | New Feature |
| **Scope Estimate** | Multiple Components (Backend: SSE-Infrastruktur + DeviceService; Frontend: RealtimeService + RoomsComponent + DeviceCard) |
| **Complexity Estimate** | Moderate |

---

## Functional Requirements

### FR-07.1 — SSE-Endpunkt (Backend)
Das Backend stellt einen Server-Sent Events (SSE)-Endpunkt bereit:
- **Endpunkt**: `GET /api/sse/devices?token=<jwt>`
- Der Client übergibt den JWT als Query-Parameter `token`
- Der Server validiert den Token, identifiziert den Benutzer und öffnet einen persistenten SSE-Stream
- Pro verbundenem Client wird ein `SseEmitter` verwaltet

### FR-07.2 — Event-Push bei Zustandsänderung (Backend)
Wenn ein Gerätezustand über `PATCH /api/rooms/{roomId}/devices/{deviceId}/state` geändert wird:
- Publiziert `DeviceService.updateState()` das aktualisierte `DeviceResponse`-Objekt
- Alle aktiven SSE-Emitter des betreffenden Benutzers empfangen das Event
- Event-Format: JSON des `DeviceResponse`-Objekts

### FR-07.3 — Echtzeit-Update im Frontend
- Beim Öffnen der Räume-Ansicht verbindet sich der Client über `EventSource` mit dem SSE-Endpunkt (`?token=<jwt>`)
- Eingehende Events aktualisieren den entsprechenden Gerätezustand in der UI ohne Neuladen
- Konsistenz: Nach jedem SSE-Event ist der angezeigte Zustand identisch mit dem Backend-Stand

### FR-07.4 — Initiales Laden
- Gerätezustände werden beim Öffnen der Seite wie bisher per REST-GET geladen
- SSE liefert anschließend nur Deltas (Änderungs-Events)

### FR-07.5 — Reconnect-Verhalten
- Bei Verbindungsabbruch versucht der Client automatisch nach 3 s die Verbindung neu aufzubauen
- Während des Verbindungsverlusts zeigt die UI eine kurze Warnanzeige ("Verbindung unterbrochen...")
- Nach erfolgreichem Reconnect: stilles Verschwinden der Warnanzeige + REST-GET zum Abgleich der States

### FR-07.6 — Scope der Updates
- Der SSE-Channel ist pro Benutzer: Ein Client empfängt alle Zustandsänderungen aller eigenen Geräte
- Mehrere gleichzeitig geöffnete Browser-Tabs desselben Benutzers werden alle aktualisiert

---

## Non-Functional Requirements (aus localDocs/requirements_smarthome_en.md)

| ID | Kategorie | Anforderung | Relevanz für FR-07 |
|----|-----------|-------------|-------------------|
| NFR-01 | Performance | Antwort auf User-Interaktion ≤ 2 s bei ≤ 10 parallelen Geräten | SSE-Event muss innerhalb von 2 s nach State-Change beim Client ankommen |
| NFR-02 | Security | Passwörter nur gehasht; Token-Validierung | JWT-Validierung des `?token=` Query-Params im SSE-Endpunkt |
| NFR-03 | Test Coverage | ≥ 75 % Line Coverage auf non-UI Business-Logic | Neue Klassen (`SseEmitterService`) müssen durch Unit-Tests abgedeckt sein |
| NFR-04 | Code Quality | Keine critical/high PMD-Violations; Build schlägt fehl bei Verletzung | Neuer Java-Code muss PMD-konform sein (ruleset.xml) |
| NFR-05 | Reliability | Einzelne Gerätefehler graceful behandeln, System läuft weiter | Fehlerhafte SSE-Verbindungen dürfen andere Clients nicht beeinflussen |
| NFR-06 | Documentation | Javadoc auf allen public Klassen/Methoden in domain/, service/, repository/, controller/ | SseController, SseEmitterService und alle public Methoden erfordern Javadoc |

---

## Acceptance Criteria (aus Ticket)

- [ ] Zustandsänderungen werden **ohne manuelles Neuladen** in der UI angezeigt
- [ ] Die Anzeige ist **konsistent mit dem tatsächlichen Gerätezustand** im Backend

---

## Out of Scope (FR-07)

- Native Push-Notifications (E-Mail / SMS) → explizit nicht in Scope (requirements_smarthome_en.md Kap. 4)
- Integration mit externen IoT-Geräten (MQTT) → FR-18, separates Ticket
- Echtzeit-Sync über Benutzer-Grenzen hinweg (Multi-Tenant) → nicht in Scope

---

## Technische Entscheidungen (aus Klärungsfragen)

| Entscheidung | Wert | Begründung |
|---|---|---|
| Protokoll | **Server-Sent Events (SSE)** | Unidirektional (Server → Client) ausreichend; kein WebSocket-Overhead; Standard-HTTP |
| Auth | **JWT als Query-Param `?token=`** | `EventSource` API des Browsers unterstützt keine Custom Headers |
| Granularität | **Pro Benutzer** | Ein Channel liefert alle eigenen Geräte-Events |
| Initial-Load | **REST-GET + SSE-Delta** | Bewährtes Muster, minimaler Umbau des bestehenden Codes |
| Reconnect | **Automatisch + Warnanzeige** | Gute UX bei kurzen Verbindungsausfällen |
| Tests | **Unit-Tests Backend** | Nur Unit-Tests im Backend (kein Integrations-/E2E-Test für FR-07) |
