# Business Logic Model — Unit 1: sse-backend

## Ablauf 1: SSE-Verbindungsaufbau

```
Client → GET /api/sse/devices?token=<jwt>
         │
         ▼
JwtQueryParamFilter
  1. ?token-Parameter lesen
  2. Falls leer/ungültig → 401 Unauthorized, Abbruch
  3. E-Mail aus JWT extrahieren
  4. User aus DB laden (via UserRepository)
  5. UsernamePasswordAuthenticationToken in SecurityContext setzen
         │
         ▼
SseController.subscribe(Principal principal)
  1. E-Mail aus Principal lesen
  2. SseEmitterService.addEmitter(email) aufrufen
  3. SseEmitter zurückgeben (HTTP 200, text/event-stream)
         │
         ▼
SseEmitterService.addEmitter(email)
  1. Neuen SseEmitter mit Timeout (30 Min.) erstellen
  2. onCompletion-Callback: removeEmitter(email, emitter)
  3. onTimeout-Callback: emitter.complete() → removeEmitter(email, emitter)
  4. onError-Callback: removeEmitter(email, emitter)
  5. Emitter in emitterMap[email] eintragen
  6. SseEmitter zurückgeben
```

## Ablauf 2: Gerätezustand-Änderung → Event-Push

```
REST: PATCH /api/rooms/{roomId}/devices/{deviceId}/state
         │
         ▼
DeviceService.updateState(email, roomId, deviceId, request)
  1. Gerät laden + validieren (wie bisher)
  2. State-Felder aktualisieren (partielle Aktualisierung, null = unverändert)
  3. Device in DB speichern
  4. DeviceResponse aufbauen
  5. NEU: sseEmitterService.broadcast(email, deviceResponse)
  6. DeviceResponse zurückgeben (HTTP 200)
         │
         ▼
SseEmitterService.broadcast(email, deviceResponse)
  1. emitterMap[email] abrufen → leer: return (kein Fehler)
  2. Für jeden Emitter in der Liste:
     a. SseEmitter.event() mit name="device-update" + data=JSON(deviceResponse) senden
     b. Bei SendFailedException oder Exception: emitter.completeWithError(e), removeEmitter(email, emitter)
  3. Fehlerhafte Emitter still entfernen — andere Emitter unberührt (NFR-05)
```

## Ablauf 3: Verbindungs-Cleanup

```
Timeout-Event (30 Min. ohne Aktivität):
  SseEmitter.onTimeout → emitter.complete() → removeEmitter(email, emitter)

Verbindungsfehler (Client trennt):
  SseEmitter.onError → removeEmitter(email, emitter)

Normales Ende (Client-seitig):
  SseEmitter.onCompletion → removeEmitter(email, emitter)
```

## Ablauf 4: JWT-Validierung (JwtQueryParamFilter)

```
1. Request-URI prüfen: nur /api/sse/** bearbeiten, sonst weiterleiten
2. ?token Parameter lesen
3. Falls leer → response.sendError(401), Abbruch
4. JwtUtil.isValid(token) prüfen
5. Falls ungültig/abgelaufen → response.sendError(401), Abbruch
6. E-Mail extrahieren via JwtUtil.extractEmail(token)
7. UserDetails laden via UserDetailsService (UserRepository)
8. UsernamePasswordAuthenticationToken erstellen + in SecurityContextHolder setzen
9. filterChain.doFilter() aufrufen (weiterleiten an SseController)
```
