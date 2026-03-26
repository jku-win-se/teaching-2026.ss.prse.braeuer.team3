# Developer Setup Guide

This guide describes how to set up the SmartHome Orchestrator project locally.

## Prerequisites

Make sure the following tools are installed on your machine:

| Tool | Version | Download |
|------|---------|----------|
| Java | 21 | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org |
| Docker Desktop | latest | https://www.docker.com/products/docker-desktop |
| Node.js | 18+ | https://nodejs.org |
| npm | 9+ | included with Node.js |
| Git | latest | https://git-scm.com |

---

## 1. Clone the Repository

```bash
git clone <repo-url>
cd <repo-folder-name>
```

---

## 2. Create Your Local Environment File

The `.env` file contains your local secrets (DB password, JWT secret). It is **never committed to Git**.

```bash
cp .env.example .env
```

Then open `.env` and fill in your values:

```env
DB_NAME=smarthome
DB_USER=smarthome_user
DB_PASSWORD=choose_a_password
JWT_SECRET=choose_a_random_string_min_32_characters
```

---

## 3. Start the Database (Docker)

```bash
docker compose up -d
```

This starts a PostgreSQL 16 container on port `5432`.

Verify it is running:
```bash
docker compose ps
```

To stop it:
```bash
docker compose down
```

> **Note:** Your data is persisted in a Docker volume (`postgres_data`). It survives container restarts.
> To wipe all data and start fresh: `docker compose down -v`

---

## 4. Create Your Local Spring Boot Config

Spring Boot does not read `.env` automatically. Create a local config file with your actual values — it is **never committed to Git**:

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
```

> If no `.example` file exists, create `backend/src/main/resources/application-local.yml` manually:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smarthome
    username: smarthome_user
    password: your_password_here
  flyway:
    enabled: true

app:
  jwt:
    secret: your_jwt_secret_here_min_32_characters_long
```

---

## 5. Start the Backend (Spring Boot)

The `local` profile is loaded automatically on startup — no extra configuration needed.

**Via terminal:**
```bash
cd backend
mvn spring-boot:run
```

**Via IDE (IntelliJ):**
- Right-click `SmarthomeApplication.java` → **Run**
- The `local` profile is set in the `main()` method and activates `application-local.yml` automatically

On first start, Flyway automatically applies all database migrations. The backend runs on **http://localhost:8080**.

To just build without running:
```bash
mvn clean install
```

To run tests:
```bash
mvn test
```

---

## 6. Start the Frontend (UI Prototype)

> **Note:** The `frontend/` folder is currently empty. Use the Angular prototype under `prototype/smarthome-orchestrator/` instead.

```bash
cd prototype/smarthome-orchestrator
npm install
npm start
```

The Angular app runs on **http://localhost:4200** and proxies API calls to the backend on port 8080.

---

## 7. Full Local Stack at a Glance

```
http://localhost:4200   →   Angular Frontend
http://localhost:8080   →   Spring Boot Backend
localhost:5432          →   PostgreSQL (Docker)
```

---

## Project Structure

```
smarthome-orchestrator/
├── backend/                  # Spring Boot (Java 21, Maven)
│   ├── src/main/java/        # Application source code
│   ├── src/main/resources/
│   │   ├── application.yml   # App configuration (reads from .env)
│   │   └── db/migration/     # Flyway SQL migrations (versioned schema)
│   ├── src/test/             # Unit and integration tests
│   └── pom.xml
├── frontend/                 # Angular SPA (currently empty — not yet implemented)
├── prototype/
│   └── smarthome-orchestrator/  # Angular UI prototype (used in place of frontend for now)
│       ├── src/
│       ├── package.json
│       └── package-lock.json
├── docker-compose.yml        # PostgreSQL for local development
├── docker-compose.test.yml   # PostgreSQL for testing/CI
├── .env.example              # Environment variable template (commit this)
├── .env                      # Your local secrets (never commit)
├── ruleset.xml               # PMD rules for code quality
└── backend/src/main/resources/
    ├── application.yml           # Shared config (commit this)
    └── application-local.yml     # Your local values (never commit)
```

---

## Database Migrations (Flyway)

Schema changes are managed via versioned SQL files in `backend/src/main/resources/db/migration/`.

- Files follow the naming convention: `V{number}__{description}.sql`
- Flyway runs them **automatically** on application startup
- Never edit an existing migration file — always create a new version

Example:
```
V1__create_users.sql
V2__create_rooms_and_devices.sql
```

---

## Code Quality

### PMD (NFR-04)
The build automatically checks for PMD violations. To run manually:
```bash
cd backend
mvn pmd:check
```

### Test Coverage (NFR-03 — minimum 75%)
```bash
cd backend
mvn verify
# Report: backend/target/site/jacoco/index.html
```

### Javadoc (NFR-06)
```bash
cd backend
mvn javadoc:javadoc
# Output: backend/target/site/apidocs/index.html
```

---

## Troubleshooting

**Port 5432 already in use**
```bash
# Check what is using the port
lsof -i :5432
# Stop local PostgreSQL if running
brew services stop postgresql  # macOS
```

**`DB_NAME` / `DB_USER` not found on startup / `Could not resolve placeholder`**
Make sure `backend/src/main/resources/application-local.yml` exists and contains your actual DB credentials and JWT secret.

**Flyway migration failed**
Never modify an existing migration file. If you need to change the schema, create a new migration file with the next version number.
