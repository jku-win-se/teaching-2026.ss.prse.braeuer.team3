# Architecture — SmartHome Orchestrator

## System Overview

SmartHome Orchestrator is a two-tier web application:

- **Backend**: Spring Boot 3.3.5 (Java 21), REST-API, JWT auth, PostgreSQL via JPA/Flyway
- **Frontend**: Angular 19 SPA, Angular Material UI, RxJS, SSE for real-time device updates
- **Database**: PostgreSQL 16 (Docker-managed)

---

## System Architecture Diagram

```mermaid
graph TB
    subgraph Browser["Browser (Angular 19 SPA)"]
        direction TB
        subgraph Pages["Pages / Features"]
            PAuth["Auth (Login · Register)"]
            PDash["Dashboard"]
            PRooms["Rooms"]
            PRules["Rules (Owner only)"]
            PSched["Schedules (Owner only)"]
            PScenes["Scenes"]
            PEnergy["Energy"]
            PLog["Activity Log (Owner only)"]
            PSettings["Settings"]
        end
        AuthInterceptor["AuthInterceptor (Bearer Token)"]
        AuthGuard["AuthGuard + OwnerGuard"]
    end

    subgraph Backend["Backend (Spring Boot 3.3.5 · Java 21)"]
        direction TB
        SEC["SecurityConfig + JwtAuthFilter"]
        subgraph Controllers["REST Controllers"]
            AuthCtrl["AuthController /api/auth"]
            RoomCtrl["RoomController /api/rooms"]
            DevCtrl["DeviceController /api/rooms/{id}/devices"]
            RuleCtrl["RuleController /api/rules"]
            SchedCtrl["ScheduleController /api/schedules"]
            SceneCtrl["SceneController /api/scenes"]
            EnergyCtrl["EnergyController /api/energy"]
            LogCtrl["ActivityLogController /api/activity-log"]
            MemberCtrl["MemberController /api/members"]
            SseCtrl["DeviceWebSocketHandler /api/sse/devices"]
        end
        subgraph Services["Business Services"]
            AuthSvc["AuthService"]
            RoomSvc["RoomService"]
            DevSvc["DeviceService"]
            RuleSvc["RuleService"]
            RuleSched["RuleScheduler (@Scheduled cron)"]
            SchedSvc["ScheduleService (@Scheduled cron)"]
            SceneSvc["SceneService"]
            EnergySvc["EnergyService"]
            LogSvc["ActivityLogService"]
            MemberSvc["MemberService"]
            CsvSvc["CsvExportService"]
        end
        subgraph Repos["JPA Repositories"]
            UserRepo["UserRepository"]
            RoomRepo["RoomRepository"]
            DevRepo["DeviceRepository"]
            RuleRepo["RuleRepository"]
            SchedRepo["ScheduleRepository"]
            SceneRepo["SceneRepository"]
            LogRepo["ActivityLogRepository"]
            MemberRepo["HomeMemberRepository"]
        end
    end

    DB[(PostgreSQL 16)]

    Browser -- "REST/HTTPS + JWT" --> SEC
    Browser -- "SSE stream" --> SseCtrl
    SEC --> Controllers

    AuthCtrl --> AuthSvc --> UserRepo
    RoomCtrl --> RoomSvc --> RoomRepo & UserRepo
    DevCtrl --> DevSvc --> RoomRepo & DevRepo
    RuleCtrl --> RuleSvc --> RuleRepo & DevRepo
    RuleSched --> RuleSvc
    SchedCtrl --> SchedSvc --> SchedRepo & DevRepo
    SceneCtrl --> SceneSvc --> SceneRepo & DevRepo
    EnergyCtrl --> EnergySvc --> DevRepo
    LogCtrl --> LogSvc --> LogRepo
    MemberCtrl --> MemberSvc --> MemberRepo & UserRepo

    DevSvc --> LogSvc & SseCtrl
    RuleSvc --> DevSvc
    SchedSvc --> DevSvc
    SceneSvc --> DevSvc
    EnergyCtrl & LogCtrl --> CsvSvc

    Repos --> DB
```

