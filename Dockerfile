# ── Stage 1: Build Angular frontend ─────────────────────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json frontend/
RUN cd frontend && npm ci --prefer-offline
COPY frontend/ frontend/
# Angular 19 outputs browser files to dist/.../browser/ — normalise to /ng-dist
RUN cd frontend && npx ng build --base-href=/ && \
    if [ -d dist/smarthome-orchestrator/browser ]; then \
      mv dist/smarthome-orchestrator/browser /ng-dist; \
    else \
      mv dist/smarthome-orchestrator /ng-dist; \
    fi

# ── Stage 2: Build Spring Boot backend ──────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app
# ruleset.xml lives at project root; pom.xml references it as ../ruleset.xml
COPY ruleset.xml /ruleset.xml
COPY backend/pom.xml pom.xml
# Download dependencies as a separate layer for faster rebuilds
RUN mvn dependency:go-offline -q || true
COPY backend/src src/
# Embed Angular output into Spring Boot static resources
COPY --from=frontend-build /ng-dist src/main/resources/static/
# Skip tests and PMD (CI handles those); just produce the fat JAR
RUN mvn package -DskipTests -Dpmd.skip=true -q

# ── Stage 3: Runtime (slim JRE image) ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/smarthome-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=dist", "-Xmx512m", "-jar", "app.jar"]
