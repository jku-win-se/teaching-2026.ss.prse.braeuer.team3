# Component Methods — FR-07: Echtzeit-Zustandsanzeige

> Detaillierte Business-Logik wird im Functional Design (Construction Phase) ausgearbeitet.
> Hier: Methodensignaturen, Zweck und I/O-Typen.

---

## Backend — SseEmitterService

```java
// Registriert einen neuen Emitter für den Benutzer mit der gegebenen E-Mail
public SseEmitter addEmitter(String userEmail)
// → SseEmitter  (fertig konfiguriert, Timeout gesetzt)

// Sendet das aktualisierte DeviceResponse als SSE-Event an alle Emitter des Benutzers
public void broadcast(String userEmail, DeviceResponse deviceResponse)
// → void  (fehlerhafte Emitter werden still entfernt)

// Entfernt einen bestimmten Emitter aus der Verwaltung (z.B. nach Timeout)
void removeEmitter(String userEmail, SseEmitter emitter)
// → void  (package-private, intern)
```

---

## Backend — SseController

```java
// GET /api/sse/devices?token=<jwt>
// Öffnet einen SSE-Stream für den authentifizierten Benutzer
public SseEmitter subscribe(Principal principal)
// → SseEmitter  (HTTP 200, Content-Type: text/event-stream)
```

---

## Backend — JwtQueryParamFilter

```java
// Wird einmalig pro Request für /api/sse/** aufgerufen
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
    throws ServletException, IOException
// → void  (setzt SecurityContext oder leitet mit 401 ab)
```

---

## Backend — DeviceService (neue Methode / geändertes Verhalten)

```java
// Bestehende Signatur — unverändert; NEU: ruft intern sseEmitterService.broadcast() auf
public DeviceResponse updateState(String email, Long roomId, Long deviceId,
                                  DeviceStateRequest request)
// → DeviceResponse  (wie bisher)
```

---

## Frontend — RealtimeService

```typescript
// Öffnet die SSE-Verbindung und initialisiert Reconnect-Logik
connect(): void

// Schließt die SSE-Verbindung (z.B. beim Logout)
disconnect(): void

// Observable der eingehenden Gerätezustands-Events
readonly deviceUpdates$: Observable<DeviceDto>

// Observable des aktuellen Verbindungsstatus (true = verbunden)
readonly connected$: Observable<boolean>
```

---

## Frontend — ConnectionStatusComponent

```typescript
// Inputs/Outputs
// Keine direkten @Input-Properties — liest connected$ direkt aus RealtimeService
```

---

## Frontend — RoomsComponent (neue Logik)

```typescript
// Bestehende Methode, erweitert um SSE-Subscription
ngOnInit(): void
// NEU: abonniert realtimeService.deviceUpdates$ und aktualisiert devices-Array

// Neue private Hilfsmethode
private applyDeviceUpdate(updated: DeviceDto): void
// Ersetzt das Gerät im lokalen devices-Array anhand der ID
```

---

## Frontend — DeviceCardComponent (Anpassung)

```typescript
// Input: Gerätezustand von außen (RoomsComponent verwaltet den State)
@Input() device: DeviceDto
// Kein lokales State-Kopieren mehr; Änderungen via updateState()-Aufruf
```
