# Business Rules — Unit 2: sse-frontend

**Datum**: 2026-04-12

## Verbindungsregeln

- BR-FE-01: `RealtimeService` öffnet die SSE-Verbindung genau einmal bei `ngOnInit` von `RoomsComponent`.
- BR-FE-02: Der JWT-Token wird als `?token=` Query-Parameter übergeben (nicht im Header, da `EventSource` keine Custom Headers unterstützt).
- BR-FE-03: Nach einem Verbindungsfehler wird max. 10 Mal mit 5 Sekunden Abstand reconnected.
- BR-FE-04: Beim Logout oder Destroy der `RoomsComponent` wird `EventSource.close()` aufgerufen.

## State-Update-Regeln

- BR-FE-05: Ein eingehendes SSE-Event aktualisiert nur `device.state`, nie `device.id`, `device.name` oder `device.type`.
- BR-FE-06: Wenn `DeviceDto.id` keinem Device in `this.devices` entspricht, wird das Event ignoriert.
- BR-FE-07: State-Updates sind immutabel: `device.state = { ...device.state, ... }` (triggert Angular Change Detection).

## UI-Regeln

- BR-FE-08: Das Warning-Banner ist nur sichtbar wenn `connectionState !== 'connected'`.
- BR-FE-09: Das Banner zeigt "Reconnecting..." wenn `state === 'reconnecting'`, sonst "Connection lost".
