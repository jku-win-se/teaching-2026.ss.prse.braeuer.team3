# Component Inventory

## Application Packages
- `backend/` — Spring Boot 3.3.5 REST-API-Server (Java 21)
- `frontend/` — Angular 19 Single-Page-Application

## Infrastructure Packages
- `docker-compose.yml` — PostgreSQL 16 Service
- `docker-compose.test.yml` — PostgreSQL 16 für CI-Tests
- `.github/workflows/Continuous Integration.yaml` — GitHub Actions CI-Pipeline

## Shared Packages
- `backend/src/main/resources/db/migration/` — Flyway-Migrationen (V1–V4)

## Test Packages
- `backend/src/test/` — JUnit 5 + MockMvc (DeviceControllerTest, DeviceServiceTest)
- `frontend/src/` — Karma/Jasmine Unit-Tests

## Total Count
- **Total Packages**: 2 (backend, frontend)
- **Application**: 2
- **Infrastructure**: 3 (docker-compose.yml, docker-compose.test.yml, CI-Pipeline)
- **Shared**: 1 (DB-Migrationen)
- **Test**: 2
