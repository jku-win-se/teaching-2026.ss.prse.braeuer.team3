# Unit of Work Story Map — FR-07: Echtzeit-Zustandsanzeige

## Story-zu-Unit-Zuordnung

| User Story / AC | Unit | Komponente |
|-----------------|------|------------|
| US-008: Als Benutzer möchte ich den aktuellen Zustand jedes Geräts in Echtzeit sehen | Unit 1 + Unit 2 | SseEmitterService + RealtimeService |
| AC-1: Zustandsänderungen ohne manuelles Neuladen | Unit 1: DeviceService.broadcast() → Unit 2: RoomsComponent.applyDeviceUpdate() | SSE-Pipeline |
| AC-2: Anzeige konsistent mit tatsächlichem Gerätezustand | Unit 1: SseEmitterService sendet exaktes DeviceResponse | SseEmitterService |

## Feature-zu-Klassen-Mapping

```
US-008 Echtzeit-Zustandsanzeige
├── [Unit 1 — sse-backend]
│   ├── SseEmitterService       → Emitter-Verwaltung, broadcast()
│   ├── SseController           → GET /api/sse/devices?token=
│   ├── JwtQueryParamFilter     → Auth für SSE-Endpunkt
│   ├── DeviceService (mod.)    → broadcast() nach updateState()
│   └── SecurityConfig (mod.)   → Filter-Chain-Erweiterung
│
└── [Unit 2 — sse-frontend]
    ├── RealtimeService         → EventSource, Reconnect, deviceUpdates$
    ├── ConnectionStatusComponent → Verbindungsstatus-Banner
    ├── RoomsComponent (mod.)   → SSE-Subscription, applyDeviceUpdate()
    └── DeviceCardComponent (mod.) → @Input-only State
```

## Abdeckung
- **FR-07** vollständig abgedeckt: 2 Units × alle Akzeptanzkriterien ✓
- **NFR-01** (Latenz): Unit 1 — direktes broadcast() nach DB-Save
- **NFR-03** (Coverage ≥75%): Unit 1 — SseEmitterServiceTest
- **NFR-04** (PMD): beide Units — PMD-konforme Java-Generierung
- **NFR-05** (Reliability): Unit 1 — fehlerhafte Emitter werden entfernt, andere unberührt
- **NFR-06** (Javadoc): Unit 1 — alle public Klassen/Methoden
