# System Architecture — SmartHome Orchestrator

> Developer-facing technical documentation.  
> For setup instructions, see [SETUP.md](../SETUP.md).  
> For the full UML diagram, see [ARCHITECTURE_UML.md](../ARCHITECTURE_UML.md).

---

## 1. Overview

SmartHome Orchestrator is a full-stack home automation platform developed as part of the Software Engineering Praktikum (SS 2026) at JKU Linz. It allows users to manage virtual smart devices, define automation rules and schedules, monitor energy usage, and coordinate access with other household members — all via a unified web interface.

The system is built as a classic two-tier web application:

| Tier | Technology | Purpose |
|------|-----------|---------|
| Frontend | Angular 19 SPA | User interface, reactive state management |
| Backend | Spring Boot 3.3.5 (Java 21) | Business logic, REST API, scheduled jobs |
| Database | PostgreSQL 16 (Docker) | Persistent storage, managed via Flyway |

---

## 2. Architecture

### 2.1 Layering

The backend follows a standard three-layer architecture:

```
Controller Layer   — HTTP request handling, input validation, DTO mapping
Service Layer      — Business logic, authorization checks, cross-service orchestration
Repository Layer   — Data access via Spring Data JPA (no raw SQL outside migrations)
```

All database schema changes are managed through versioned Flyway migrations (`V1__` through `V13__`), applied automatically on startup.

### 2.2 Authentication & Authorization

- **JWT-based stateless auth**: `AuthController` issues signed JWTs on login; all other endpoints require a valid `Authorization: Bearer <token>` header.
- **Spring Security filter**: `JwtAuthFilter` validates the token and populates the `SecurityContext` on every request.
- **Role model** (FR-13): Every user is either the **Owner** of a household (full access) or a **Member** (device control only — no rule/schedule/log management). Role is resolved at runtime by `MemberService`.
- **Frontend guards**: `AuthGuard` blocks unauthenticated navigation; `OwnerGuard` restricts the Rules, Schedules, and Log pages to the household Owner.
- **Password hashing**: BCrypt via Spring Security — plain-text passwords are never stored or logged (NFR-02).

### 2.3 Real-Time Device State (FR-07)

Device state changes are pushed to all connected frontend sessions via **Server-Sent Events (SSE)** through `DeviceWebSocketHandler` at `GET /api/sse/devices`. The Angular `RealtimeService` subscribes to this stream and updates the UI reactively — no polling required.

The same SSE channel carries **rule-fire notifications** (FR-12) so the frontend can display them as toasts without separate polling.

### 2.4 Automation Engine

#### Rules (FR-10/11)
`RuleService` is invoked synchronously after every `PATCH /state` call. It queries all enabled rules whose trigger device and condition match the new device state. For each matching rule it calls `DeviceService.updateStateAsActor` (a dedicated method that bypasses rule re-evaluation to prevent infinite chains) and pushes a notification via SSE.

Supported trigger types:
- `TIME` — rule fires at a specified hour/minute on configured days of the week
- `THRESHOLD` — rule fires when a sensor value crosses a threshold (GT / LT operator)
- `EVENT` — rule fires when a device state changes to a specified value

#### Schedules (FR-09)
`ScheduleService.runDueSchedules()` runs every minute via `@Scheduled(cron = "0 * * * * *")`. It queries all enabled schedules due in the current minute and calls `DeviceService.updateStateAsActor` for each one, logging the execution.

### 2.5 Conflict Detection (FR-15)
`RuleController` exposes `GET /api/rules/conflicts?actionDeviceId={id}&actionValue={v}` which `RuleService` uses to detect rules that would put the same device into contradictory states simultaneously. The frontend calls this endpoint when saving a rule and warns the user if conflicts exist.

### 2.6 Energy Dashboard (FR-14)
`EnergyService` estimates power consumption based on device type and state (e.g., a switch that is `on` consumes its rated wattage). The frontend (`EnergyComponent`) aggregates device-level data into room totals and a household total, grouped by day and week.

### 2.7 CSV Export (FR-16)
`CsvExportService` is a shared utility consumed by both `ActivityLogController` (`GET /api/activity-log/export`) and `EnergyController` (`GET /api/energy/export`). It serializes JPA entities to RFC-4180 CSV with UTF-8 BOM for Excel compatibility.

---

## 3. REST API Reference

All endpoints except `/api/auth/**` require `Authorization: Bearer <jwt>`.

### Authentication

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/register` | Create a new account | None |
| POST | `/api/auth/login` | Login, returns JWT | None |

### Rooms

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/rooms` | List all rooms for the authenticated user |
| POST | `/api/rooms` | Create a room |
| PUT | `/api/rooms/{id}` | Rename a room |
| DELETE | `/api/rooms/{id}` | Delete a room (and all its devices) |

