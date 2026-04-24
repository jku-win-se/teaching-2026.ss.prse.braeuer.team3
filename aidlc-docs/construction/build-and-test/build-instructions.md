# Build Instructions — SmartHome Orchestrator

## Prerequisites
- **Build Tool**: Maven 3.9+
- **Java**: 21
- **Node**: 18+ (frontend)
- **Database**: PostgreSQL 16 running locally
- **Environment**: `.env` file at project root with DB credentials

## Build Steps

### 1. Install Dependencies
```bash
cd backend
mvn dependency:resolve
```

### 2. Build Backend
```bash
mvn clean install -DskipTests
```

### 3. PMD Check (NFR-04)
```bash
mvn pmd:check
```
- Config: `ruleset.xml` in project root
- Fails on critical/high violations

### 4. Javadoc Validation (NFR-06)
```bash
mvn javadoc:javadoc
```
- Report: `target/site/apidocs/`

### 5. Verify Build
- **Expected**: `BUILD SUCCESS`
- **Artifacts**: `target/smarthome-0.0.1-SNAPSHOT.jar`
