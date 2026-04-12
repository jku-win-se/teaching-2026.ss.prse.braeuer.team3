# Business Logic Model — Unit 2: sse-frontend

**Datum**: 2026-04-12  
**Story**: US-008 / FR-07 — Echtzeit-Zustandsanzeige

## Kernkonzepte

### RealtimeService
- Öffnet `EventSource` zu `GET /api/sse/devices?token=<JWT>`
- JWT wird über `AuthService.getToken()` aus `sessionStorage` gelesen
- Emittiert `DeviceDto`-Objekte via RxJS `Subject<DeviceDto>`
- Verwaltet Verbindungszustand: `'connected' | 'disconnected' | 'reconnecting'`
- Reconnect-Logik: nach `onerror` → 5 Sekunden warten → neu verbinden (max. 10 Versuche)
- Cleanup: `EventSource.close()` bei Destroy

### RoomsComponent (modifiziert)
- Injiziert `RealtimeService`, abonniert `deviceUpdates$`
- Auf jedes eingehende Event: Suche Device in `this.devices` per `String(event.id) === device.id`
- Bei Treffer: immutable State-Update via `device.state = { ... }`
- SSE-Verbindung öffnen in `ngOnInit`, schließen via `takeUntilDestroyed()`

### ConnectionStatusComponent (neu)
- Empfängt `@Input() state: 'connected' | 'disconnected' | 'reconnecting'`
- Zeigt Warning-Banner wenn `state !== 'connected'`
- Kein Meldungstext wenn verbunden

## Datenfluss

```
Backend DeviceService.updateState()
  → SseEmitterService.broadcast()
  → EventSource (Browser)
  → RealtimeService.deviceUpdates$ (Observable<DeviceDto>)
  → RoomsComponent: suche Device per id, update state
  → DeviceCardComponent: re-rendert via @Input()-Binding
```

## Fehlerbehandlung

| Szenario | Verhalten |
|---|---|
| `EventSource` `onerror` | `connectionState` → `'reconnecting'`, Banner erscheint |
| Token abgelaufen / 401 | EventSource schließt, Banner bleibt |
| Reconnect erfolgreich | `connectionState` → `'connected'`, Banner verschwindet |
| DeviceDto.id passt nicht | Update wird ignoriert (kein Fehler) |
