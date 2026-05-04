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
            DevSvc["DeviceService"]
            RuleSvc["RuleService (@Scheduled eval)"]
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
    RoomCtrl --> DevSvc --> RoomRepo & DevRepo
    DevCtrl --> DevSvc
    RuleCtrl --> RuleSvc --> RuleRepo & DevRepo
    SchedCtrl --> SchedSvc --> SchedRepo & DevRepo
    SceneCtrl --> SceneSvc --> SceneRepo & DevRepo
    EnergyCtrl --> EnergySvc --> DevRepo
    LogCtrl --> LogSvc --> LogRepo
    MemberCtrl --> MemberSvc --> MemberRepo & UserRepo

    DevSvc --> LogSvc
    RuleSvc --> DevSvc & LogSvc & SseCtrl
    SchedSvc --> DevSvc & LogSvc
    SceneSvc --> DevSvc & LogSvc
    EnergyCtrl & LogCtrl --> CsvSvc

    Repos --> DB
```

---

## Key Design Decisions

- **Authentication**: Spring Security with BCrypt password hashing and self-issued JWTs (NFR-02). All endpoints (except `/api/auth/**`) require a valid Bearer token.
- **Real-time updates**: Server-Sent Events (SSE) push device state changes and rule-fire notifications to the frontend — no external broker needed.
- **Role enforcement**: `MemberService` resolves ownership and membership on every request. The `OwnerGuard` (frontend) and `@PreAuthorize` / runtime checks (backend) enforce Owner-only access to rules, schedules, and the activity log.
- **Rule evaluation**: `RuleService` is triggered synchronously after each `PATCH /state` call. It evaluates all enabled rules whose trigger device and conditions match the new state, then calls `DeviceService.updateStateAsActor` to avoid infinite trigger chains.
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
    DevSvc->>LogSvc: log(manual change)
    DevSvc->>RuleSvc: evaluateRules(changedDevice)
    RuleSvc->>DB: findMatchingEnabledRules()
    loop for each matching rule
        RuleSvc->>DevSvc: updateStateAsActor(actionDevice, action)
        DevSvc->>DB: save(actionDevice)
        RuleSvc->>SseCtrl: push(rule fired notification)
    end
    SseCtrl-->>FE: SSE event (device update + notification)
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
| RuleService | — | Rule evaluation engine | FR-10/11/12 |
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
        String email
        String passwordHash
    }
    class Household {
        Long id
        Long ownerId
    }
    class HomeMember {
        Long id
        Long householdId
        Long userId
        String role
    }
    class Room {
        Long id
        Long householdId
        String name
    }
    class Device {
        Long id
        Long roomId
        String name
        String type
        String stateJson
    }
    class Rule {
        Long id
        Long householdId
        String triggerType
        Long triggerDeviceId
        Long actionDeviceId
        Boolean enabled
    }
    class Schedule {
        Long id
        Long deviceId
        String cronDays
        Integer hour
        Integer minute
        String actionJson
        Boolean enabled
    }
    class Scene {
        Long id
        Long householdId
        String name
    }
    class SceneEntry {
        Long id
        Long sceneId
        Long deviceId
        String actionValue
    }
    class ActivityLog {
        Long id
        Long deviceId
        Instant timestamp
        String actor
        String description
    }

    User "1" --> "1" Household : owns
    Household "1" --> "*" HomeMember : has
    Household "1" --> "*" Room : contains
    Room "1" --> "*" Device : contains
    Household "1" --> "*" Rule : defines
    Device "1" --> "*" Schedule : scheduled by
    Household "1" --> "*" Scene : has
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
