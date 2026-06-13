@echo off
:: build-dist.bat — Builds the SmartHome Orchestrator as a native Windows .msi installer.
::
:: Prerequisites (must be installed on the build machine):
::   - JDK 21  (java + jpackage in PATH)
::   - Maven 3.9+
::   - Node.js 18+ and npm
::   - WiX Toolset 3.x  (required by jpackage for .msi creation)
::     Download: https://github.com/wixtoolset/wix3/releases
::
:: The smarthome-db Docker container is NOT bundled — users must start it manually
:: with  docker compose up -d  before launching the installed app.
::
:: Usage:
::   build-dist.bat
::
:: Output:
::   dist-app\SmartHome Orchestrator-1.0.0.msi

setlocal EnableDelayedExpansion

set "APP_NAME=SmartHome Orchestrator"
set "APP_VERSION=1.0.0"
set "JAR_NAME=smarthome-0.0.1-SNAPSHOT.jar"

set "PROJECT_ROOT=%~dp0"
:: Remove trailing backslash
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"
set "BACKEND_DIR=%PROJECT_ROOT%\backend"
set "STATIC_DIR=%BACKEND_DIR%\src\main\resources\static"
set "OUTPUT_DIR=%PROJECT_ROOT%\dist-app"

:: ── Dependency checks ──────────────────────────────────────────────────────────
echo =^> Checking dependencies...

where java >nul 2>&1
if errorlevel 1 ( echo ERROR: 'java' not found. Please install JDK 21+. & exit /b 1 )

where mvn >nul 2>&1
if errorlevel 1 ( echo ERROR: 'mvn' not found. Please install Maven 3.9+. & exit /b 1 )

where node >nul 2>&1
if errorlevel 1 ( echo ERROR: 'node' not found. Please install Node.js 18+. & exit /b 1 )

where jpackage >nul 2>&1
if errorlevel 1 ( echo ERROR: 'jpackage' not found. Install a JDK ^(not just JRE^). & exit /b 1 )

:: ── Angular build ──────────────────────────────────────────────────────────────
echo.
echo =^> Building Angular frontend...
cd /d "%FRONTEND_DIR%"
call npm ci --prefer-offline
if errorlevel 1 ( echo ERROR: npm ci failed. & exit /b 1 )

set "ANGULAR_TMP=%FRONTEND_DIR%\dist-tmp"
call npx ng build --output-path="%ANGULAR_TMP%" --base-href=/
if errorlevel 1 ( echo ERROR: Angular build failed. & exit /b 1 )
cd /d "%PROJECT_ROOT%"

echo =^> Copying Angular output to Spring Boot static resources...
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"

:: Angular 19 application builder puts files in a 'browser' subdirectory
if exist "%ANGULAR_TMP%\browser" (
  xcopy /e /i /q "%ANGULAR_TMP%\browser\*" "%STATIC_DIR%\" >nul
) else (
  xcopy /e /i /q "%ANGULAR_TMP%\*" "%STATIC_DIR%\" >nul
)
rmdir /s /q "%ANGULAR_TMP%"

:: ── Spring Boot build ──────────────────────────────────────────────────────────
echo.
echo =^> Building Spring Boot backend ^(fat JAR^)...
cd /d "%BACKEND_DIR%"
call mvn package -DskipTests -q
if errorlevel 1 ( echo ERROR: Maven build failed. & exit /b 1 )
cd /d "%PROJECT_ROOT%"

:: ── jpackage ───────────────────────────────────────────────────────────────────
echo.
echo =^> Creating native Windows installer with jpackage...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

jpackage ^
  --input "%BACKEND_DIR%\target" ^
  --main-jar "%JAR_NAME%" ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --dest "%OUTPUT_DIR%" ^
  --type msi ^
  --java-options "-Dspring.profiles.active=dist" ^
  --java-options "-Xmx512m" ^
  --java-options "-Djava.awt.headless=false" ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser

if errorlevel 1 ( echo ERROR: jpackage failed. & exit /b 1 )

echo.
echo ================================================================
echo  Done!  Installer: %OUTPUT_DIR%
echo.
echo  Before launching the app:
echo    1. Start the database:  docker compose up -d
echo    2. (Optional) Set DB credentials as env vars if you changed
echo       the defaults in your .env file:
echo         set DB_PASSWORD=your_password
echo         set JWT_SECRET=your_secret_min_32_chars
echo    3. Run the .msi installer
echo    4. Launch "SmartHome Orchestrator" from the Start Menu
echo    5. The browser opens automatically at http://localhost:8080
echo ================================================================

endlocal
