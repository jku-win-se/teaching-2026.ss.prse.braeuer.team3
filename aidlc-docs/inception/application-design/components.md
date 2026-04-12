# Components — FR-07: Echtzeit-Zustandsanzeige

## Backend — Neue Komponenten

### SseEmitterService
- **Typ**: Spring Service (`@Service`)
- **Purpose**: Verwaltet alle aktiven SSE-Emitter pro Benutzer; versendet Gerätezustands-Events an alle verbundenen Clients eines Benutzers
- **Verantwortlichkeiten**:
  - Emitter für einen Benutzer registrieren und nach Ablauf/Fehler entfernen
  - Zustandsänderungs-Event (DeviceResponse als JSON) an alle Emitter eines Benutzers broadcasten
  - Thread-sicheres Management des Emitter-Speichers

### SseController
- **Typ**: Spring REST Controller (`@RestController`)
- **Purpose**: Stellt den SSE-Endpunkt `GET /api/sse/devices` bereit; validiert den JWT aus dem `?token=`-Query-Parameter und öffnet einen persistenten Event-Stream für den Benutzer
- **Verantwortlichkeiten**:
  - JWT aus Query-Param auslesen und den authentifizierten Benutzer bestimmen
  - Neuen `SseEmitter` anlegen und im `SseEmitterService` registrieren
  - SseEmitter mit `Content-Type: text/event-stream` zurückgeben

### JwtQueryParamFilter
- **Typ**: Spring Security Filter (`OncePerRequestFilter`)
- **Purpose**: Extrahiert den JWT aus dem `?token=`-Query-Parameter für den SSE-Endpunkt und setzt den SecurityContext — ohne den bestehenden `JwtAuthFilter` zu verändern
- **Verantwortlichkeiten**:
  - Nur für `/api/sse/**` aktiv
  - Token validieren und `UsernamePasswordAuthenticationToken` in den SecurityContext setzen

---

## Backend — Modifizierte Komponenten

### DeviceService (modifiziert)
- **Änderung**: Ruft nach jeder erfolgreichen Zustandsänderung (`updateState()`) den `SseEmitterService.broadcast()` auf
- **Neue Verantwortlichkeit**: Event-Publishing für Echtzeit-Updates

### SecurityConfig (modifiziert)
- **Änderung**: `/api/sse/devices` wird als öffentlicher Pfad für den Standard-JWT-Filter freigegeben (Auth erfolgt via `JwtQueryParamFilter`); `JwtQueryParamFilter` wird in die Filter-Chain eingehängt

---

## Frontend — Neue Komponenten

### RealtimeService
- **Typ**: Angular Injectable Service (`@Injectable({ providedIn: 'root' })`)
- **Purpose**: Baut die SSE-Verbindung zum Backend auf, liefert einen RxJS-Observable-Stream für Gerätezustands-Events und implementiert die Reconnect-Logik
- **Verantwortlichkeiten**:
  - `EventSource` mit `?token=<jwt>` öffnen
  - Eingehende Events in `DeviceDto`-Objekte deserialisieren und als `Observable<DeviceDto>` emittieren
  - Bei Verbindungsabbruch: automatischer Reconnect nach 3 s; Verbindungsstatus als `Observable<boolean>` publifizieren

### ConnectionStatusComponent
- **Typ**: Angular Standalone Component
- **Purpose**: Zeigt eine Material-Snackbar / ein Banner an, wenn die SSE-Verbindung unterbrochen ist oder wieder hergestellt wird
- **Verantwortlichkeiten**:
  - Verbindungsstatus von `RealtimeService` abonnieren
  - "Verbindung unterbrochen..." bei Abbruch anzeigen; Banner bei Wiederverbindung ausblenden

---

## Frontend — Modifizierte Komponenten

### RoomsComponent (modifiziert)
- **Änderung**: Abonniert `RealtimeService.deviceUpdates$`; bei eingehenden Events wird der betreffende Gerätezustand in der lokalen Liste aktualisiert

### DeviceCardComponent (modifiziert)
- **Änderung**: Empfängt Gerätezustand per `@Input()`-Binding und reagiert auf externe Zustandsänderungen (keine lokale State-Kopie mehr)
