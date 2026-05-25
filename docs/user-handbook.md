# Benutzerhandbuch — SmartHome Orchestrator

> Dieses Handbuch beschreibt alle Funktionen des SmartHome Orchestrators aus Benutzersicht.  
> Für die technische Einrichtung der Entwicklungsumgebung siehe [SETUP.md](../SETUP.md).

---

## Inhaltsverzeichnis

1. [Systemanforderungen & Start](#1-systemanforderungen--start)
2. [Registrierung & Login](#2-registrierung--login)
3. [Benutzerrollen](#3-benutzerrollen)
4. [Räume verwalten](#4-räume-verwalten)
5. [Geräte verwalten](#5-geräte-verwalten)
6. [Geräte steuern](#6-geräte-steuern)
7. [Automationsregeln (Rules)](#7-automationsregeln-rules)
8. [Zeitpläne (Schedules)](#8-zeitpläne-schedules)
9. [Vacation Mode](#9-vacation-mode)
10. [Day Simulation](#10-day-simulation)
11. [Szenen (Scenes)](#11-szenen-scenes)
12. [Energie-Dashboard](#12-energie-dashboard)
13. [Aktivitätslog & CSV-Export](#13-aktivitätslog--csv-export)
14. [Benachrichtigungen & Konfliktwarnungen](#14-benachrichtigungen--konfliktwarnungen)
15. [Settings: Mitglieder & MQTT](#15-settings-mitglieder--mqtt)

---

## 1. Systemanforderungen & Start

Der SmartHome Orchestrator läuft vollständig im Browser — keine Installation auf dem eigenen Gerät notwendig.

**Voraussetzungen für den lokalen Betrieb:**
- Docker Desktop (für die Datenbank)
- Java 21 + Maven (für das Backend)
- Node.js 18+ + npm (für das Frontend)

**Starten der Anwendung:**

```bash
# 1. Datenbank starten
docker compose up -d

# 2. Backend starten (im Ordner backend/)
mvn spring-boot:run

# 3. Frontend starten (im Ordner frontend/)
npm install && npm start
```

Die Anwendung ist danach unter **http://localhost:4200** erreichbar.

---

## 2. Registrierung & Login

### Registrierung

Beim ersten Besuch erscheint die Login-Seite. Über den Link "Register" gelangt man zum Registrierungsformular.

**Felder:**
- **E-Mail-Adresse** — muss einzigartig im System sein
- **Passwort** — wird verschlüsselt gespeichert, niemals im Klartext

Nach erfolgreicher Registrierung ist man automatisch als **Owner** (Eigentümer) des eigenen Haushalts eingetragen.

### Login

E-Mail und Passwort eingeben → "Login" klicken. Bei korrekten Zugangsdaten wird man automatisch zum Dashboard weitergeleitet. Die Session bleibt aktiv bis zum manuellen Abmelden (Logout-Button in der Navigation).

---

## 3. Benutzerrollen

Das System kennt zwei Rollen:

| Rolle | Rechte |
|-------|--------|
| **Owner** (Eigentümer) | Vollzugriff: Geräte/Räume verwalten, Regeln, Zeitpläne, Vacation Mode, Day Simulation, Log, Mitglieder und MQTT verwalten |
| **Member** (Mitglied) | Geräte steuern und Szenen aktivieren — keine Verwaltungsfunktionen |

Die eigene Rolle ist in den Einstellungen (Settings) sichtbar. Member sehen nur die Funktionen, die sie verwenden dürfen. Owner-only Seiten und Tabs wie Rules, Schedules, Vacation Mode, Day Simulation, Aktivitätslog, Household Access und MQTT Integration werden für Member nicht angezeigt bzw. sind nicht zugänglich.

---

## 4. Räume verwalten

> 🔒 Nur für Owner verfügbar.

Räume sind die zentrale Organisationsstruktur. Jedes Gerät gehört zu genau einem Raum.

**Neuen Raum anlegen:**  
Auf der Rooms-Seite den Button "Add Room" klicken → Namen eingeben → bestätigen.

**Raum umbenennen:**  
Das Stift-Icon neben dem Raumnamen anklicken → neuen Namen eingeben.

**Raum löschen:**  
Das Papierkorb-Icon neben dem Raumnamen anklicken → Löschung bestätigen.  
⚠️ Beim Löschen eines Raums werden auch alle darin enthaltenen Geräte entfernt.

---

## 5. Geräte verwalten

> 🔒 Nur für Owner verfügbar.

### Gerät hinzufügen

In einem Raum den Button "Add Device" klicken. Im Dialog:
- **Name** vergeben (z.B. "Wohnzimmerlampe")
- **Gerätetyp** auswählen:

| Typ | Beschreibung |
|-----|-------------|
| Switch | Ein/Aus-Schalter |
| Dimmer | Helligkeit 0–100 % |
| Thermostat | Zieltemperatur in °C |
| Sensor | Messwert mit Einheit (z.B. °C, %, lux) |
| Cover / Blind | Position 0 % (geschlossen) bis 100 % (offen) |

### Gerät umbenennen

Das Stift-Icon auf der Gerätekarte anklicken → neuen Namen eingeben.

### Gerät entfernen

Das Papierkorb-Icon auf der Gerätekarte anklicken → Löschung bestätigen.

Member sehen diese Verwaltungsaktionen nicht. Sie können vorhandene Geräte steuern, aber keine Räume oder Geräte anlegen, umbenennen oder löschen.

---

## 6. Geräte steuern

Jedes Gerät wird als Karte auf der Rooms-Seite angezeigt. Der aktuelle Zustand wird in Echtzeit aktualisiert — keine manuelle Seitenaktualisierung notwendig.

**Steuerung je nach Gerätetyp:**

- **Switch**: Toggle-Schalter anklicken → sofortiges Ein/Aus
- **Dimmer**: Schieberegler für Helligkeit (0–100 %)
- **Thermostat**: Zieltemperatur über Plus/Minus oder Eingabefeld setzen
- **Sensor**: Aktuellen Messwert über "Inject Value" einspeisen (Simulationsmodus)
- **Cover/Blind**: Schieberegler für Position (0 % = geschlossen, 100 % = offen)

Jede manuelle Zustandsänderung wird automatisch im Aktivitätslog erfasst.

---

## 7. Automationsregeln (Rules)

> 🔒 Nur für Owner verfügbar.

Regeln ermöglichen automatische Gerätesteuerung nach dem Prinzip **IF \<Trigger\> THEN \<Aktion\>**.

### Regel erstellen

Auf der Rules-Seite "Add Rule" klicken. Im Dialog:

1. **Name** der Regel vergeben
2. **Trigger-Typ** wählen:

| Trigger-Typ | Beschreibung | Beispiel |
|-------------|-------------|---------|
| **TIME** | Zeitbasiert: Regel feuert täglich zu einer bestimmten Uhrzeit an gewählten Wochentagen | Mo–Fr um 07:00 → Licht an |
| **THRESHOLD** | Schwellwertbasiert: Sensor-Wert überschreitet (GT) oder unterschreitet (LT) einen Grenzwert | Temperatur < 18 °C → Heizung an |
| **EVENT** | Zustandsbasiert: Ein Gerät wechselt in einen bestimmten Zustand | Bewegungssensor aktiv → Flurlampe an |

3. **Trigger-Gerät** und Bedingung konfigurieren (je nach Typ)
4. **Aktions-Gerät** und Zielzustand festlegen
5. Speichern

### Regel aktivieren/deaktivieren

Den Toggle auf der Regelkarte umlegen. Deaktivierte Regeln werden nicht ausgewertet.

### Konfliktwarnung

Beim Speichern einer Regel prüft das System automatisch, ob eine andere Regel dasselbe Gerät in einen widersprüchlichen Zustand versetzen würde. Wird ein Konflikt erkannt, erscheint eine Warnung — die Regel kann dennoch gespeichert werden.

---

## 8. Zeitpläne (Schedules)

> 🔒 Nur für Owner verfügbar.

Zeitpläne sind wiederkehrende Aktionen zu festen Uhrzeiten — ohne Bedingungen (anders als Regeln).

**Beispiel:** Jeden Abend um 22:00 Uhr alle Lichter ausschalten.

### Zeitplan erstellen

Auf der Schedules-Seite "Add Schedule" klicken. Im Dialog:

1. **Name** vergeben
2. **Zielgerät** auswählen
3. **Aktion** festlegen (Zielzustand des Geräts)
4. **Uhrzeit** (Stunde + Minute) einstellen
5. **Wochentage** auswählen, an denen der Zeitplan aktiv ist
6. Speichern

Das System prüft alle aktiven Zeitpläne jede Minute und führt fällige Einträge aus. Die Ausführung wird im Aktivitätslog protokolliert.

### Zeitplan aktivieren/deaktivieren

Den Toggle auf der Zeitplankarte umlegen.

---

## 9. Vacation Mode

> 🔒 Nur für Owner verfügbar.

Der Vacation Mode aktiviert oder deaktiviert einen bestehenden Zeitplan für einen bestimmten Urlaubszeitraum. Damit kann ein Haushalt während der Abwesenheit anders automatisiert werden, ohne normale Zeitpläne dauerhaft umbauen zu müssen.

**Beispiel:** Während des Urlaubs wird ein Zeitplan aktiviert, der abends Licht einschaltet. Alternativ kann ein normaler Alltags-Zeitplan für den Urlaubszeitraum deaktiviert werden.

### Vacation Mode erstellen

Auf der Vacation-Mode-Seite den Plus-Button anklicken. Im Dialog:

1. **Vacation name** vergeben
2. **Schedule** auswählen
3. **Action** auswählen:
   - **Enable schedule during vacation**: Der Zeitplan wird im Urlaubszeitraum aktiviert
   - **Disable schedule during vacation**: Der Zeitplan wird im Urlaubszeitraum deaktiviert
4. **Start date** und **End date** festlegen
5. "Add Vacation Mode" klicken

Das Enddatum darf nicht vor dem Startdatum liegen. Der gewählte Zeitraum ist inklusive Start- und Enddatum.

### Statusanzeigen

| Status | Bedeutung |
|--------|-----------|
| **Active** | Der Vacation Mode ist aktuell im gewählten Zeitraum aktiv |
| **Upcoming** | Der Vacation Mode startet erst zu einem späteren Datum |
| **Deactivated** | Der Vacation Mode wurde beendet oder vorzeitig deaktiviert |

### Vacation Mode deaktivieren oder löschen

Über "Deactivate" kann ein Vacation Mode vorzeitig beendet werden. Über das Papierkorb-Icon kann der Eintrag gelöscht werden.

---

## 10. Day Simulation

> 🔒 Nur für Owner verfügbar.

Die Day Simulation testet Automationsregeln in einem simulierten 24-Stunden-Tag. Sie dient als Vorschau: Das Live-System wird nicht verändert, es werden keine echten Gerätezustände überschrieben und keine Aktivitätslog-Einträge geschrieben.

### Simulation starten

Auf der Simulation-Seite:

1. **Day to Simulate** auswählen (Montag bis Sonntag)
2. Unter **Starting Device States** die Startzustände der Geräte setzen
3. "Run Simulation" klicken

Die Simulation verwendet die aktuellen Geräte als Ausgangspunkt. Die Startzustände können vor dem Lauf angepasst werden, um unterschiedliche Situationen zu testen.

### Timeline auswerten

Nach dem Lauf zeigt die **Simulation Timeline**, welche Automationen an diesem Tag auslösen würden.

| Anzeige | Bedeutung |
|---------|-----------|
| Uhrzeit | Simulierter Zeitpunkt der Aktion |
| Gerät und Raum | Betroffenes Gerät und dessen Raum |
| Aktion | Zielzustand, der durch die Regel gesetzt wird |
| Regel | Name der auslösenden Automationsregel |

Wenn ein Ereignis als **redundant** markiert ist, hat die Regel zwar ausgelöst, aber keine praktische Änderung bewirkt, weil das Gerät bereits im Zielzustand war.

---

## 11. Szenen (Scenes)

Szenen sind benannte Gruppen von Gerätezuständen, die mit einem einzigen Klick aktiviert werden können.

**Beispiel:** Szene "Filmabend" → Licht auf 20 % dimmen, Rollade schließen.

### Szene erstellen

Auf der Scenes-Seite "Add Scene" klicken:

1. **Name** und **Icon** vergeben
2. **Geräteaktionen** hinzufügen: Gerät auswählen + Zielzustand festlegen
3. Speichern

### Szene aktivieren

Den "Play"-Button auf der Szenenkarte anklicken. Alle konfigurierten Gerätezustände werden sofort angewendet. Die Aktivierung erscheint im Aktivitätslog.

---

## 12. Energie-Dashboard

Das Energie-Dashboard zeigt den geschätzten Stromverbrauch aller Geräte.

**Anzeige:**
- **Pro Gerät**: geschätzte Leistungsaufnahme (Watt) und kumulierter Verbrauch
- **Pro Raum**: Summe aller Geräte im Raum
- **Haushalt gesamt**: Summe aller Räume
- **Zeitraum**: täglich und wöchentlich

> Die Werte sind Schätzungen basierend auf Gerätetyp und aktuellem Zustand — keine Messung echter Hardware.

### CSV-Export

Über den Button "Export CSV" auf der Energie-Seite kann eine Zusammenfassung des Verbrauchs als CSV-Datei heruntergeladen werden (kompatibel mit Excel und LibreOffice).

---

## 13. Aktivitätslog & CSV-Export

> 🔒 Nur für Owner verfügbar.

Das Aktivitätslog erfasst automatisch jeden Zustandswechsel im Haushalt:

| Spalte | Beschreibung |
|--------|-------------|
| Zeitstempel | Datum und Uhrzeit des Ereignisses |
| Gerät | Name des betroffenen Geräts |
| Actor | Auslöser: Benutzername, Regelname oder "Schedule" |
| Beschreibung | Was hat sich geändert (z.B. "switch → on") |

### Filtern

Das Log kann nach Zeitraum (`von` / `bis`) und nach einzelnem Gerät gefiltert werden.

### Eintrag löschen

Einzelne Einträge können über das Papierkorb-Icon entfernt werden (z.B. zum Bereinigen von Testdaten).

### CSV-Export

Über "Export CSV" wird das gefilterte Log als CSV-Datei exportiert.

---

## 14. Benachrichtigungen & Konfliktwarnungen

### Regelausführung (Benachrichtigungen)

Wenn eine Automationsregel feuert — also eine Aktion automatisch auslöst — erscheint eine **In-App-Benachrichtigung** (Toast/Snackbar) im Browser. Sie enthält den Regelnamen und das betroffene Gerät.

Tritt bei der Regelausführung ein Fehler auf (z.B. Gerät nicht erreichbar), wird ebenfalls eine Fehler-Benachrichtigung angezeigt.

### Konfliktwarnungen (Scheduling Conflicts)

Beim Speichern einer neuen oder geänderten Regel prüft das System, ob ein Konflikt mit einer bestehenden Regel besteht. Ein Konflikt liegt vor, wenn zwei Regeln dasselbe Gerät gleichzeitig in widersprüchliche Zustände versetzen würden (z.B. eine Regel schaltet Lampe ein, eine andere schaltet sie zur selben Zeit aus). Die Warnung wird direkt im Regel-Dialog angezeigt.

---

## 15. Settings: Mitglieder & MQTT

Die Settings-Seite enthält das eigene Profil. Owner sehen zusätzlich Tabs für Haushaltszugriff und MQTT Integration. Member sehen nur die Profilfunktionen.

### Household Access

> 🔒 Nur für Owner verfügbar.

Über den Tab **Household Access** können weitere Personen als **Member** zum Haushalt eingeladen werden.

#### Mitglied einladen

1. Settings-Seite öffnen
2. Tab "Household Access" öffnen
3. E-Mail-Adresse des einzuladenden Nutzers eingeben
4. "Invite" klicken

Die eingeladene Person muss bereits ein registriertes Konto im System haben. Nach der Einladung kann sie sich mit ihrem eigenen Account anmelden und hat sofort Zugriff auf die Haushaltsdaten mit Member-Rechten.

#### Mitglied entfernen

In der Mitgliederliste "Revoke Access" neben dem Mitglied anklicken → Zugang wird sofort entzogen.

### MQTT Integration

> 🔒 Nur für Owner verfügbar.

Der Tab **MQTT Integration** simuliert eine MQTT-Anbindung. Es wird eine Broker-Konfiguration gespeichert und ein Nachrichtenprotokoll geführt, aber kein echter MQTT-Broker kontaktiert.

#### MQTT konfigurieren

1. Settings-Seite öffnen
2. Tab "MQTT Integration" öffnen
3. **Broker-URL** eingeben (z.B. `mqtt://localhost:1883`)
4. **Basis-Topic** eingeben (z.B. `smarthome`)
5. "Speichern" klicken

Die Gerätezustände werden bei aktiver Verbindung unter dem Schema `[topic]/devices/[id]` publiziert.

#### Verbindung herstellen oder trennen

Nach dem Speichern der Konfiguration kann über "Verbinden" eine simulierte Verbindung hergestellt werden. Der Status wechselt von **Getrennt** zu **Verbunden (simuliert)**. Über "Trennen" wird die simulierte Verbindung wieder beendet.

Wenn keine Verbindung aktiv ist, bleibt das Smart-Home-System normal verwendbar. MQTT-Publish-Einträge werden dann nicht erzeugt.

#### Nachrichtenprotokoll

Im simulierten Nachrichtenprotokoll erscheinen Systemmeldungen und `PUBLISH`-Nachrichten, sobald bei aktiver Verbindung Gerätezustände geändert werden. Das Protokoll kann über das Papierkorb-Icon gelöscht werden.

Typische Einträge enthalten Zeitstempel, Richtung (`SYSTEM` oder `PUBLISH`), Topic und Payload.
