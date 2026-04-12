# Requirement Verification Questions — FR-07: Echtzeit-Zustandsanzeige

**Ticket**: FR-07 / US-008  
**Datum**: 2026-04-12  

Bitte beantwortet alle Fragen, indem ihr `[Answer]:` direkt nach der jeweiligen Frage befüllt.  
Nutzt die Buchstaben-Optionen (A, B, C, …) oder wählt **X** für eine eigene Antwort.

---

## Frage 1 — Technologieansatz für Echtzeit-Kommunikation

Welchen Ansatz soll für die Echtzeit-Zustandsanzeige verwendet werden?

- **A)** **WebSocket / STOMP** — Bidirektionale Verbindung; Server kann jederzeit Nachrichten pushen. Erfordert `spring-boot-starter-websocket` im Backend und `@stomp/stompjs` im Frontend. Gut geeignet, wenn in Zukunft auch der Client aktiv über den Socket steuern soll.
- **B)** **Server-Sent Events (SSE)** — Unidirektionaler Push vom Server zum Client über Standard-HTTP. Keine neuen Browser-Bibliotheken nötig (Angular `EventSource`). Einfacher zu implementieren, ausreichend für rein empfangende Clients.
- **C)** **Polling** — Frontend fragt regelmäßig per HTTP nach (z. B. alle 2–5 s). Kein echter Push, erhöhte Last, aber minimaler Implementierungsaufwand.
- **X)** Andere Lösung (bitte beschreiben):

[Answer]: B

---

## Frage 2 — Umfang der Echtzeit-Updates

Welche Zustandsänderungen sollen in Echtzeit übertragen werden?

- **A)** Alle Änderungen, die ein Benutzer an **eigenen Geräten** über die UI durchführt (Self-Echo: eigene Aktionen werden bestätigt zurückgespiegelt).
- **B)** Änderungen, die von **anderen Clients** desselben Benutzers ausgelöst werden (Multi-Tab / Multi-Browser-Szenario).
- **C)** Beide: A + B (eigene UI-Aktionen + parallele Sessions).
- **D)** Zukünftig auch externe Geräte-Trigger (z. B. Sensor-Schwellwert-Ereignis), aber für FR-07 nur A oder B.
- **X)** Andere Abgrenzung:

[Answer]: Einfach immer der Stand des backends auch im frontend anzeigen

---

## Frage 3 — Authentifizierung der Echtzeit-Verbindung

Wie soll die WebSocket-/SSE-Verbindung authentifiziert werden?

- **A)** JWT-Token wird beim Verbindungsaufbau als Query-Parameter mitgegeben (`?token=…`).
- **B)** JWT-Token wird im HTTP-Header beim initialen Handshake mitgegeben (für SSE über `EventSource` mit `fetch`-Wrapper).
- **C)** Kein eigenes Auth für die Real-Time-Verbindung — Zustandsänderungen werden nur über REST ausgelöst, die Real-Time-Verbindung ist session-gebunden.
- **X)** Andere Lösung:

[Answer]: A

---

## Frage 4 — Granularität der Topics/Events

Auf welcher Ebene sollen Clients Updates abonnieren?

- **A)** Pro **Benutzer**: Der Client empfängt alle Änderungen aller seiner Geräte über einen einzigen Channel (`/user/{id}/devices`).
- **B)** Pro **Raum**: Der Client abonniert nur den Raum, den er gerade ansieht (`/user/{id}/rooms/{roomId}/devices`).
- **C)** Pro **Gerät**: Jedes Gerät hat einen eigenen Channel (`/user/{id}/devices/{deviceId}`).
- **X)** Andere Granularität:

[Answer]: A

---

## Frage 5 — Verhalten bei Verbindungsverlust

Was soll passieren, wenn die Echtzeit-Verbindung abbricht?

- **A)** Automatischer Reconnect nach kurzer Verzögerung (z. B. 3 s), UI zeigt keinen Fehler.
- **B)** Automatischer Reconnect + kurze Warnanzeige ("Verbindung unterbrochen, stelle wieder her…").
- **C)** Kein automatischer Reconnect; Benutzer muss Seite manuell neu laden.
- **X)** Andere Strategie:

[Answer]: B

---

## Frage 6 — Initiales Laden vs. Real-Time-Sync

Wie soll das erste Laden der Gerätezustände beim Öffnen der Seite funktionieren?

- **A)** Wie bisher: REST-GET beim Laden, dann Real-Time-Updates als Delta.
- **B)** Alles über den Real-Time-Channel: Verbindung aufbauen, dann Snapshot-Nachricht pushen, danach Deltas.
- **X)** Andere Strategie:

[Answer]: A

---

## Frage 7 — Testanforderungen

Welche Tests werden für FR-07 erwartet?

- **A)** Nur Unit-Tests (Backend-Service-Methode + Angular-Service-Methode).
- **B)** Unit-Tests + Integrationstests (Spring Boot `@SpringBootTest` mit WebSocket-Test-Client).
- **C)** Unit-Tests + Integrationstests + Ende-zu-Ende-Tests (z. B. Playwright/Cypress).
- **X)** Andere Anforderungen:

[Answer]:Nur unit tests im backend
