# Business Rules — US-013 Backend Notification

## BR-01: Benachrichtigung bei JEDER Regelausführung
Alle Trigger-Typen (EVENT, THRESHOLD, TIME) erzeugen Benachrichtigungen — keine Filterung.

## BR-02: Benachrichtigung nur an den Regelbesitzer
`broadcastRuleNotification` wird mit `rule.getUser().getEmail()` aufgerufen.
Andere Benutzer erhalten keine Benachrichtigungen für Regeln, die ihnen nicht gehören.

## BR-03: Benachrichtigung nur wenn WebSocket-Session aktiv
Wenn der Benutzer nicht verbunden ist (kein Eintrag in `sessionMap`), wird die Benachrichtigung still verworfen — kein Fehler, keine Persistenz.

## BR-04: Fehler-Benachrichtigung bei Exception im catch-Block
Wenn eine `Exception` beim Ausführen einer Regel geworfen wird, wird:
1. Der Fehler wie bisher via `log.warn` geloggt (bleibt unverändert)
2. **Zusätzlich** eine Fehler-Benachrichtigung an den Benutzer gesendet

## BR-05: Keine Änderung der bestehenden Fehlerbehandlungslogik
Die catch-Blöcke unterdrücken Exceptions weiterhin (kein Re-throw) — das Verhalten der Rule Engine bleibt stabil. Die Benachrichtigung ist ein reines Add-on.

## BR-06: Aktionsbeschreibung basiert auf actionValue
- `"true"` → „{deviceName} eingeschaltet"
- `"false"` → „{deviceName} ausgeschaltet"
- `"open"` → „{deviceName} geöffnet"
- `"close"` → „{deviceName} geschlossen"
- sonstige Werte → `"{actionValue}: {deviceName}"`

## BR-07: Fehlertext ist benutzerfreundlich (kein Stack-Trace, kein technischer Text)
`ResponseStatusException` mit Status 404 → „Gerät nicht verfügbar"
Alle anderen Exceptions → „Regelausführung fehlgeschlagen" oder „Unbekannter Fehler"
