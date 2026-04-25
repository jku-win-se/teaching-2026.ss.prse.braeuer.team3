# Tech Stack Decisions — schedule-backend (FR-09)

## New Dependency: Quartz Scheduler

**Decision**: Use `spring-boot-starter-quartz` (Spring Boot managed version).

**Rationale**:
- Spring Boot 3.3.5 manages Quartz 2.3.x via the starter — no explicit version needed
- `spring-boot-starter-quartz` auto-configures `SchedulerFactoryBean` with Spring integration
- `SpringBeanJobFactory` (included in starter) enables Spring bean injection in `ScheduleJobExecutor`
- PostgreSQL `JobStoreTX` is configured via `application.properties` — no manual datasource wiring

**pom.xml addition**:
```xml
<!-- Quartz Scheduler -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

---

## Quartz Configuration Strategy

**Store**: `JdbcJobStore` (PostgreSQL-backed) — jobs and triggers survive restarts.

**Configuration location**: `application.properties` (Spring Boot Quartz auto-config properties).

```properties
# Quartz scheduler settings
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.threadPool.threadCount=5
spring.quartz.properties.org.quartz.jobStore.isClustered=false
```

**Schema initialization**: `never` — Flyway migration V7 creates the Quartz tables explicitly, giving version-controlled schema management consistent with the rest of the project.

**Flyway V7 source**: Use the official Quartz 2.3.x PostgreSQL DDL script (`quartz-tables_postgres.sql`) from the Quartz distribution. Rename to `V7__quartz_schema.sql`.

---

## JSON Serialization: Jackson `ObjectMapper`

**Decision**: Reuse the existing Spring-managed `ObjectMapper` bean (already auto-configured by `spring-boot-starter-web`).

**Usage**: Inject `ObjectMapper` into `ScheduleService` to serialize/deserialize `DeviceStateRequest` ↔ `actionPayload` (TEXT column).

**No new dependency required.**

---

## Logging: SLF4J (existing)

**Decision**: Use `org.slf4j.Logger` / `LoggerFactory` — already on classpath via `spring-boot-starter-web`.

**No new dependency required.**

---

## Existing Dependencies Reused (no changes)

| Dependency | Usage in FR-09 |
|---|---|
| `spring-boot-starter-data-jpa` | `ScheduleRepository`, `@Transactional` |
| `spring-boot-starter-security` | JWT protection on `/api/schedules` |
| `spring-boot-starter-validation` | Bean validation on `ScheduleRequest` |
| `flyway-core` + `flyway-database-postgresql` | V6 + V7 migrations |
| `postgresql` (runtime) | Quartz JDBC store |
| `spring-boot-starter-web` | REST controller, `ObjectMapper` |

---

## Summary of Changes to Existing Files

| File | Change |
|---|---|
| `pom.xml` | Add `spring-boot-starter-quartz` |
| `application.properties` | Add Quartz configuration properties |
| `DeviceService.java` | Add `updateStateAsActor()` internal method |
| `DeviceService.java` | Inject `ScheduleService`; call `removeAllJobsForDevice()` in `deleteDevice()` |
| `V6__create_schedules.sql` | New file — schedules table |
| `V7__quartz_schema.sql` | New file — Quartz system tables |
