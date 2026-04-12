# NFR Design — Unit 2: sse-frontend

**Datum**: 2026-04-12

## NFR-FE-01: Latenz ≤ 2s
- `EventSource` liefert Server-Push ohne Polling → Latenz durch Netzwerk, nicht durch Client
- Keine zusätzlichen Buffer-Mechanismen → Direktweiterleitung via `Subject.next()`

## NFR-FE-02: Auto-Reconnect + Warning-Banner
- `RealtimeService.connect()` setzt `connectionState` → `'reconnecting'` bei `onerror`
- Reconnect via `setTimeout(..., 5000)` — max. 10 Versuche
- `connectionState$` als `BehaviorSubject<ConnectionState>` → `RoomsComponent` bindet Banner über `async`-Pipe

## NFR-FE-03: Testbarkeit
- `RealtimeService` akzeptiert optionalen `eventSourceFactory`-Parameter (Default: `(url) => new EventSource(url)`)
- Tests übergeben eine Fake-Factory → kein DOM nötig
- `takeUntilDestroyed()` setzt DestroyRef voraus — Tests verwenden `TestBed`

## NFR-FE-05: Fehler-Isolation
- RxJS `catchError` in `deviceUpdates$` → fehlerhafte JSON-Parsing-Fehler werden geloggt, Stream bleibt offen

## NFR-FE-06: Strict TypeScript
- `DeviceSseEvent` Interface für eingehende SSE-Daten (entspricht `DeviceDto`)
- Keine Type-Assertions ohne Guards
