# Component Methods — FR-09: Zeitpläne konfigurieren

> Detailed business rules are defined in Functional Design (CONSTRUCTION phase).
> This document covers method signatures and high-level purpose.

---

## `ScheduleRepository` (Interface)

```java
// Find all schedules belonging to a specific device
List<Schedule> findByDevice(Device device);

// Find all enabled schedules (used at startup to re-register Quartz jobs)
List<Schedule> findByEnabledTrue();

// Find a schedule by its primary key
Optional<Schedule> findById(Long id);

// Standard Spring Data: save (insert + update) and delete
Schedule save(Schedule schedule);
void deleteById(Long id);
```

---

## `ScheduleService`

```java
// Returns all schedules for all devices accessible to the authenticated user.
// If deviceId is provided, results are filtered to that device.
List<ScheduleResponse> getSchedules(String userEmail, Long deviceId);

// Creates a new schedule, persists it, and registers a Quartz cron trigger.
// Returns the created schedule as ScheduleResponse.
ScheduleResponse createSchedule(String userEmail, ScheduleRequest request);

// Updates an existing schedule. Reschedules the Quartz trigger to reflect new time/days.
// Throws 404 if schedule not found or device not owned by user.
ScheduleResponse updateSchedule(String userEmail, Long scheduleId, ScheduleRequest request);

// Toggles the enabled flag. Pauses or resumes the Quartz trigger accordingly.
// Returns the updated ScheduleResponse.
ScheduleResponse setEnabled(String userEmail, Long scheduleId, boolean enabled);

// Deletes the schedule and removes the Quartz trigger.
// Throws 404 if not found or not owned by user.
void deleteSchedule(String userEmail, Long scheduleId);

// Called by ScheduleJobExecutor. Applies the stored actionPayload to the device
// via DeviceService.updateState() and records the result in ActivityLogService.
// Handles and logs any execution failure.
void executeSchedule(Long scheduleId);

// Registers all enabled schedules as Quartz jobs. Called at application startup.
// Invoked from ApplicationRunner or @PostConstruct.
void registerAllSchedulesOnStartup();

// --- Private helpers (no Javadoc required) ---
// Builds a Quartz cron expression from hour, minute, daysOfWeek
private String buildCronExpression(int hour, int minute, String daysOfWeek);

// Builds a unique Quartz JobKey/TriggerKey for a schedule ID
private JobKey jobKey(Long scheduleId);

// Validates that the device belongs to the authenticated user
private Device resolveOwnedDevice(String userEmail, Long deviceId);

// Converts Schedule entity to ScheduleResponse DTO
private ScheduleResponse toResponse(Schedule schedule);
```

---

## `ScheduleJobExecutor`

```java
// Quartz Job entry point. Reads scheduleId from JobDataMap, delegates to ScheduleService.
@Override
public void execute(JobExecutionContext context) throws JobExecutionException;
```

---

## `ScheduleController`

```java
// GET /api/schedules[?deviceId=]
// Returns all schedules for the authenticated user, optionally filtered by device.
@GetMapping
ResponseEntity<List<ScheduleResponse>> getSchedules(
    @AuthenticationPrincipal UserDetails principal,
    @RequestParam(required = false) Long deviceId);

// POST /api/schedules
// Creates a new schedule. Returns 201 Created with the new ScheduleResponse.
@PostMapping
ResponseEntity<ScheduleResponse> createSchedule(
    @AuthenticationPrincipal UserDetails principal,
    @RequestBody ScheduleRequest request);

// PUT /api/schedules/{id}
// Updates an existing schedule. Returns 200 OK with updated ScheduleResponse.
@PutMapping("/{id}")
ResponseEntity<ScheduleResponse> updateSchedule(
    @AuthenticationPrincipal UserDetails principal,
    @PathVariable Long id,
    @RequestBody ScheduleRequest request);

// PATCH /api/schedules/{id}/enabled
// Toggles the enabled flag. Body: { "enabled": true/false }
// Returns 200 OK with updated ScheduleResponse.
@PatchMapping("/{id}/enabled")
ResponseEntity<ScheduleResponse> setEnabled(
    @AuthenticationPrincipal UserDetails principal,
    @PathVariable Long id,
    @RequestBody Map<String, Boolean> body);

// DELETE /api/schedules/{id}
// Deletes a schedule. Returns 204 No Content.
@DeleteMapping("/{id}")
ResponseEntity<Void> deleteSchedule(
    @AuthenticationPrincipal UserDetails principal,
    @PathVariable Long id);
```

---

## `ScheduleService` (Angular)

```typescript
// GET /api/schedules[?deviceId=]
getSchedules(deviceId?: number): Observable<ScheduleDto[]>

// POST /api/schedules
createSchedule(request: ScheduleRequest): Observable<ScheduleDto>

// PUT /api/schedules/:id
updateSchedule(id: number, request: ScheduleRequest): Observable<ScheduleDto>

// PATCH /api/schedules/:id/enabled
setEnabled(id: number, enabled: boolean): Observable<ScheduleDto>

// DELETE /api/schedules/:id
deleteSchedule(id: number): Observable<void>
```

---

## `SchedulesComponent` (Angular)

```typescript
// Loads all schedules for display in the table
loadSchedules(): void

// Opens ScheduleDialogComponent in create mode
openCreateDialog(prefilledDeviceId?: number): void

// Opens ScheduleDialogComponent in edit mode with the given schedule
openEditDialog(schedule: ScheduleDto): void

// Toggles enabled on the given schedule via ScheduleService.setEnabled()
toggleEnabled(schedule: ScheduleDto): void

// Deletes the given schedule after confirmation, then reloads
deleteSchedule(schedule: ScheduleDto): void
```

---

## `ScheduleDialogComponent` (Angular)

```typescript
// Initializes reactive form; pre-fills fields if editing an existing schedule
ngOnInit(): void

// Submits the form — calls createSchedule or updateSchedule depending on mode
submit(): void
```
