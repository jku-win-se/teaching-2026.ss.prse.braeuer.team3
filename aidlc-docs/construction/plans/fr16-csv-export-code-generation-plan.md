# Code Generation Plan — FR-16: CSV Export

## Unit Context
- **Feature**: FR-16 — Activity Log & Energy Usage CSV Export
- **Workspace Root**: `/Users/oliversugic/Studium/PR_SE/teaching-2026.ss.prse.braeuer.team3`
- **Backend package**: `at.jku.se.smarthome`
- **Frontend**: Angular 19, standalone components

---

## Unit 1 — csv-backend

### Step 1 — ActivityLogRepository: add `findAllByUser`
- **File**: `backend/src/main/java/at/jku/se/smarthome/repository/ActivityLogRepository.java`
- **Action**: MODIFY — add non-paginated query method
  ```java
  List<ActivityLog> findAllByUser(User user, Sort sort);
  ```

### Step 2 — CsvExportService: new service
- **File**: `backend/src/main/java/at/jku/se/smarthome/service/CsvExportService.java`
- **Action**: CREATE
- **Responsibilities**:
  - `buildActivityLogCsv(List<ActivityLog>)` → CSV string, header + rows, RFC-4180 escaping
  - `buildEnergyCsv(List<EnergyDeviceResponse>)` → CSV string, header + rows
  - Static helper `escapeCsv(String)` for field-level escaping

### Step 3 — ActivityLogService: add `exportActivityLogCsv`
- **File**: `backend/src/main/java/at/jku/se/smarthome/service/ActivityLogService.java`
- **Action**: MODIFY — inject `CsvExportService`, add method
  - `exportActivityLogCsv(String email)` — owner-only, fetches all entries sorted ASC, delegates to `CsvExportService`

### Step 4 — EnergyService: add `exportEnergyCsv`
- **File**: `backend/src/main/java/at/jku/se/smarthome/service/EnergyService.java`
- **Action**: MODIFY — inject `CsvExportService`, add method
  - `exportEnergyCsv(String email)` — fetches device energy list, delegates to `CsvExportService`

### Step 5 — ActivityLogController: add export endpoint
- **File**: `backend/src/main/java/at/jku/se/smarthome/controller/ActivityLogController.java`
- **Action**: MODIFY — add GET `/api/activity-log/export`
  - Returns `ResponseEntity<byte[]>` with `Content-Type: text/csv` and `Content-Disposition: attachment; filename="activity-log.csv"`

### Step 6 — EnergyController: add export endpoint
- **File**: `backend/src/main/java/at/jku/se/smarthome/controller/EnergyController.java`
- **Action**: MODIFY — add GET `/api/energy/export`
  - Returns `ResponseEntity<byte[]>` with `Content-Type: text/csv` and `Content-Disposition: attachment; filename="energy-summary.csv"`

### Step 7 — CsvExportServiceTest: unit tests
- **File**: `backend/src/test/java/at/jku/se/smarthome/service/CsvExportServiceTest.java`
- **Action**: CREATE
- Tests: header row, data row content, CSV escaping (comma in field, double-quote in field), empty list

### Step 8 — ActivityLogControllerTest: extend with export test
- **File**: `backend/src/test/java/at/jku/se/smarthome/controller/ActivityLogControllerTest.java`
- **Action**: MODIFY — add tests for `GET /api/activity-log/export`
  - 200 with CSV content-type when authenticated
  - 401 when not authenticated

### Step 9 — EnergyControllerTest: extend with export test
- **File**: `backend/src/test/java/at/jku/se/smarthome/controller/EnergyControllerTest.java`
- **Action**: MODIFY — add tests for `GET /api/energy/export`
  - 200 with CSV content-type when authenticated
  - 401 when not authenticated

---

## Unit 2 — csv-frontend

### Step 10 — activity-log.service.ts: add `exportCsv`
- **File**: `frontend/src/app/core/activity-log.service.ts`
- **Action**: MODIFY — add method that calls `GET /api/activity-log/export` with `responseType: 'blob'` and triggers browser download

### Step 11 — energy.service.ts: add `exportCsv`
- **File**: `frontend/src/app/core/energy.service.ts`
- **Action**: MODIFY — add method that calls `GET /api/energy/export` with `responseType: 'blob'` and triggers browser download

### Step 12 — log.component.ts: add Export CSV button
- **File**: `frontend/src/app/features/log/log.component.ts`
- **Action**: MODIFY — add export button in page header, call `activityLogService.exportCsv()`, show snackbar on error

### Step 13 — energy.component.ts: wire existing stub
- **File**: `frontend/src/app/features/energy/energy.component.ts`
- **Action**: MODIFY — replace no-op `exportCsv()` with real call to `energyService.exportCsv()`

---

## Story Traceability
- [ ] FR-16.1 — Activity Log Export → Steps 1, 2, 3, 5, 7, 8, 10, 12
- [ ] FR-16.2 — Energy Export → Steps 2, 4, 6, 7, 9, 11, 13
- [ ] FR-16.3 — Frontend Activity Log button → Steps 10, 12
- [ ] FR-16.4 — Frontend Energy button wired → Steps 11, 13
