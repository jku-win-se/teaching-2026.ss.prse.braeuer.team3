# Application Design Plan — FR-07: Echtzeit-Zustandsanzeige

**Datum**: 2026-04-12  
**Basis**: requirements.md + reverse-engineering artifacts + execution-plan.md

Alle Design-Entscheidungen sind durch die Requirements-Antworten bereits eindeutig festgelegt.
Keine offenen Klärungsfragen.

## Artefakte zu erstellen

- [x] `components.md` — Komponentendefinitionen + Verantwortlichkeiten
- [x] `component-methods.md` — Methodensignaturen + I/O-Typen
- [x] `services.md` — Service-Definitionen und Orchestrierung
- [x] `component-dependency.md` — Abhängigkeiten und Kommunikationsmuster
- [x] `application-design.md` — konsolidiertes Dokument

## Getroffene Design-Entscheidungen

| Entscheidung | Wahl | Begründung |
|---|---|---|
| SSE-Auth-Filter | Dedizierter `JwtQueryParamFilter` | Saubere Trennung; bestehender `JwtAuthFilter` bleibt unverändert |
| Emitter-Storage | `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` | Thread-sicher; mehrere Tabs pro User möglich |
| Frontend-SSE-Wrapper | RxJS `Observable<DeviceResponse>` | Passt zu Angular-Patterns; automatisches Unsubscribe via `takeUntilDestroyed` |
| Reconnect-Banner | `ConnectionStatusComponent` (Standalone) | Wiederverwendbar; entkoppelt von RoomsComponent |
| SSE-Endpunkt-Lage | Neuer `SseController` (nicht in DeviceController) | Single Responsibility; vermeidet Aufblähung des DeviceControllers |
