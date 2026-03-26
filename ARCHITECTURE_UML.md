# Architecture UML

## System Overview

```mermaid
graph TD
    Browser["Browser\nAngular SPA"]

    subgraph Backend["Spring Boot Backend"]
        Auth["Spring Security\n(JWT + BCrypt)"]
        DevSvc["DeviceService\n(Rooms + Devices)"]
        AutoSvc["AutomationService\n(Rules + Schedules)"]
        SceneSvc["SceneService"]
        EnergySvc["EnergyService"]
        LogSvc["ActivityLogService"]
    end

    subgraph Infrastructure["Infrastructure (Docker)"]
        DB[("PostgreSQL")]
    end

    Browser -- "REST (HTTPS)" --> Auth
    Browser -- "REST (HTTPS) + JWT" --> DevSvc
    Browser -- "REST (HTTPS) + JWT" --> AutoSvc
    Browser -- "REST (HTTPS) + JWT" --> SceneSvc
    Browser -- "REST (HTTPS) + JWT" --> EnergySvc
    Browser -- "REST (HTTPS) + JWT" --> LogSvc

    Auth --> DB
    DevSvc --> DB
    AutoSvc --> DB
    SceneSvc --> DB
    EnergySvc --> DB
    LogSvc --> DB

    AutoSvc --> LogSvc
    SceneSvc --> DevSvc
    AutoSvc --> DevSvc
```

**Key design decisions:**
- **Authentication** is handled by Spring Security with BCrypt password hashing and self-issued JWTs — satisfies NFR-02
- **PostgreSQL** runs in a Docker container, configured via `docker-compose.yml` committed to the repository — all developers share the same DB setup
- **Real-time device state updates** are pushed via WebSocket (STOMP) from the backend — no external infrastructure needed
- **MQTT / IoT** is out of scope (virtual devices only); FR-18 is an optional extension

---

## Class Diagram

```mermaid
classDiagram
    class User {
        UUID id
        String email
        String passwordHash
        UserRole role
    }
    class Room {
        UUID id
        String name
        UUID ownerId
    }
    class Device {
        UUID id
        String name
        DeviceType type
        UUID roomId
        DeviceState state
    }
    class Scene {
        UUID id
        String name
        List~SceneAction~ actions
    }
    class Rule {
        UUID id
        RuleTrigger trigger
        RuleAction action
        Boolean enabled
    }
    class Schedule {
        UUID id
        String cronExpression
        RuleAction action
        Boolean isVacationOverride
    }
    class ActivityEntry {
        UUID id
        UUID deviceId
        LocalDateTime timestamp
        String triggeredBy
    }
    class EnergyRecord {
        UUID id
        UUID deviceId
        Double wattsConsumed
        LocalDate date
    }

    User "1" --> "many" Room : owns
    Room "1" --> "many" Device : contains
    Scene "1" --> "many" Device : references
    Rule --> Device : targets
    Schedule --> Device : targets
    Device "1" --> "many" ActivityEntry : logs
    Device "1" --> "many" EnergyRecord : tracks
```

**Notes:**
- `passwordHash` is managed by Spring Security (BCrypt) — plain-text passwords are never stored or logged (NFR-02)
- `UserRole` (Owner / Member) is enforced by Spring Security method-level authorization
- DB schema is managed via Flyway migrations — version-controlled and shared across all developers
