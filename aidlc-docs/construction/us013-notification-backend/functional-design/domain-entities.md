# Domain Entities — US-013 Backend Notification

## RuleNotificationDto (neues DTO)

Kein JPA-Entity, kein Repository. Reine Datenklasse für die WebSocket-Übertragung.

| Feld        | Typ     | Beschreibung |
|-------------|---------|--------------|
| messageType | String  | Immer `"ruleNotification"` — dient dem Frontend als Routing-Key |
| ruleName    | String  | Name der ausgeführten Regel (aus `Rule.getName()`) |
| success     | boolean | `true` = Erfolg, `false` = Fehler |
| message     | String  | Bei Erfolg: Aktionsbeschreibung. Bei Fehler: benutzerfreundlicher Fehlertext |

### Serialisiertes JSON-Beispiel (Erfolg)
```json
{
  "messageType": "ruleNotification",
  "ruleName": "Nacht-Modus",
  "success": true,
  "message": "Wohnzimmer-Licht ausgeschaltet"
}
```

### Serialisiertes JSON-Beispiel (Fehler)
```json
{
  "messageType": "ruleNotification",
  "ruleName": "Nacht-Modus",
  "success": false,
  "message": "Gerät nicht verfügbar"
}
```

## Bestehende Entitäten (unverändert)
- `Rule` — liefert `name`, `actionDevice`, `actionValue`, `user`
- `User` — liefert `email` für WebSocket-Routing
