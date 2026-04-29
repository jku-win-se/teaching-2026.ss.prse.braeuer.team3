# Business Logic Model — US-012 Backend (Unit 1)

## TIME Trigger Evaluation — RuleScheduler

Läuft jede Minute via `@Scheduled(cron = "0 * * * * *")`. Folgt exakt dem Muster von `ScheduleService.runDueSchedules()`.

```
runDueTimeRules():
  now = LocalDateTime.now()
  today = now.getDayOfWeek().name()          // z.B. "MONDAY"
  
  candidates = ruleRepository
    .findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
        TIME, now.getHour(), now.getMinute())
  
  für jede rule in candidates:
    days = rule.getTriggerDaysOfWeek().split(",")
    wenn today in days:
      ruleService.executeRule(rule)           // bestehende private Methode
```

**Repository-Methode** (neu in `RuleRepository`):
```java
List<Rule> findByEnabledTrueAndTriggerTypeAndTriggerHourAndTriggerMinute(
    TriggerType triggerType, int hour, int minute);
```

---

## RuleService — createRule / updateRule (Anpassungen)

```
applyRequest(rule, request, triggerDevice, actionDevice):
  rule.name = request.name
  rule.triggerType = request.triggerType
  
  wenn triggerType == TIME:
    rule.triggerDevice = null
    rule.triggerHour = request.triggerHour
    rule.triggerMinute = request.triggerMinute
    rule.triggerDaysOfWeek = request.triggerDaysOfWeek
    rule.triggerOperator = null
    rule.triggerThresholdValue = null
  sonst:
    rule.triggerDevice = triggerDevice
    rule.triggerHour = null
    rule.triggerMinute = null
    rule.triggerDaysOfWeek = null
    // THRESHOLD-Felder wie bisher
  
  rule.actionDevice = actionDevice
  rule.actionValue = request.actionValue
  rule.enabled = request.enabled ?? true
```

```
createRule(email, request):
  user = resolveUser(email)
  actionDevice = resolveOwnedDevice(user, request.actionDeviceId)
  
  wenn request.triggerType == TIME:
    triggerDevice = null
  sonst:
    triggerDevice = resolveOwnedDevice(user, request.triggerDeviceId)
  
  rule = new Rule()
  rule.user = user
  applyRequest(rule, request, triggerDevice, actionDevice)
  speichern + zurückgeben
```

---

## toResponse — Anpassung

```
toResponse(rule):
  return new RuleResponse(
    id, name, enabled, triggerType,
    rule.triggerDevice?.id,         // null für TIME
    rule.triggerDevice?.name,       // null für TIME
    triggerOperator,
    triggerThresholdValue,
    rule.triggerHour,               // NEU
    rule.triggerMinute,             // NEU
    rule.triggerDaysOfWeek,         // NEU
    actionDevice.id,
    actionDevice.name,
    actionValue
  )
```
