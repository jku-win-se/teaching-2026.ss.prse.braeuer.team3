# US-012 Requirement Verification Questions
# Rules Frontend-Backend Integration

**Feature**: US-012 — Trigger-Typen im Frontend (Zeit / Schwellenwert / Ereignis)
**Datum**: 2026-04-28
**Status**: Awaiting answers

---

## Kontext

Das Backend (FR-10) implementiert zwei Trigger-Typen:
- `THRESHOLD` — Sensorwert über/unter einem Grenzwert (`GT` / `LT`)
- `EVENT` — Gerätezustandsänderung (on/off)

Das Frontend hat bereits eine Mock-Implementierung mit **drei** Trigger-Typen: `time`, `threshold`, `event`.
Die Mock-Daten werden aktuell ohne Backend-Anbindung verwendet.

---

## Frage 1: Zeitbasierter Trigger (kritisch)

US-012 fordert einen **zeitbasierten Trigger** (Uhrzeit/Datum). Das Backend (FR-10) unterstützt diesen Typ **nicht** — dort existieren nur `THRESHOLD` und `EVENT`.

Der zeitbasierte Anwendungsfall (z.B. "Licht täglich um 07:00 einschalten") ist bereits als **FR-09 Schedule** implementiert und voll funktionsfähig im Frontend.

Wie soll mit dem zeitbasierten Trigger umgegangen werden?

**[Answer]:** B

- A) Den zeitbasierten Trigger aus dem Rules-Dialog **entfernen** — Zeitsteuerung erfolgt über Schedules (FR-09), Rules decken nur THRESHOLD und EVENT ab
- B) Den zeitbasierten Trigger im Backend **ergänzen** (`TIME` als neuer `TriggerType`) — zusätzliche Backend-Entwicklung nötig
- C) Den zeitbasierten Trigger im Frontend als **rein UI-seitig** (nicht persistiert) behalten — technisch nicht sinnvoll, nur als Platzhalter
- D) Other (please describe after [Answer]: tag below)

---

## Frage 2: Threshold-Operatoren (Einschränkung)

Das Frontend zeigt aktuell 5 Operatoren: `>`, `<`, `>=`, `<=`, `==`.
Das Backend unterstützt nur `GT` (>) und `LT` (<).

Wie sollen die Operatoren im Dialog eingeschränkt werden?

**[Answer]:** A

- A) UI auf **GT und LT** beschränken (nur `>` und `<` anbieten) — entspricht Backend-Kapazität
- B) Backend um `GTE`, `LTE`, `EQ` erweitern — zusätzliche Backend-Entwicklung nötig
- C) Other (please describe after [Answer]: tag below)

---

## Frage 3: Geräteliste im Dialog

Der Dialog lädt aktuell Geräte aus **Mock-Daten**. Für die Backend-Anbindung müssen echte Geräte aus der API geladen werden.

Soll die Geräteliste im Rule-Dialog aus allen Geräten des Benutzers bestehen, oder gefiltert nach Raum?

**[Answer]:** B

- A) **Alle Geräte** des Benutzers (über alle Räume hinweg) — einfacher, kein Raumfilter nötig
- B) **Geräte gefiltert nach Raum** — Benutzer wählt zuerst Raum, dann Gerät
- C) Other (please describe after [Answer]: tag below)

---

## Frage 4: Edit und Delete in der UI

Aktuell zeigt die Rules-Listenansicht keine Edit- oder Delete-Buttons. Das Backend unterstützt `PUT /api/rules/{id}` und `DELETE /api/rules/{id}`.

Sollen Edit und Delete in diesem Zyklus implementiert werden?

**[Answer]:** A

- A) **Ja, beides** — Edit (Dialog mit Vorausfüllung) und Delete (mit Bestätigungsdialog)
- B) **Nur Delete** — Edit ist zu aufwändig für diesen Zyklus
- C) **Nein** — nur Erstellen, Aktivieren/Deaktivieren und Auflisten (CRUD minimal)
- D) Other (please describe after [Answer]: tag below)

---

## Frage 5: Umgang mit `hasConflict`

Das Frontend-Modell hat ein `hasConflict`-Flag (für Konflikt-Anzeige). Das Backend liefert dieses Feld nicht in `RuleResponse`.

Soll die Konflikt-Erkennung erhalten bleiben?

**[Answer]:** A

- A) **Entfernen** — Backend liefert kein `hasConflict`, UI-Only-Feature ist nicht sinnvoll
- B) **Backend erweitern** — `hasConflict` in `RuleResponse` berechnen und zurückgeben
- C) **Als Frontend-only behalten** — immer `false` (kein Konflikt) setzen, Anzeige bleibt erhalten
- D) Other (please describe after [Answer]: tag below)