---

## Key Design Decisions

- **Authentication**: Spring Security with BCrypt password hashing and self-issued JWTs (NFR-02). All endpoints (except `/api/auth/**`) require a valid Bearer token.
- **Real-time updates**: Server-Sent Events (SSE) push device state changes and rule-fire notifications to the frontend — no external broker needed.
- **Role enforcement**: `MemberService` resolves ownership and membership on every request. The `OwnerGuard` (frontend) and `@PreAuthorize` / runtime checks (backend) enforce Owner-only access to rules, schedules, and the activity log.
- **Ownership model**: Each `Room`, `Rule`, and `Scene` is linked directly to its owning `User` via a `user_id` FK. Shared-home membership is handled separately via the `HomeMember` join table (owner → member user pair).
- **Rule evaluation**: `RuleService.evaluateRulesForDevice()` is called synchronously by `DeviceService` after each `PATCH /state` request. It checks all enabled IF-THEN rules for the updated device and calls `DeviceService.updateStateAsActor()` to apply actions — skipping recursive rule evaluation to prevent infinite trigger chains. Time-based rules are fired by `RuleScheduler` every minute.
- **Schedule execution**: `ScheduleService.runDueSchedules()` runs every minute via Spring `@Scheduled(cron = "0 * * * * *")` and fires all schedules due within the current minute.
- **Database**: PostgreSQL 16 in Docker. Schema managed by Flyway migrations (V1–V13) — version-controlled, auto-applied on startup.
- **CSV export**: `CsvExportService` is shared by both `ActivityLogController` and `EnergyController` for FR-16.

---

## Key Data Flows

### Rule Execution Flow (FR-10/11/12)

```mermaid
sequenceDiagram
    participant FE as Angular
    participant DevCtrl as DeviceController
    participant DevSvc as DeviceService
    participant RuleSvc as RuleService
    participant SseCtrl as DeviceWebSocketHandler
    participant DB as PostgreSQL

    FE->>DevCtrl: PATCH /api/rooms/{roomId}/devices/{id}/state
    DevCtrl->>DevSvc: updateState(email, roomId, deviceId, req)
    DevSvc->>DB: save(device)
    DevSvc->>SseCtrl: broadcast(deviceUpdate)
    DevSvc->>RuleSvc: evaluateRulesForDevice(device, req, stateOnChanged)
    RuleSvc->>DB: findByEnabledTrueAndTriggerDevice(device)
    loop for each matching rule
        RuleSvc->>DevSvc: updateStateAsActor(actionDeviceId, action, owner, actorName)
        DevSvc->>DB: save(actionDevice)
        DevSvc->>SseCtrl: broadcast(actionDeviceUpdate)
    end
    SseCtrl-->>FE: SSE event (device update)
```

### Schedule Execution Flow (FR-09)

```mermaid
sequenceDiagram
    participant CRON as @Scheduled (every minute)
    participant SchedSvc as ScheduleService
    participant DevSvc as DeviceService
    participant LogSvc as ActivityLogService
    participant DB as PostgreSQL

    CRON->>SchedSvc: runDueSchedules()
    SchedSvc->>DB: findDueSchedules(currentMinute)
    loop for each due schedule
        SchedSvc->>DevSvc: updateStateAsActor(device, action)
        DevSvc->>DB: save(device)
        SchedSvc->>LogSvc: log(schedule executed)
    end
```

---

## Component Catalogue

