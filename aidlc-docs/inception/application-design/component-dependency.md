# Component Dependencies — FR-07: Echtzeit-Zustandsanzeige

## Abhängigkeitsmatrix

| Komponente | Hängt ab von | Art der Abhängigkeit |
|---|---|---|
| `SseController` | `SseEmitterService`, `JwtUtil` | Constructor Injection |
| `SseEmitterService` | — | Keine externen Abhängigkeiten |
| `JwtQueryParamFilter` | `JwtUtil`, `UserRepository` | Constructor Injection |
| `DeviceService` | `SseEmitterService` *(neu)*, DeviceRepository, RoomRepository, UserRepository | Constructor Injection |
| `SecurityConfig` | `JwtQueryParamFilter` *(neu)*, JwtAuthFilter | Bean-Injection |
| `RealtimeService` | `AuthService` | Angular DI |
| `RoomsComponent` | `RealtimeService` *(neu)*, DeviceService, RoomService | Angular DI |
| `ConnectionStatusComponent` | `RealtimeService` | Angular DI |
| `DeviceCardComponent` | — *(nur @Input)* | Input-Binding |

## Kommunikationsmuster

```mermaid
graph TD
    subgraph Backend
        SEC["SecurityConfig"]
        JQPF["JwtQueryParamFilter"]
        SC["SseController"]
        DS["DeviceService"]
        SSE["SseEmitterService"]
        DC["DeviceController"]
    end

    subgraph Frontend
        RT["RealtimeService"]
        RC["RoomsComponent"]
        CS["ConnectionStatusComponent"]
        DCC["DeviceCardComponent"]
        AS["AuthService"]
    end

    AS -- "getToken()" --> RT
    RT -- "GET /api/sse/devices?token=" --> SC
    JQPF -- "validiert token" --> SC
    SC -- "addEmitter(email)" --> SSE
    DC -- "updateState()" --> DS
    DS -- "broadcast(email, dto)" --> SSE
    SSE -- "SSE Event (JSON)" --> RT
    RT -- "deviceUpdates$" --> RC
    RT -- "connected$" --> CS
    RC -- "@Input device" --> DCC
    SEC -- "registriert" --> JQPF

    style SSE fill:#FFA726,stroke:#E65100,stroke-width:2px
    style RT fill:#FFA726,stroke:#E65100,stroke-width:2px
    style CS fill:#FFA726,stroke:#E65100,stroke-width:2px
    style JQPF fill:#FFA726,stroke:#E65100,stroke-width:2px
```

## Änderungsauswirkungen

| Geänderte Komponente | Auswirkung auf |
|---|---|
| `DeviceService.updateState()` | `SseEmitterService.broadcast()` wird neu aufgerufen |
| `SecurityConfig` | `JwtQueryParamFilter` wird in die Chain eingehängt |
| `RoomsComponent` | Abonniert zusätzlich `RealtimeService.deviceUpdates$` |
| `DeviceCardComponent` | Verwendet `@Input`-bound State statt lokalem State |
