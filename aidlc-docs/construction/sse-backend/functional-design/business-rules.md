# Business Rules — Unit 1: sse-backend

## Auth & Sicherheit

| Regel | Beschreibung |
|-------|--------------|
| BR-01 | Ein SSE-Stream darf nur für authentifizierte Benutzer geöffnet werden. Kein gültiger JWT → HTTP 401, kein Stream. |
| BR-02 | Der JWT wird ausschließlich aus dem `?token=`-Query-Parameter gelesen. HTTP-Header werden für den SSE-Endpunkt nicht geprüft. |
| BR-03 | Ein Emitter darf nur Events für den Benutzer empfangen, der den Stream geöffnet hat. Kein Cross-User-Broadcasting. |

## Emitter-Lifecycle

| Regel | Beschreibung |
|-------|--------------|
| BR-04 | Jeder Emitter hat einen Timeout von 30 Minuten. Nach Ablauf wird er automatisch beendet und aus der Registry entfernt. |
| BR-05 | Ein fehlerhafter Emitter (Exception beim Senden) wird sofort aus der Registry entfernt. Alle anderen Emitter desselben Benutzers bleiben aktiv (NFR-05). |
| BR-06 | Mehrere gleichzeitige Emitter pro Benutzer (z. B. mehrere Tabs) sind zulässig. Alle erhalten dasselbe Event. |

## Broadcasting

| Regel | Beschreibung |
|-------|--------------|
| BR-07 | Das SSE-Event wird nur nach einer erfolgreichen Persistierung in der DB gesendet (`save()` muss erfolgreich sein). |
| BR-08 | Hat ein Benutzer keine aktiven Emitter, wird `broadcast()` ohne Fehler beendet (kein Log, kein Exception). |
| BR-09 | Der Event-Payload ist das vollständige `DeviceResponse`-Objekt als JSON. Kein partielles/Delta-Format. |
| BR-10 | Der SSE-Event-Name ist `device-update` (für Frontend-seitiges `addEventListener`). |

## Validierung (unveränderte Regeln aus DeviceService)

| Regel | Beschreibung |
|-------|--------------|
| BR-11 | Der Benutzer muss Eigentümer des Raums sein, in dem sich das Gerät befindet. |
| BR-12 | Das Gerät muss im angegebenen Raum existieren. |
| BR-13 | `null`-Felder in `DeviceStateRequest` bedeuten "unverändert" (partielle Aktualisierung). |
