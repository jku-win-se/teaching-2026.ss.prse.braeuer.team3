# Code Generation Plan — FR-08: Activity Log

## Unit Context
- **Unit Name**: fr08-activity-log
- **Branch**: `15-fr-08-aktivitätsprotokoll`
- **Issue**: #15
- **Requirements**: `aidlc-docs/inception/requirements/fr08-requirements.md`
- **Type**: Brownfield — new files + modifications to existing files

## Acceptance Criteria
- [ ] **AC-1**: Every manual device state change creates an activity log entry (timestamp, device, actor name, action description)
- [ ] **AC-2**: Log viewable in UI, paginated (20/page default)
- [ ] **AC-3**: Log filterable by date range + device dropdown
- [ ] **AC-4**: New entries appear in real-time via WebSocket
- [ ] **AC-5**: User can delete individual log entries

---

## Files Overview

### New Files — Backend
| File | Purpose |
|------|---------|
| `backend/src/main/java/at/jku/se/smarthome/domain/ActivityLog.java` | JPA entity |
| `backend/src/main/java/at/jku/se/smarthome/repository/ActivityLogRepository.java` | Spring Data repository |
| `backend/src/main/java/at/jku/se/smarthome/service/ActivityLogService.java` | Business logic |
| `backend/src/main/java/at/jku/se/smarthome/controller/ActivityLogController.java` | REST API |
| `backend/src/main/java/at/jku/se/smarthome/dto/ActivityLogResponse.java` | Response DTO |
| `backend/src/main/resources/db/migration/V6__create_activity_log_table.sql` | DB migration |

### Modified Files — Backend
| File | Change |
|------|--------|
| `backend/src/main/java/at/jku/se/smarthome/service/DeviceService.java` | Call `ActivityLogService.log()` in `updateState()` |

### New Files — Frontend
| File | Purpose |
|------|---------|
| `frontend/src/app/activity-log/activity-log.component.ts` | Component logic |
| `frontend/src/app/activity-log/activity-log.component.html` | Template |
| `frontend/src/app/activity-log/activity-log.component.scss` | Styles |
| `frontend/src/app/activity-log/activity-log.service.ts` | HTTP service |

### Modified Files — Frontend
| File | Change |
|------|--------|
| `frontend/src/app/app.routes.ts` | Add route for `/activity-log` |
| `frontend/src/app/app.component.html` | Add nav link to activity log |

### New Files — Tests
| File | Purpose |
|------|---------|
| `backend/src/test/java/at/jku/se/smarthome/service/ActivityLogServiceTest.java` | Unit tests |
| `backend/src/test/java/at/jku/se/smarthome/controller/ActivityLogControllerTest.java` | Controller tests |

---

## Step-by-Step Plan

### Step 1 — DB Migration: `V6__create_activity_log_table.sql` [ ]
```sql
CREATE TABLE activity_logs (
    id          BIGSERIAL PRIMARY KEY,
    timestamp   TIMESTAMPTZ NOT NULL DEFAULT now(),
    device_id   BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_name  VARCHAR(255) NOT NULL,
    action      VARCHAR(500) NOT NULL
);
CREATE INDEX idx_activity_logs_user_id   ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_device_id ON activity_logs(device_id);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp DESC);
```

### Step 2 — Domain: `ActivityLog.java` [ ]
JPA entity mapping the `activity_logs` table.
Fields: `id`, `timestamp` (Instant), `device` (ManyToOne), `user` (ManyToOne), `actorName` (String), `action` (String).
Javadoc on class and all public methods. PMD-safe (no primitives for nullable fields, no System.out).

### Step 3 — DTO: `ActivityLogResponse.java` [ ]
Fields: `id`, `timestamp`, `deviceId`, `deviceName`, `roomName`, `actorName`, `action`.
Javadoc on class and all public methods.

