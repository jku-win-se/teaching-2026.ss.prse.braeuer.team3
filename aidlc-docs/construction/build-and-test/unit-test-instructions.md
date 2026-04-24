# Unit Test Execution — SmartHome Orchestrator

## Run Unit Tests
```bash
cd backend
mvn test
```

## Results (Bugfix #71 — Activity Log Timeframe Filter)
- **Total Tests**: 145
- **Passed**: 145
- **Failed**: 0
- **Coverage**: 88% (target: ≥75% — NFR-03 satisfied)
- **Report**: `target/site/jacoco/`

## Test Classes
| Class | Tests | Status |
|-------|-------|--------|
| ActivityLogControllerTest | 5 | PASS |
| ActivityLogServiceTest | 14 | PASS |
| DeviceServiceTest | 18 | PASS |
| AuthServiceTest | 11 | PASS |
| RoomServiceTest | 12 | PASS |
| DeviceControllerTest | 16 | PASS |
| RoomControllerTest | 11 | PASS |
| AuthControllerTest | 8 | PASS |
| DeviceWebSocketHandlerTest | 10 | PASS |
| JwtHandshakeInterceptorTest | 5 | PASS |
| WebSocketConfigTest | 3 | PASS |
| JwtUtilTest | 8 | PASS |
| DeviceTest | 10 | PASS |
| RoomTest | 7 | PASS |
| UserTest | 7 | PASS |
