# Business Logic Model — US-013 Backend Notification

## Übersicht

Die Benachrichtigungs-Logik ist eine reine Erweiterung der bestehenden `RuleService`-Ausführungspipeline. Kein neuer Service, kein neuer Controller, keine neue DB-Tabelle.

## Datenfluss — Erfolgreiche Regelausführung

```
RuleService.executeRule(Rule rule)
  ├── DeviceService.updateStateAsActor(...)  [bereits vorhanden]
  └── DeviceWebSocketHandler.broadcastRuleNotification(
        userEmail   = rule.getUser().getEmail(),
        dto         = RuleNotificationDto(
                        ruleName = rule.getName(),
                        success  = true,
                        message  = buildSuccessMessage(rule)
                      )
      )
```

## Datenfluss — Fehlgeschlagene Regelausführung

Gilt für catch-Blöcke in beiden Evaluation-Methoden:

```
RuleService.evaluateRulesForDevice(...) / evaluateTimeRules()
  └── catch(Exception e)
        └── DeviceWebSocketHandler.broadcastRuleNotification(
              userEmail = rule.getUser().getEmail(),
              dto       = RuleNotificationDto(
                            ruleName = rule.getName(),
                            success  = false,
                            message  = toUserMessage(e)
                          )
            )
```

## Hilfsmethoden

### buildSuccessMessage(Rule rule)
Erzeugt eine kurze Aktionsbeschreibung aus `actionValue` + `actionDevice.getName()`:

| actionValue | actionDevice.getName() | Ergebnis |
|-------------|------------------------|----------|
| "true"      | "Wohnzimmer-Licht"     | "Wohnzimmer-Licht eingeschaltet" |
| "false"     | "Wohnzimmer-Licht"     | "Wohnzimmer-Licht ausgeschaltet" |
| "open"      | "Jalousie"             | "Jalousie geöffnet" |
| "close"     | "Jalousie"             | "Jalousie geschlossen" |
| andere Werte| beliebig               | "{actionValue}: {deviceName}" |

### toUserMessage(Exception e)
Mappt bekannte Exceptions auf benutzerfreundliche Texte:

| Bedingung | Ausgabe |
|-----------|---------|
| `ResponseStatusException` (404) | "Gerät nicht verfügbar" |
| `ResponseStatusException` (andere) | "Regelausführung fehlgeschlagen" |
| sonstige Exception | "Unbekannter Fehler" |

## Erweiterung DeviceWebSocketHandler

Neue Methode `broadcastRuleNotification(String userEmail, RuleNotificationDto dto)` — analog zu `broadcastActivityLog()`:
- Serialisiert `dto` zu JSON via Jackson
- Sendet an alle offenen Sessions des Benutzers
- Fehlerhafte Sessions werden entfernt und übersprungen
