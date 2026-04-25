# Domain Entities — schedule-backend (FR-09)

## `Schedule` Entity

**Table**: `schedules`
**Package**: `at.jku.se.smarthome.domain`

### Fields

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `id` | `Long` | `id` | PK, auto-generated | |
| `name` | `String` | `name` | NOT NULL, VARCHAR(100) | User-defined display name |
| `device` | `Device` | `device_id` (FK) | NOT NULL, ON DELETE CASCADE | LAZY ManyToOne |
| `daysOfWeek` | `String` | `days_of_week` | NOT NULL, VARCHAR(100) | Comma-separated: `"MONDAY,WEDNESDAY"` |
| `hour` | `int` | `hour` | NOT NULL, 0–23 | |
| `minute` | `int` | `minute` | NOT NULL, 0–59 | |
| `actionPayload` | `String` | `action_payload` | NOT NULL, TEXT | JSON-serialized `DeviceStateRequest` |
| `enabled` | `boolean` | `enabled` | NOT NULL, DEFAULT true | |

### Flyway Migration V6

```sql
CREATE TABLE schedules (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    device_id    BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    days_of_week VARCHAR(100) NOT NULL,
    hour         INT NOT NULL CHECK (hour BETWEEN 0 AND 23),
    minute       INT NOT NULL CHECK (minute BETWEEN 0 AND 59),
    action_payload TEXT NOT NULL,
    enabled      BOOLEAN NOT NULL DEFAULT TRUE
);
```

### Flyway Migration V7 — Quartz Schema

Standard Quartz 2.x PostgreSQL schema (`quartz-tables_postgres.sql`).
Key tables: `QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`, `QRTZ_CRON_TRIGGERS`, `QRTZ_FIRED_TRIGGERS`, `QRTZ_SCHEDULER_STATE`, `QRTZ_LOCKS`.

### JPA Annotations Summary

```java
@Entity
@Table(name = "schedules")
public class Schedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "days_of_week", nullable = false, length = 100)
    private String daysOfWeek;  // e.g. "MONDAY,WEDNESDAY,FRIDAY"

    @Column(nullable = false)
    private int hour;

    @Column(nullable = false)
    private int minute;

    @Column(name = "action_payload", nullable = false, columnDefinition = "TEXT")
    private String actionPayload;  // JSON-serialized DeviceStateRequest

    @Column(nullable = false)
    private boolean enabled = true;
}
```

---

## `ScheduleRequest` DTO

```java
public class ScheduleRequest {
    private String name;          // required, max 100 chars
    private Long deviceId;        // required
    private List<String> daysOfWeek;  // required, at least 1, e.g. ["MONDAY","FRIDAY"]
    private int hour;             // 0–23
    private int minute;           // 0–59
    private String actionPayload; // JSON-serialized DeviceStateRequest
    private boolean enabled;      // default true
}
```

---

## `ScheduleResponse` DTO

```java
public class ScheduleResponse {
    private Long id;
    private String name;
    private Long deviceId;
    private String deviceName;
    private String roomName;
    private List<String> daysOfWeek;
    private int hour;
    private int minute;
    private String actionPayload;
    private boolean enabled;
}
```

---

## Quartz Domain Objects (internal, not JPA)

| Object | Type | Purpose |
|---|---|---|
| `JobDetail` | `org.quartz.JobDetail` | Represents `ScheduleJobExecutor` class + `scheduleId` in `JobDataMap` |
| `CronTrigger` | `org.quartz.CronTrigger` | Fires at cron expression derived from `hour`, `minute`, `daysOfWeek` |
| `JobKey` | `org.quartz.JobKey` | Identity: name=`"schedule-{id}"`, group=`"schedules"` |
| `TriggerKey` | `org.quartz.TriggerKey` | Identity: name=`"trigger-{id}"`, group=`"schedules"` |
