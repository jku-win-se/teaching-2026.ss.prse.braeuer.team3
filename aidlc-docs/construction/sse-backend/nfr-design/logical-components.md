# Logical Components — Unit 1: sse-backend

## Komponentenübersicht mit NFR-Verankerung

```
SseEmitterService
 ├── emitterMap: ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>
 │    └── [NFR-01] Thread-safe für parallele Broadcasts ohne Lock-Contention
 ├── addEmitter(email) → SseEmitter
 │    └── [NFR-05] Lifecycle-Callbacks (onTimeout, onError, onCompletion) → auto-removeEmitter
 ├── broadcast(email, deviceResponse)
 │    └── [NFR-05] Try-Catch-per-Iteration: fehlerhafter Emitter ≠ Abbruch für andere
 │    └── [NFR-01] Synchroner Aufruf nach save() → minimale Latenz
 └── removeEmitter(email, emitter) [package-private]
      └── [NFR-05] Bereinigung ohne Side-Effects

SseController
 ├── subscribe(Principal) → SseEmitter
 │    └── [NFR-02] Principal aus JwtQueryParamFilter; kein direktes Token-Handling
 └── [NFR-06] Klassen- + Methoden-Javadoc

JwtQueryParamFilter
 ├── doFilterInternal(request, response, chain)
 │    └── [NFR-02] ?token= validieren via JwtUtil.isValid(); 401 bei Fehler
 │    └── [NFR-04] Spezifische Exceptions; kein leerer Catch
 └── shouldNotFilter(request) → true für alles außer /api/sse/**
      └── [NFR-04] Klare Scope-Begrenzung

DeviceService (Modifikation)
 └── updateState() — NEU: sseEmitterService.broadcast(email, response)
      └── [NFR-01] Synchron nach save() → Event-Push ohne Zusatz-Latenz
      └── [NFR-05] broadcast() wirft keine Exception → updateState() bleibt stabil

SecurityConfig (Modifikation)
 └── JwtQueryParamFilter vor JwtAuthFilter in FilterChain
      └── [NFR-02] /api/sse/devices via ?token= authentifiziert, nicht via Bearer-Header
```

## Keine neuen Infrastruktur-Komponenten

- Keine Message Queue (Synchronous Broadcast ausreichend für ≤ 10 Devices)
- Kein Cache
- Kein neuer Docker-Service
- Kein neues Maven-Dependency
