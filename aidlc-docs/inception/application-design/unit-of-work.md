# Units of Work — FR-07: Echtzeit-Zustandsanzeige

## Unit 1 — sse-backend

**Schicht**: Spring Boot Backend  
**Ziel**: SSE-Infrastruktur im Backend bereitstellen — Emitter-Verwaltung, gesicherter SSE-Endpunkt, Event-Publishing nach Zustandsänderung

### Neue Dateien
| Datei | Zweck |
|-------|-------|
| `backend/src/main/java/at/jku/se/smarthome/service/SseEmitterService.java` | Emitter-Verwaltung + broadcast() |
| `backend/src/main/java/at/jku/se/smarthome/controller/SseController.java` | GET /api/sse/devices?token= |
| `backend/src/main/java/at/jku/se/smarthome/security/JwtQueryParamFilter.java` | JWT aus ?token= extrahieren und validieren |
| `backend/src/test/java/at/jku/se/smarthome/service/SseEmitterServiceTest.java` | Unit-Tests für SseEmitterService |

### Modifizierte Dateien
| Datei | Änderung |
|-------|----------|
| `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java` | Inject SseEmitterService; broadcast() in updateState() aufrufen |
| `backend/src/main/java/at/jku/se/smarthome/security/SecurityConfig.java` | JwtQueryParamFilter registrieren; /api/sse/devices öffnen |

### Akzeptanzkriterien
- `GET /api/sse/devices?token=<gültiger-jwt>` → HTTP 200, Content-Type: text/event-stream
- Nach `PATCH /state` empfangen alle SSE-Clients des Benutzers das Event ≤ 2 s
- Ungültiger/fehlender Token → HTTP 401
- Unit-Tests: SseEmitterService ≥ 75% Line Coverage (NFR-03)
- Keine PMD critical/high Violations (NFR-04)
- Vollständige Javadocs auf allen public Klassen/Methoden (NFR-06)

---

## Unit 2 — sse-frontend

**Schicht**: Angular 19 Frontend  
**Ziel**: SSE-Verbindung im Frontend konsumieren — RealtimeService, Reconnect-Logik, Zustandsaktualisierung in der UI, Verbindungsstatus-Anzeige

### Neue Dateien
| Datei | Zweck |
|-------|-------|
| `frontend/src/app/core/realtime.service.ts` | EventSource-Verbindung, Reconnect, Observable-Streams |
| `frontend/src/app/shared/components/connection-status/connection-status.component.ts` | Reconnect-Banner-Komponente |
| `frontend/src/app/shared/components/connection-status/connection-status.component.html` | Template für Reconnect-Banner |

### Modifizierte Dateien
| Datei | Änderung |
|-------|----------|
| `frontend/src/app/features/rooms/rooms.component.ts` | RealtimeService subscriben; applyDeviceUpdate() implementieren |
| `frontend/src/app/features/rooms/rooms.component.html` | ConnectionStatusComponent einbinden |
| `frontend/src/app/shared/components/device-card/device-card.component.ts` | State ausschließlich via @Input beziehen |

### Akzeptanzkriterien
- Gerätezustand ändert sich im UI ohne Seiten-Reload nach SSE-Event
- Bei Verbindungsabbruch: Banner sichtbar; nach Reconnect: Banner weg + States per REST aktualisiert
- SSE-Verbindung wird beim Logout getrennt
- TypeScript-Kompilierung und `ng lint` ohne Fehler
