# Code Generation Plan — US-013 Notification Backend

## Unit Context
- **Unit**: us013-notification-backend
- **Story**: US-013 (In-App-Benachrichtigungen bei Regelausführung)
- **Workspace Root**: /Users/simonfalkner/Documents/Uni/PR_SE/teaching-2026.ss.prse.braeuer.team3
- **Project Type**: Brownfield (Spring Boot 3.3.5, Java 21)

## Steps

- [x] Step 1: Create `RuleNotificationDto.java`
  - NEW file: `backend/src/main/java/at/jku/se/smarthome/dto/RuleNotificationDto.java`
  - Fields: `messageType` (= "ruleNotification"), `ruleName`, `success`, `message`
  - Full Javadoc (NFR-06)

- [x] Step 2: Modify `DeviceWebSocketHandler.java`
  - ADD method `broadcastRuleNotification(String userEmail, RuleNotificationDto dto)`
  - Analog zu `broadcastActivityLog()` — serialisiert DTO direkt (messageType ist im DTO enthalten)
  - Full Javadoc (NFR-06)

- [x] Step 3: Modify `RuleService.java`
  - ADD constructor parameter `DeviceWebSocketHandler wsHandler`
  - ADD private helper `buildSuccessMessage(Rule rule)`
  - ADD private helper `toUserMessage(Exception e)`
  - MODIFY `executeRule(Rule rule)` — broadcastRuleNotification nach Erfolg
  - MODIFY catch-Block in `evaluateRulesForDevice()` — broadcastRuleNotification bei Fehler
  - MODIFY catch-Block in `evaluateTimeRules()` — broadcastRuleNotification bei Fehler
  - Javadoc für neue public/private Methoden

- [x] Step 4: Modify `RuleServiceTest.java`
  - ADD `@Mock DeviceWebSocketHandler wsHandler`
  - UPDATE `setUp()` — wsHandler in RuleService-Konstruktor übergeben
  - ADD Test: `evaluateRules_threshold_fires_broadcastsSuccessNotification()`
  - ADD Test: `evaluateRules_event_fires_broadcastsSuccessNotification()`
  - ADD Test: `evaluateTimeRules_fires_broadcastsSuccessNotification()`
  - ADD Test: `evaluateRules_executionFails_broadcastsFailureNotification()`
  - ADD Test: `evaluateTimeRules_executionFails_broadcastsFailureNotification()`

- [x] Step 5: Modify `DeviceWebSocketHandlerTest.java`
  - ADD Test: `broadcastRuleNotification_openSession_sendsMessage()`
  - ADD Test: `broadcastRuleNotification_noSessions_doesNotThrow()`