| Component | Endpoint / Path | Responsibility | FR |
|-----------|----------------|---------------|----|
| AuthController | `POST /api/auth/register`, `/login` | Register, login, JWT issue | FR-01/02 |
| RoomController | `GET/POST/PUT/DELETE /api/rooms` | CRUD for rooms | FR-03 |
| DeviceController | `/api/rooms/{id}/devices` | CRUD + state PATCH | FR-04/05/06 |
| DeviceWebSocketHandler | `GET /api/sse/devices` | SSE stream for real-time state | FR-07 |
| RuleController | `GET/POST/PUT/PATCH/DELETE /api/rules` | IF-THEN rule CRUD + conflict check | FR-10/11/15 |
| ScheduleController | `GET/POST/PUT/PATCH/DELETE /api/schedules` | Time-based schedule CRUD | FR-09 |
| SceneController | `GET/POST/PUT/DELETE /api/scenes`, `POST /{id}/activate` | Scene CRUD + activation | FR-17 |
| EnergyController | `GET /api/energy/devices`, `GET /api/energy/export` | Energy dashboard + CSV | FR-14/16 |
| ActivityLogController | `GET /api/activity-log`, `GET /api/activity-log/export` | Paginated log + CSV + delete | FR-08/16 |
| MemberController | `GET/POST /api/members`, `DELETE /api/members/{id}` | Invite / revoke members (Owner only) | FR-13/20 |
| RoomService | — | Room CRUD, owner-only write enforcement | FR-03/13 |
| RuleService | — | IF-THEN rule evaluation engine | FR-10/11/12 |
| RuleScheduler | — | Cron trigger for time-based rules (every minute) | FR-10/12 |
| ScheduleService | — | Cron execution every minute | FR-09 |
| MemberService | — | Ownership resolution, Owner authorization | FR-13/20 |
| CsvExportService | — | CSV serialization shared by log + energy | FR-16 |
| EnergyService | — | Power estimate per device (wattage × uptime) | FR-14 |

---

## Domain Model (simplified)

```mermaid
classDiagram
    class User {
        Long id
        String name
        String email
        String passwordHash
    }
    class HomeMember {
        Long id
        User owner
        User member
        String role
    }
    class Room {
        Long id
        User user
        String name
        String icon
    }
    class Device {
        Long id
        Room room
        String name
        DeviceType type
        boolean stateOn
        int brightness
        double temperature
        double sensorValue
        int coverPosition
    }
    class Rule {
        Long id
        User user
        String name
        TriggerType triggerType
        Device triggerDevice
        TriggerOperator triggerOperator
        Double triggerThresholdValue
        Device actionDevice
        String actionValue
        boolean enabled
    }
    class Schedule {
        Long id
        Device device
        String name
        String daysOfWeek
        int hour
        int minute
        String actionPayload
        boolean enabled
    }
    class Scene {
        Long id
        User user
        String name
        String icon
    }
    class SceneEntry {
        Long id
        Scene scene
        Device device
        String actionValue
    }
    class ActivityLog {
        Long id
        Device device
        User user
        Instant timestamp
        String actorName
        String action
    }

    User "1" --> "*" HomeMember : owns home
    User "1" --> "*" HomeMember : member of
    User "1" --> "*" Room : owns
    Room "1" --> "*" Device : contains
    User "1" --> "*" Rule : owns
    Device "1" --> "*" Rule : triggers
    Device "1" --> "*" Rule : acted on by
    Device "1" --> "*" Schedule : scheduled by
    User "1" --> "*" Scene : owns
    Scene "1" --> "*" SceneEntry : contains
    Device "1" --> "*" ActivityLog : logs
```

---

## Infrastructure

| Component | Technology | Port |
|-----------|-----------|------|
| Frontend | Angular 19 (Angular CLI, npm) | 4200 |
| Backend | Spring Boot 3.3.5 (Maven, Java 21) | 8080 |
| Database | PostgreSQL 16 (Docker) | 5432 |
| CI | GitHub Actions (Ubuntu, Java 21, Node 22) | — |

**Notes:**
- `passwordHash` stored via BCrypt — plain-text passwords never persisted (NFR-02)
- DB schema managed by Flyway V1–V13 migrations, auto-applied on startup
- CI pipeline: build → PMD check → tests → Jacoco coverage gate (≥ 75 %, NFR-03/04)
