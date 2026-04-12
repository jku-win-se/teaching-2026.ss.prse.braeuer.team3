# Domain Entities — Unit 1: sse-backend

## Bestehende Entitäten (unverändert)

`DeviceResponse` — wird als Event-Payload über den SSE-Stream gesendet:
| Feld | Typ | Beschreibung |
|------|-----|--------------|
| id | Long | Geräte-ID |
| name | String | Anzeigename |
| type | String | SWITCH / DIMMER / THERMOSTAT / SENSOR / COVER |
| stateOn | Boolean | Ein/Aus |
| brightness | Integer | 0–100 (nur DIMMER) |
| temperature | Double | Grad (nur THERMOSTAT) |
| sensorValue | Double | Messwert (nur SENSOR) |
| coverPosition | Integer | 0–100 (nur COVER) |

## Neue Konzepte

### EmitterRegistry
- Konzept (kein neues Domain-Objekt): interne Datenstruktur in `SseEmitterService`
- `Map<String /*userEmail*/, List<SseEmitter>>` — hält alle aktiven Verbindungen pro Benutzer
- Lifecycle: Emitter wird bei Verbindungsaufbau hinzugefügt; bei Timeout, Fehler oder Completion entfernt

### SseEvent
- Kein eigenes DTO — der Payload ist direkt ein serialisiertes `DeviceResponse`-Objekt (JSON)
- Event-Name (SSE `event:`-Feld): `device-update`
- Event-Data (SSE `data:`-Feld): JSON-String des `DeviceResponse`
