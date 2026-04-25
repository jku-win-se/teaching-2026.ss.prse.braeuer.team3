# Business Logic Model — schedule-backend (FR-09)

## Overview

`ScheduleService` is the central orchestrator. It owns all schedule CRUD, the minute-based polling scheduler, and execution logic. `DeviceService` provides one internal method `updateStateAsActor()` used by the executor.

> **Scheduler implementation**: Spring `@Scheduled(cron = "0 * * * * *")` polling — fires at the start of every minute, queries enabled schedules matching the current hour/minute/day, and executes them. No external scheduler state; the `enabled` DB flag controls whether a schedule participates.

---

## 1. Create Schedule

```
Input:  userEmail, ScheduleRequest
Output: ScheduleResponse

1. resolveOwnedDevice(userEmail, request.deviceId)
   → load Device by ID
   → verify device.room.user.id == user.id  [BR-01]
   → throw 404 if not found / not owned

2. Validate request:
   → name not blank, ≤100 chars              [BR-05]
   → daysOfWeek non-empty                    [BR-03]
   → hour in 0–23, minute in 0–59            [BR-04]
   → actionPayload parseable as DeviceStateRequest [BR-06]

3. Build Schedule entity:
   → name, device, daysOfWeek (join with ","), hour, minute, actionPayload, enabled

4. scheduleRepository.save(schedule)
   → returns persisted Schedule with generated ID

5. return toResponse(schedule)
```

---

## 2. Get Schedules

```
Input:  userEmail, deviceId? (optional filter)
Output: List<ScheduleResponse>

1. Resolve user from userEmail (throw 401 if not found)

2. IF deviceId provided:
   → resolveOwnedDevice(userEmail, deviceId)   [BR-01]
   → scheduleRepository.findByDevice(device)
   ELSE:
   → deviceRepository.findAllByRoomUserId(userId)
   → scheduleRepository.findByDeviceIn(devices)

3. Map each Schedule → ScheduleResponse
```

---

## 3. Update Schedule

```
Input:  userEmail, scheduleId, ScheduleRequest
Output: ScheduleResponse

1. resolveOwnedSchedule(userEmail, scheduleId)  [BR-02]

2. Validate ScheduleRequest fields              [BR-03..BR-06]

3. Update entity fields: name, daysOfWeek, hour, minute, actionPayload, enabled

4. scheduleRepository.save(schedule)

5. return toResponse(schedule)
```

---

## 4. Toggle Enabled

```
Input:  userEmail, scheduleId, boolean enabled
Output: ScheduleResponse

1. resolveOwnedSchedule(userEmail, scheduleId)  [BR-02]

2. schedule.setEnabled(enabled)
3. scheduleRepository.save(schedule)
   → polling method respects enabled flag automatically [BR-07]

4. return toResponse(schedule)
```

---

## 5. Delete Schedule

```
Input:  userEmail, scheduleId
Output: void (204)

1. resolveOwnedSchedule(userEmail, scheduleId)  [BR-02]

2. scheduleRepository.delete(schedule)
   → DB cascade handles child rows if any       [BR-08]
```

---

## 6. Polling Scheduler — runDueSchedules

```
Triggered: @Scheduled(cron = "0 * * * * *") — at second 0 of every minute

1. now = LocalDateTime.now()
2. today = now.getDayOfWeek().name()   (e.g. "MONDAY")
3. candidates = scheduleRepository.findByEnabledTrueAndHourAndMinute(now.hour, now.minute)
4. For each candidate:
   → IF candidate.daysOfWeek contains today:
      → executeSchedule(candidate.id)
```

---

## 7. Execute Schedule

```
Input:  scheduleId
Output: void

1. scheduleRepository.findById(scheduleId)
   → IF not found: log warning, return (deleted between query and execution) [BR-12]

2. device = schedule.device
3. owner  = device.room.user
4. actorName = "Scheduler (" + schedule.name + ")"

5. ObjectMapper.readValue(schedule.actionPayload, DeviceStateRequest.class)
   → IF fails: activityLogService.log("Execution failed: invalid action payload"), return

6. deviceService.updateStateAsActor(device.id, request, owner, actorName)
   → applies state fields, broadcasts WebSocket, logs ActivityLog

7. ON any Exception:
   → activityLogService.log(device, owner, actorName, "Execution failed: " + e.getMessage())
```

---

## `DeviceService` Internal Method — `updateStateAsActor`

```
Input:  deviceId, DeviceStateRequest request, User owner, String actorName
Output: DeviceResponse

(Same logic as updateState() but skips ownership lookup — caller is trusted internal code)

1. deviceRepository.findById(deviceId)
   → throw 404 if not found (defensive)

2. Apply non-null fields from request to device

3. deviceRepository.save(device)

4. webSocketHandler.broadcast(owner.getEmail(), toResponse(device))

5. String action = activityLogService.buildActionDescription(device, request)
   activityLogService.log(device, owner, actorName, action)

6. return toResponse(device)
```

---

## DTO Conversion — `toResponse(Schedule)`

```
ScheduleResponse {
  id            = schedule.id
  name          = schedule.name
  deviceId      = schedule.device.id
  deviceName    = schedule.device.name
  roomName      = schedule.device.room.name
  daysOfWeek    = Arrays.asList(schedule.daysOfWeek.split(","))
  hour          = schedule.hour
  minute        = schedule.minute
  actionPayload = schedule.actionPayload
  enabled       = schedule.enabled
}
```