### Devices

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/rooms/{roomId}/devices` | List devices in a room |
| POST | `/api/rooms/{roomId}/devices` | Add a device |
| PUT | `/api/rooms/{roomId}/devices/{id}` | Rename a device |
| PATCH | `/api/rooms/{roomId}/devices/{id}/state` | Update device state |
| DELETE | `/api/rooms/{roomId}/devices/{id}` | Remove a device |

### Real-Time

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/sse/devices` | SSE stream — device updates and notifications |

### Rules

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/rules[?deviceId={id}]` | List rules (optional filter) | Owner |
| GET | `/api/rules/conflicts?actionDeviceId={id}&actionValue={v}` | Check for conflicting rules | Owner |
| POST | `/api/rules` | Create a rule | Owner |
| PUT | `/api/rules/{id}` | Update a rule | Owner |
| PATCH | `/api/rules/{id}/enabled` | Enable / disable a rule | Owner |
| DELETE | `/api/rules/{id}` | Delete a rule | Owner |

### Schedules

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/schedules[?deviceId={id}]` | List schedules | Owner |
| POST | `/api/schedules` | Create a schedule | Owner |
| PUT | `/api/schedules/{id}` | Update a schedule | Owner |
| PATCH | `/api/schedules/{id}/enabled` | Enable / disable | Owner |
| DELETE | `/api/schedules/{id}` | Delete a schedule | Owner |

### Scenes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/scenes` | List all scenes |
| POST | `/api/scenes` | Create a scene |
| PUT | `/api/scenes/{id}` | Update a scene |
| POST | `/api/scenes/{id}/activate` | Activate a scene |
| DELETE | `/api/scenes/{id}` | Delete a scene |

### Energy

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/energy/devices` | Device-level energy estimates | Any |
| GET | `/api/energy/export` | CSV export of energy summary | Owner |

### Activity Log

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/activity-log` | Paginated log (`page`, `size`, `from`, `to`, `deviceId`) | Owner |
| GET | `/api/activity-log/export` | CSV export | Owner |
| DELETE | `/api/activity-log/{id}` | Delete a log entry | Owner |

### Members

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/members` | List all household members | Owner |
| POST | `/api/members/invite` | Invite a member by email | Owner |
| DELETE | `/api/members/{id}` | Revoke member access | Owner |

---

## 4. Database Schema

Schema is managed by Flyway migrations V1–V13. Key tables:

| Table | Description | Migration |
|-------|-------------|-----------|
| `users` | User accounts (email, bcrypt hash) | V1 |
| `rooms` | Rooms scoped to a household | V2, V10 |
| `devices` | Virtual devices with type + JSON state | V3, V4 |
| `activity_log` | Immutable log of every state change | V5 |
| `schedules` | Time-based device actions (cron-like) | V6 |
| `rules` | IF-THEN automation rules | V7, V8 |
| `households` | Household entity linking owner to rooms/rules | V9, V10 |
| `home_members` | Owner-to-Member relationships | V11, V12 |
| `scenes` / `scene_entries` | Named device state presets | V13 |

---

## 5. Build & Quality Assurance

### Build

```bash
# Backend
cd backend && mvn clean install      # compile + test + PMD + Jacoco
mvn spring-boot:run                   # run locally (needs application-local.yml)

# Frontend
cd frontend && npm install && npm start   # dev server at localhost:4200
```

### PMD (NFR-04)

The build enforces zero critical/high PMD violations. Ruleset defined in `ruleset.xml`.

```bash
cd backend && mvn pmd:check
```

### Test Coverage (NFR-03 — minimum 75 %)

```bash
cd backend && mvn verify
# Report: backend/target/site/jacoco/index.html
```

### Javadoc (NFR-06)

All public classes, interfaces, and methods in `controller/`, `service/`, `repository/`, and `domain/` layers carry full Javadoc (`@param`, `@return`, `@throws`).

```bash
cd backend && mvn javadoc:javadoc
# Output: backend/target/site/apidocs/index.html
```

### CI Pipeline

GitHub Actions (`.github/workflows/Continuous Integration.yaml`) runs on every push:
1. Start PostgreSQL test container (`docker-compose.test.yml`)
2. `mvn verify` (compile + PMD + tests + Jacoco gate)
3. `npm install && npm run build` (Angular production build)
4. Jacoco badge update (`.github/badges/jacoco.svg`)

---

## 6. Extension Points

| Area | How to extend |
|------|--------------|
| New device type | Add value to `DeviceType` enum; extend `EnergyService` wattage map; update frontend `DeviceCardComponent` |
| New rule trigger type | Add value to `TriggerType` enum; implement evaluation branch in `RuleService.evaluateRules()` |
| New schedule recurrence pattern | Extend `ScheduleService.isDue()` logic and the schedule DTO |
| Physical IoT (FR-18) | Add an MQTT integration layer that calls `DeviceService.updateStateAsActor()` — the rest of the pipeline (rules, logging, SSE) works automatically |
| Vacation mode (FR-21) | Extend `Schedule` entity with a date-range override flag; filter in `ScheduleService.runDueSchedules()` |
