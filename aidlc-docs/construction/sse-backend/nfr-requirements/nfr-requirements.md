# NFR Requirements — Unit 1: sse-backend

## NFR-01 — Performance
- **Anforderung**: SSE-Event muss ≤ 2 s nach dem `PATCH /state`-Request beim Client ankommen
- **Konkretisierung für Unit 1**: `broadcast()` wird synchron nach `device.save()` aufgerufen — kein asynchroner Delay. Bei ≤ 10 simultanen Emittern akzeptabel.
- **Messlatte**: Unter Normallast (≤ 10 parallele Geräte) kein spürbarer Lag

## NFR-02 — Security
- **Anforderung**: JWT-Validierung vor jedem SSE-Stream
- **Konkretisierung für Unit 1**: `JwtQueryParamFilter` validiert `?token=` via `JwtUtil.isValid()`. Abgelaufene / ungültige Token → 401, kein Stream öffnen.
- **Passwörter**: nicht betroffen (kein neues Auth)

## NFR-03 — Test Coverage (≥ 75 % Line Coverage)
- **Anforderung**: ≥ 75 % Line Coverage auf non-UI Business-Logic-Klassen
- **Konkretisierung für Unit 1**: `SseEmitterService` wird vollständig durch `SseEmitterServiceTest` abgedeckt (addEmitter, broadcast mit 0/1/n Emittern, Fehler-Cleanup)
- **JwtQueryParamFilter**: Coverage via vorhandener Security-Test-Infrastruktur

## NFR-04 — Code Quality (PMD)
- **Anforderung**: Keine critical/high PMD-Violations; CI schlägt fehl bei Verletzungen
- **Konkretisierung für Unit 1**:
  - Keine leeren Catch-Blöcke (Exception wird in `broadcast()` per `completeWithError` behandelt)
  - Keine unbenutzten Imports / Variablen
  - Kein `System.out.println` — nur `Logger`/`log.warn()` falls nötig
  - Keine überlangen Methoden (broadcast ≤ 30 Zeilen)
  - Spezifische Exception-Typen statt generischem `Exception` wo möglich

## NFR-05 — Reliability
- **Anforderung**: Einzelne Fehler dürfen andere Geräte/Verbindungen nicht beeinflussen
- **Konkretisierung für Unit 1**: In `broadcast()` werden fehlerhafte Emitter per try/catch isoliert entfernt — andere Emitter-Iterationen laufen weiter (kein `throw` aus der Schleife)

## NFR-06 — Documentation (Javadoc)
- **Anforderung**: Alle `public` Klassen/Interfaces/Methoden in domain/, service/, controller/ müssen Javadoc haben
- **Konkretisierung für Unit 1**:
  - `SseEmitterService`: Klassen-Javadoc + `addEmitter()`, `broadcast()`, `removeEmitter()`
  - `SseController`: Klassen-Javadoc + `subscribe()`
  - `JwtQueryParamFilter`: Klassen-Javadoc + `doFilterInternal()`
  - Minimum: Beschreibungssatz + `@param` + `@return` / `@throws` wo zutreffend