### Step 4 — Repository: `ActivityLogRepository.java` [ ]
```java
Page<ActivityLog> findByUserAndTimestampBetweenAndDevice(
    User user, Instant from, Instant to, Device device, Pageable pageable);
Page<ActivityLog> findByUserAndTimestampBetween(
    User user, Instant from, Instant to, Pageable pageable);
Page<ActivityLog> findByUser(User user, Pageable pageable);
Optional<ActivityLog> findByIdAndUser(Long id, User user);
```
Javadoc on interface and all methods.

### Step 5 — Service: `ActivityLogService.java` [ ]
Methods:
- `log(Device device, User user, String action)` — creates and saves a log entry
- `getLogs(String email, int page, int size, Instant from, Instant to, Long deviceId)` — returns paginated `Page<ActivityLogResponse>` applying filters dynamically
- `deleteLog(String email, Long logId)` — deletes entry if owned by user, throws 404 if not found, 403 if not owner

Action description helper — generates human-readable action string from `DeviceStateRequest`:
- SWITCH: "Turned on" / "Turned off"
- DIMMER: "Brightness set to {n}%" / "Turned on" / "Turned off"
- THERMOSTAT: "Temperature set to {n}°C" / "Turned on" / "Turned off"
- SENSOR: "Sensor value set to {n}"
- COVER: "Cover opened" / "Cover closed" / "Cover position set to {n}%"

Javadoc on class and all public methods.

### Step 6 — Modify `DeviceService.java` [ ]
Inject `ActivityLogService` via constructor. In `updateState()`, after saving the device, call:
```java
String action = activityLogService.buildActionDescription(device, request);
activityLogService.log(device, resolvedUser, action);
```
Update Javadoc on `updateState()` to mention logging.

### Step 7 — Controller: `ActivityLogController.java` [ ]
```
GET  /api/activity-log
     ?page=0&size=20&from={ISO}&to={ISO}&deviceId={id}
     → 200 Page<ActivityLogResponse>

DELETE /api/activity-log/{id}
     → 204 No Content
```
Secured via JWT (same pattern as DeviceController).
Javadoc on class and all public methods.

### Step 8 — Backend Tests [ ]
**`ActivityLogServiceTest.java`**:
- `log_createsEntry`
- `getLogs_noFilter_returnsAll`
- `getLogs_withDateRange_filtersCorrectly`
- `getLogs_withDeviceId_filtersCorrectly`
- `deleteLog_removesEntry`
- `deleteLog_throwsNotFound_whenEntryNotOwned`

**`ActivityLogControllerTest.java`**:
- `getActivityLog_returns200`
- `deleteActivityLog_returns204`
- `deleteActivityLog_returns404_whenNotFound`

### Step 9 — Frontend Service: `activity-log.service.ts` [ ]
- `getLogs(page, size, from?, to?, deviceId?): Observable<Page<ActivityLogResponse>>`
- `deleteLog(id): Observable<void>`

### Step 10 — Frontend Component [ ]
`ActivityLogComponent`:
- Table: timestamp (24h format: `MMM d, H:mm`) | device | room | actor | action | delete button
- Filter bar: date range (from/to) + device dropdown (populated from DeviceService)
- Angular Material `MatPaginator` (pageSize=20)
- WebSocket subscription: prepend new log entries in real-time
- `data-testid` attributes on all interactive elements

### Step 11 — Frontend Routing & Navigation [ ]
- Add route `/activity-log` → `ActivityLogComponent` in `app.routes.ts`
- Add nav link in `app.component.html`

### Step 12 — PMD & Javadoc Review [ ]
- Mental PMD review of all generated Java code
- Verify Javadoc on all public classes/methods in domain/service/repository/controller layers

---

## Completion Criteria
- [ ] Steps 1–12 all marked [x]
- [ ] AC-1 through AC-5 satisfied
- [ ] Full test suite passes
- [ ] PMD: 0 critical/high violations
- [ ] Javadoc complete on all public API
