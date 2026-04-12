# NFR Design Patterns — Unit 1: sse-backend

## Pattern 1 — Isolierter Fehler-Schutz (NFR-05)

**Problem**: Ein fehlerhafter SSE-Emitter soll nicht den Broadcast für andere Emitter stoppen.

**Pattern**: Try-Catch-per-Iteration mit stiller Bereinigung

```java
// In SseEmitterService.broadcast()
List<SseEmitter> emitters = emitterMap.getOrDefault(userEmail, new CopyOnWriteArrayList<>());
for (SseEmitter emitter : emitters) {
    try {
        emitter.send(SseEmitter.event().name("device-update").data(deviceResponse));
    } catch (IOException e) {
        emitter.completeWithError(e);
        removeEmitter(userEmail, emitter);
    }
}
```
- Exception wird **nicht** aus der Schleife geworfen
- Jeder Emitter wird unabhängig behandelt

---

## Pattern 2 — Lifecycle-Callbacks (NFR-01, NFR-05)

**Problem**: Emitter müssen bei Timeout/Fehler/Completion automatisch aus der Registry entfernt werden.

**Pattern**: Lambda-Callbacks auf SseEmitter

```java
// In SseEmitterService.addEmitter()
SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 Minuten
emitter.onCompletion(() -> removeEmitter(userEmail, emitter));
emitter.onTimeout(() -> {
    emitter.complete();
    removeEmitter(userEmail, emitter);
});
emitter.onError(e -> removeEmitter(userEmail, emitter));
```
- Kein Memory-Leak durch hängende Emitter
- Automatisches Cleanup ohne externe Scheduler

---

## Pattern 3 — Token-aus-Query-Parameter (NFR-02)

**Problem**: `EventSource` sendet keine Custom-Header; JWT muss über Query-Parameter übertragen werden.

**Pattern**: Pre-Authentifizierungs-Filter

```java
// In JwtQueryParamFilter.doFilterInternal()
String token = request.getParameter("token");
if (token == null || !jwtUtil.isValid(token)) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}
// ... SecurityContext setzen, dann filterChain.doFilter(request, response)
```
- Filter läuft nur für `/api/sse/**` (geprüft in `shouldNotFilter()` invertiert)
- Kein Eingriff in bestehenden `JwtAuthFilter`

---

## Pattern 4 — PMD-konforme Exception-Behandlung (NFR-04)

**Regel**: Kein leerer Catch-Block, keine generischen `Exception`-Catches ohne Begründung.

```java
// KORREKT — spezifisch + mit Aktion
try {
    emitter.send(...);
} catch (IOException e) {
    emitter.completeWithError(e);
    removeEmitter(userEmail, emitter);
}

// FALSCH — leer oder generisch ohne Aktion
try {
    emitter.send(...);
} catch (Exception e) { } // PMD: EmptyCatchBlock
```

---

## Pattern 5 — Javadoc-Struktur (NFR-06)

Template für alle public Methoden in neuen Klassen:

```java
/**
 * Kurzbeschreibung des Zwecks (erste Satz).
 * Optionale Detailbeschreibung.
 *
 * @param paramName Beschreibung des Parameters
 * @return Beschreibung des Rückgabewerts
 * @throws IOException wenn der SSE-Stream nicht erreichbar ist
 */
public ReturnType methodName(ParamType paramName) throws IOException { ... }
```
