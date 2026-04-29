# US-013 Requirement Verification Questions
# In-App-Benachrichtigungen bei Regelausführung

**Feature**: Als Benutzer möchte ich In-App-Benachrichtigungen erhalten, wenn eine Regel ausgeführt wird oder fehlschlägt, damit ich stets informiert bin.

**Akzeptanzkriterien (aus Issue):**
- Benachrichtigung bei erfolgreicher Regelausführung sichtbar
- Benachrichtigung bei fehlgeschlagener Regelausführung mit Fehlergrund sichtbar

---

## Frage 1: Darstellungsform der Benachrichtigung

Wie sollen die Benachrichtigungen angezeigt werden?

**A)** Toast/Snackbar (kurze Einblendung am unteren Bildschirmrand, verschwindet automatisch nach 3–5 Sekunden) — analog zu bestehenden Snackbars im Projekt (z. B. beim Löschen einer Regel)

**B)** Notification-Panel — Klick auf das Glocken-Icon (bereits in der Toolbar vorhanden) öffnet eine Dropdown-Liste aller Benachrichtigungen

**C)** Beides — Toast für sofortige Rückmeldung + Notification-Panel für persistente History

**D)** Andere (bitte nach dem [Answer]-Tag beschreiben)

[Answer]: C

---

## Frage 2: Persistenz der Benachrichtigungen

Sollen Benachrichtigungen gespeichert und später wieder abrufbar sein?

**A)** Nein — nur transient, während der aktuellen Browser-Session sichtbar (kein Backend-Storage erforderlich)

**B)** Ja — im Backend persistiert, damit der Benutzer die letzten N Benachrichtigungen auch nach einem Seitenreload sieht

**C)** Andere (bitte nach dem [Answer]-Tag beschreiben)

[Answer]: A

---

## Frage 3: Umfang der Erfolgs-Benachrichtigungen

Bei welchen Regelausführungen soll eine Erfolgs-Benachrichtigung erscheinen?

**A)** Bei ALLEN Regelausführungen (EVENT, THRESHOLD, TIME) — auch bei automatisch im Hintergrund ausgelösten TIME-Regeln

**B)** Nur bei durch Benutzerinteraktion ausgelösten Regeln (EVENT, THRESHOLD) — TIME-Regeln laufen still im Hintergrund

**C)** Andere (bitte nach dem [Answer]-Tag beschreiben)

[Answer]: A

---

## Frage 4: Inhalt der Benachrichtigung

Was soll eine Benachrichtigung enthalten?

**A)** Regelname + kurze Beschreibung der Aktion (z. B. „Regel 'Nacht-Modus' ausgeführt: Wohnzimmer-Licht ausgeschaltet")

**B)** Nur Regelname (z. B. „Regel 'Nacht-Modus' ausgeführt ✓")

**C)** Andere (bitte nach dem [Answer]-Tag beschreiben)

[Answer]: A

---

## Frage 5: Fehlermeldungs-Detailgrad

Wie detailliert sollen Fehlermeldungen bei fehlgeschlagener Regelausführung sein?

**A)** Regelname + technische Fehlermeldung (z. B. „Regel 'Nacht-Modus' fehlgeschlagen: Device not found")

**B)** Regelname + benutzerfreundlicher Fehlertext (z. B. „Regel 'Nacht-Modus' fehlgeschlagen: Gerät nicht verfügbar")

**C)** Andere (bitte nach dem [Answer]-Tag beschreiben)

[Answer]: B

---

## Frage 6: Übertragungskanal

Über welchen Kanal sollen Benachrichtigungen vom Backend zum Frontend gesendet werden?

**A)** Über die bestehende WebSocket-Verbindung (`/ws/devices`) mit neuem `messageType: "ruleNotification"` — kein zusätzlicher Infrastrukturaufwand

**B)** Neuer dedizierter Endpunkt (z. B. Polling oder separater WebSocket)

**C)** Andere (bitte nach dem [Answer]-Tag beschreiben)

[Answer]: wahrscheinlich wäre eine ws/notifications die schönere lösung, oder?
