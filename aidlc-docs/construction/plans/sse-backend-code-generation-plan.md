# Code Generation Plan — Unit 1: sse-backend

**Datum**: 2026-04-12  
**Workspace Root**: /Users/simonfalkner/Documents/Uni/PR_SE/teaching-2026.ss.prse.braeuer.team3  
**Base Package**: `at.jku.se.smarthome`  
**Projekt-Typ**: Brownfield — bestehende Dateien werden modifiziert; keine Duplikate

## Story-Referenz
- US-008 / FR-07: Echtzeit-Zustandsanzeige ohne Reload
- AC-1: Zustandsänderungen ohne manuelles Neuladen
- AC-2: Anzeige konsistent mit Gerätezustand im Backend

## Kontext & Abhängigkeiten
- Bestehender Code: `DeviceService`, `SecurityConfig`, `JwtUtil`, `UserRepository`, `DeviceResponse`
- Neue Abhängigkeit: `spring-web` SseEmitter (bereits in pom.xml via `spring-boot-starter-web`)
- Keine pom.xml-Änderung nötig

---

## Schritte

### Schritt 1 — SseEmitterService erstellen
- [x] Datei: `backend/src/main/java/at/jku/se/smarthome/service/SseEmitterService.java`
- [ ] Aktion: NEU erstellen
- [ ] Inhalt:
  - `@Service`-annotierte Klasse mit Javadoc (NFR-06)
  - `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` emitterMap
  - `addEmitter(String userEmail)` → SseEmitter (30 Min. Timeout, Lifecycle-Callbacks)
  - `broadcast(String userEmail, DeviceResponse deviceResponse)` → void (try-catch-per-iteration, NFR-05)
  - `removeEmitter(String userEmail, SseEmitter emitter)` → void (package-private)
  - PMD-konform: keine leeren Catch-Blöcke, spezifische Exceptions (NFR-04)

### Schritt 2 — SseController erstellen
- [x] Datei: `backend/src/main/java/at/jku/se/smarthome/controller/SseController.java`
- [ ] Aktion: NEU erstellen
- [ ] Inhalt:
  - `@RestController`, `@RequestMapping("/api/sse")` mit Javadoc (NFR-06)
  - `subscribe(Principal principal)` → SseEmitter
    - `GET /api/sse/devices`, produces = `MediaType.TEXT_EVENT_STREAM_VALUE`
    - Delegiert an `sseEmitterService.addEmitter(principal.getName())`
  - Constructor Injection von SseEmitterService

### Schritt 3 — JwtQueryParamFilter erstellen
- [x] Datei: `backend/src/main/java/at/jku/se/smarthome/security/JwtQueryParamFilter.java`
- [ ] Aktion: NEU erstellen
- [ ] Inhalt:
  - Extends `OncePerRequestFilter` mit Javadoc (NFR-06)
  - `shouldNotFilter()`: true für alle Requests außer `/api/sse/**`
  - `doFilterInternal()`: ?token= lesen → JwtUtil.isValid() → 401 oder SecurityContext setzen → chain.doFilter()
  - Constructor Injection von JwtUtil + UserDetailsService

### Schritt 4 — SecurityConfig modifizieren
- [x] Datei: `backend/src/main/java/at/jku/se/smarthome/security/SecurityConfig.java`
- [x] Aktion: MODIFY in-place (keine Kopie)
- [ ] Änderungen:
  - `JwtQueryParamFilter` als Bean injizieren (Constructor)
  - In `filterChain`: `/api/sse/devices` zu `permitAll()` hinzufügen (Auth via Filter, nicht via Bearer)
  - `JwtQueryParamFilter` vor `JwtAuthFilter` in FilterChain einhängen: `addFilterBefore(jwtQueryParamFilter, JwtAuthFilter.class)`

### Schritt 5 — DeviceService modifizieren
- [x] Datei: `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java`
- [x] Aktion: MODIFY in-place (keine Kopie)
- [ ] Änderungen:
  - `SseEmitterService` per Constructor injizieren
  - In `updateState()` nach `deviceRepository.save()`: `sseEmitterService.broadcast(email, response)` aufrufen
  - Bestehende Javadocs aktualisieren (SseEmitterService in Abhängigkeitsliste)

### Schritt 6 — SseEmitterServiceTest erstellen
- [x] Datei: `backend/src/test/java/at/jku/se/smarthome/service/SseEmitterServiceTest.java`
- [ ] Aktion: NEU erstellen
- [ ] Inhalt (≥75% Coverage, NFR-03):
  - `addEmitter_returnsConfiguredEmitter` — prüft Timeout + Callbacks registriert
  - `broadcast_noEmitters_doesNotThrow` — leere Map → kein Fehler
  - `broadcast_singleEmitter_sendsEvent` — ein Emitter empfängt Event
  - `broadcast_multipleEmitters_allReceiveEvent` — mehrere Emitter (Multi-Tab)
  - `broadcast_failingEmitter_removedOthersStillReceive` — fehlerhafter Emitter wird entfernt, andere bleiben (NFR-05)
  - `removeEmitter_removesFromMap` — Bereinigung korrekt

---

## Verifikation nach Generierung
- [ ] Keine doppelten Dateien (z.B. `DeviceService_modified.java`)
- [ ] `mvn compile` läuft fehlerfrei (Schritt 1–5)
- [ ] `mvn pmd:check` keine critical/high Violations (Schritt 1–5)
- [ ] Alle public Methoden haben Javadoc (Schritt 1–5)
- [ ] `mvn test` — `SseEmitterServiceTest` alle 6 Tests grün
