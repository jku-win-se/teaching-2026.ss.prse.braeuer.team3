# DOC-01: Requirement Verification Questions
# Projektdokumentation (technisch + benutzerseitig) + Aktualisierte Architekturdokumentation

**Datum**: 2026-05-03  
**Status**: Warte auf Antworten des Teams

> **Anleitung**: Bitte füllt alle `[Answer]:` Tags direkt in dieser Datei aus.  
> Wählt den passenden Buchstaben (A, B, C …) oder beschreibt eure Antwort nach dem Tag.

---

## Q1 – Zielgruppe der benutzerseitigen Dokumentation

Für wen wird das User Manual / die Benutzerdokumentation primär geschrieben?

- A) Für den Lehrenden / die Bewertungskommission (Professor, Tutor) — formaler Ton, vollständige Feature-Abdeckung
- B) Für einen fiktiven Endnutzer der Anwendung (Haushalts-Nutzer ohne Technikkenntnisse) — einfache Sprache, Screenshot-orientiert
- C) Für beide — zwei separate Dokumente (ein kurzes User Manual + ein technisches Handbuch)
- D) Anderes (bitte beschreiben)

[Answer]:

---

## Q2 – Sprache der Dokumentation

In welcher Sprache soll die Dokumentation verfasst werden?

- A) Deutsch (beide Dokumente)
- B) Englisch (beide Dokumente)
- C) Technische Doku auf Englisch, User Manual auf Deutsch
- D) Anderes

[Answer]:

---

## Q3 – Format / Ausgabeformat

In welchem Format soll die Dokumentation geliefert werden?

- A) Markdown-Dateien im Repository (`.md`) — direkt lesbar auf GitHub/GitLab
- B) Word-Dokument (`.docx`) — für Abgabe oder Druck
- C) PDF — für formale Abgabe
- D) Sowohl Markdown im Repo als auch ein PDF für die Abgabe
- E) Anderes

[Answer]:

---

## Q4 – Umfang der technischen Dokumentation

Was soll die **technische Dokumentation** abdecken? (Mehrfachauswahl möglich — bitte alle zutreffenden Buchstaben angeben)

- A) Architekturübersicht (System-Diagramm, Schichten, Komponenten)
- B) Setup / Local Development Guide (Docker, Backend starten, Frontend starten)
- C) REST-API-Referenz (alle Endpunkte mit Request/Response-Beispielen)
- D) Datenbankschema / Entitätenbeschreibung
- E) CI/CD-Pipeline-Beschreibung
- F) Code-Qualitäts-Regeln (PMD, Javadoc, Test-Coverage-Ziele)
- G) Anderes

[Answer]:

---

## Q5 – Umfang der benutzerseitigen Dokumentation

Was soll das **User Manual** abdecken? (Mehrfachauswahl möglich)

- A) Registrierung & Login
- B) Räume und Geräte verwalten (erstellen, umbenennen, löschen)
- C) Geräte steuern (Switch, Dimmer, Thermostat, Sensor, Blind)
- D) Automationsregeln erstellen (Trigger-Typen: Zeit, Schwellwert, State-Change)
- E) Zeitpläne (Schedules) konfigurieren
- F) Szenen anlegen und aktivieren
- G) Energie-Dashboard nutzen
- H) Aktivitätslog lesen + CSV-Export
- I) Mitglieder einladen / verwalten (Owner vs. Member Rollen)
- J) In-App-Benachrichtigungen / Konfliktwarnungen
- K) Alle oben genannten (vollständige Abdeckung)

[Answer]:

---

## Q6 – Ablageort der Dokumente

Wo sollen die fertigen Dokumente im Repository abgelegt werden?

- A) Im Root-Verzeichnis (z.B. `README.md` erweitern, `USER_GUIDE.md` hinzufügen)
- B) In einem eigenen `docs/` Verzeichnis
- C) In `localDocs/` (bereits im Repo vorhanden und in `.gitignore` ausgeschlossen — also nur lokal)
- D) Anderes

[Answer]:

---

## Q7 – Aktualisierte Architekturdokumentation: Tiefe

Wie detailliert soll die aktualisierte Architekturdokumentation sein?

- A) Nur das Systemdiagramm (Mermaid-Diagramm wie `ARCHITECTURE_UML.md`) aktualisieren — alle neuen Services und Controller einpflegen
- B) Systemdiagramm + kurze Komponentenbeschreibungen (wie in `aidlc-docs/inception/reverse-engineering/architecture.md`)
- C) Vollständige Architekturdokumentation: Diagramm + Komponentenbeschreibungen + Sequenzdiagramme für Key Flows (z.B. Rule Execution, Schedule Trigger, Scene Activation)
- D) Anderes

[Answer]:

---

## Q8 – Deadline / Abgabetermin

Gibt es einen konkreten Abgabetermin für diese Dokumentation?

- A) Ja — bitte Datum nach [Answer]: angeben
- B) Nein / so bald wie möglich
- C) Anderes

[Answer]:

---

## Q9 – Vorhandene Inhalte wiederverwenden

Sollen bestehende Dokumente als Basis verwendet werden?

- A) Ja — `ARCHITECTURE_UML.md` als Basis für die Architekturdoku, `README.md` / `SETUP.md` als Basis für die technische Doku
- B) Nein — alles neu schreiben
- C) Nur bestimmte Teile (bitte beschreiben)

[Answer]:

---

## Q10 – Screenshots / UI-Mockups

Soll die benutzerseitige Dokumentation Screenshots oder UI-Beschreibungen enthalten?

- A) Nein — reine Textbeschreibung der Features reicht
- B) Ja — ASCII/Text-basierte UI-Mockups oder beschreibende Platzhalter wo Screenshots hingehören
- C) Ja — echte Screenshots (müsst ihr dann selbst einfügen, da ich keinen laufenden Browser habe)

[Answer]:

---

*Bitte alle Felder ausfüllen und dann mit „weiter" oder „proceed" bestätigen.*
