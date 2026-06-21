#!/usr/bin/env bash
# build-dist.sh — Builds the SmartHome Orchestrator as a native macOS .dmg installer.
#
# Prerequisites (must be installed on the build machine):
#   - JDK 21  (java + jpackage in PATH)
#   - Maven 3.9+
#   - Node.js 18+ and npm
#   - Xcode Command Line Tools  (xcode-select --install)
#
# The smarthome-db Docker container is NOT bundled — users must start it manually
# with  docker compose up -d  before launching the installed app.
#
# Usage:
#   chmod +x build-dist.sh
#   ./build-dist.sh
#
# Output:
#   dist-app/SmartHome Orchestrator-1.0.0.dmg

set -euo pipefail

APP_NAME="SmartHome Orchestrator"
APP_VERSION="1.0.0"
JAR_NAME="smarthome-0.0.1-SNAPSHOT.jar"

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_DIR="$PROJECT_ROOT/backend"
STATIC_DIR="$BACKEND_DIR/src/main/resources/static"
OUTPUT_DIR="$PROJECT_ROOT/dist-app"

# ── Dependency checks ──────────────────────────────────────────────────────────
echo "==> Checking dependencies..."
for cmd in java mvn node npm; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' not found. Please install it and add it to PATH." >&2
    exit 1
  fi
done

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
  echo "ERROR: Java 21+ is required (found $JAVA_VERSION)." >&2
  exit 1
fi

if ! command -v jpackage &>/dev/null; then
  echo "ERROR: 'jpackage' not found. Make sure you have a JDK (not just a JRE) installed." >&2
  exit 1
fi

# ── Angular build ──────────────────────────────────────────────────────────────
echo ""
echo "==> Building Angular frontend..."
cd "$FRONTEND_DIR"
npm ci --prefer-offline

# Angular 19 outputs to <outputPath>/browser/ — build to a temp dir and copy
ANGULAR_TMP="$FRONTEND_DIR/dist-tmp"
npx ng build --output-path="$ANGULAR_TMP" --base-href=/
cd "$PROJECT_ROOT"

echo "==> Copying Angular output to Spring Boot static resources..."
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"

# The application builder puts browser files in a 'browser/' subdirectory
if [ -d "$ANGULAR_TMP/browser" ]; then
  cp -r "$ANGULAR_TMP/browser/." "$STATIC_DIR/"
else
  cp -r "$ANGULAR_TMP/." "$STATIC_DIR/"
fi
rm -rf "$ANGULAR_TMP"

# ── Spring Boot build ──────────────────────────────────────────────────────────
echo ""
echo "==> Building Spring Boot backend (fat JAR)..."
cd "$BACKEND_DIR"
mvn package -DskipTests -q
cd "$PROJECT_ROOT"

# ── jlink: create self-contained JRE ──────────────────────────────────────────
echo ""
echo "==> Creating self-contained JRE with jlink..."
RUNTIME_DIR="$PROJECT_ROOT/jre-runtime"
rm -rf "$RUNTIME_DIR"

jlink \
  --add-modules java.se,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported,jdk.zipfs,jdk.localedata,jdk.management \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --output "$RUNTIME_DIR"

# ── jpackage ───────────────────────────────────────────────────────────────────
echo ""
echo "==> Creating native macOS installer with jpackage..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

jpackage \
  --input "$BACKEND_DIR/target" \
  --main-jar "$JAR_NAME" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --dest "$OUTPUT_DIR" \
  --type dmg \
  --runtime-image "$RUNTIME_DIR" \
  --java-options "-Dspring.profiles.active=dist" \
  --java-options "-Xmx512m" \
  --java-options "-Djava.awt.headless=true"

rm -rf "$RUNTIME_DIR"

echo ""
echo "══════════════════════════════════════════════════════════════"
echo " Done!  Installer: $OUTPUT_DIR"
echo ""
echo " Before launching the app:"
echo "   1. Start the database:  docker compose up -d"
echo "   2. (Optional) Set DB credentials as env vars if you changed"
echo "      the defaults in your .env file:"
echo "        export DB_PASSWORD=your_password"
echo "        export JWT_SECRET=your_secret_min_32_chars"
echo "   3. Install the .dmg and launch '$APP_NAME'"
echo "   4. The browser opens automatically at http://localhost:8080"
echo "══════════════════════════════════════════════════════════════"
