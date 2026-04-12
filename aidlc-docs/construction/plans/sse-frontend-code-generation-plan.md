# Code Generation Plan — Unit 2: sse-frontend

**Datum**: 2026-04-12  
**Workspace Root**: /Users/simonfalkner/Documents/Uni/PR_SE/teaching-2026.ss.prse.braeuer.team3  
**Projekt-Typ**: Brownfield — bestehende Dateien werden modifiziert; keine Duplikate

## Story-Referenz
- US-008 / FR-07: Echtzeit-Zustandsanzeige ohne Reload
- AC-1: Zustandsänderungen ohne manuelles Neuladen
- AC-2: Anzeige konsistent mit Gerätezustand im Backend

## Kontext & Abhängigkeiten
- Bestehender Code: `AuthService.getToken()`, `RoomsComponent`, `DeviceCardComponent`, `Device` / `DeviceState` (models.ts)
- `DeviceDto` aus `device.service.ts` wird als SSE-Payload-Typ wiederverwendet
- Backend-Endpoint: `GET /api/sse/devices?token=<JWT>` → `text/event-stream`, Event-Name: `device-update`

---

## Schritte

### Schritt 1 — RealtimeService erstellen
- [ ] Datei: `frontend/src/app/core/realtime.service.ts`
- [ ] Aktion: NEU erstellen
- [ ] Inhalt:
  - `@Injectable({ providedIn: 'root' })`
  - `export type ConnectionState = 'connected' | 'disconnected' | 'reconnecting'`
  - `private connectionState$ = new BehaviorSubject<ConnectionState>('disconnected')`
  - `readonly state$ = this.connectionState$.asObservable()`
  - `private readonly deviceSubject = new Subject<DeviceDto>()`
  - `readonly deviceUpdates$ = this.deviceSubject.asObservable()`
  - `private eventSource: EventSource | null = null`
  - `private reconnectAttempts = 0`, `private readonly MAX_RECONNECT = 10`
  - `connect()`: liest Token via `AuthService.getToken()`, baut URL `http://localhost:8080/api/sse/devices?token=...`, erstellt `EventSource`
    - `onopen`: `connectionState$.next('connected')`, `reconnectAttempts = 0`
    - `addEventListener('device-update', ...)`: JSON.parse → `deviceSubject.next(dto)`
    - `onerror`: `connectionState$.next('reconnecting')`, Reconnect nach 5s wenn `< MAX_RECONNECT`
  - `disconnect()`: `eventSource?.close()`, `connectionState$.next('disconnected')`
  - Strict TypeScript, kein `console.log`

### Schritt 2 — ConnectionStatusComponent erstellen
- [ ] Datei: `frontend/src/app/shared/components/connection-status/connection-status.component.ts`
- [ ] Aktion: NEU erstellen
- [ ] Inhalt:
  - `@Component({ selector: 'app-connection-status', standalone: true, ... })`
  - `@Input() state: ConnectionState = 'connected'`
  - Inline-Template: Warning-Banner nur wenn `state !== 'connected'`
  - Zwei Texte: `'Reconnecting...'` (wenn `'reconnecting'`) und `'Connection lost. Real-time updates paused.'` (wenn `'disconnected'`)
  - Styling: gelber/orangefarbener Banner, `mat-icon warning`

### Schritt 3 — RoomsComponent modifizieren
- [ ] Datei: `frontend/src/app/features/rooms/rooms.component.ts`
- [ ] Aktion: MODIFY in-place
- [ ] Änderungen:
  - `RealtimeService` und `ConnectionStatusComponent` importieren
  - `ConnectionStatusComponent` zu `imports`-Array hinzufügen
  - `destroyRef = inject(DestroyRef)` oder `takeUntilDestroyed()` nutzen
  - In `ngOnInit`: `realtimeService.connect()` aufrufen, `deviceUpdates$` abonnieren
    - Auf Event: finde `device` in `this.devices` per `String(dto.id) === device.id`
    - Update: `device.state = { on: dto.stateOn, brightness: dto.brightness, temperature: dto.temperature, sensorValue: dto.sensorValue, sensorUnit: device.state.sensorUnit, coverPosition: dto.coverPosition }`
  - `connectionState$` via `async`-Pipe in Template binden
  - `<app-connection-status>` Banner am Seitenanfang (über `.page-container`)
  - Constructor: `RealtimeService` injizieren
  - `ngOnDestroy` oder `takeUntilDestroyed()` für Cleanup

### Schritt 4 — aidlc-state.md aktualisieren
- [ ] Datei: `aidlc-docs/aidlc-state.md`
- [ ] Aktion: MODIFY in-place
- [ ] Änderung: Code Generation (Unit 2: sse-frontend) als abgeschlossen markieren

---

## Verifikation nach Generierung
- [ ] `ng build` läuft fehlerfrei (kein TypeScript-Fehler)
- [ ] `ng lint` keine Errors
- [ ] Browser: Device-State ändert sich nach Backend-Aufruf ohne Page-Reload
- [ ] Browser: Warnung erscheint nach EventSource-Fehler
