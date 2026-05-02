# Requirements Clarification Questions — FR-13 & FR-20

**Feature Scope:**
- **FR-13**: Zwei Benutzerrollen: Eigentümer (vollständiger Zugriff) und Mitglied (nur Steuerung, keine Geräte-/Regelverwaltung)
- **FR-20**: Eigentümer kann weitere Mitglieder per E-Mail-Adresse einladen und deren Zugang widerrufen

Bitte beantworte alle Fragen, indem du den passenden Buchstaben nach dem `[Answer]:` Tag einträgst.
Falls keine Option passt, wähle den letzten Buchstaben (Other) und beschreibe deine Anforderung.

---

## Question 1
Wie sieht das Datenmodell für die gemeinsame "Home"-Ebene aus?

Aktuell besitzt jeder User seine eigenen Räume und Geräte. Wenn ein Member eingeladen wird, welche Daten sieht er?

A) Der Member sieht **alle** Räume und Geräte des Eigentümers (vollständige Sicht auf das Home des Owners)
B) Der Member sieht nur explizit freigegebene Räume (Owner wählt pro Raum, ob er geteilt wird)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 2
Was passiert, wenn ein Member-User selbst auch Räume und Geräte hat (eigenes Home)?

A) Ein User kann gleichzeitig Eigentümer seines eigenen Homes UND Mitglied eines anderen Homes sein — die UI zeigt beide Homes separat (z.B. Tabs oder Dropdown-Auswahl)
B) Ein User kann gleichzeitig Eigentümer seines eigenen Homes UND Mitglied eines anderen Homes sein — alle Räume werden in einer gemeinsamen Ansicht angezeigt (merged)
C) Ein eingeladener Member verliert keinen Zugang zu seinem eigenen Home, aber das System unterstützt vorerst nur die Anzeige eines Homes gleichzeitig
X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 3
Muss eine Einladung vom eingeladenen User aktiv **akzeptiert** werden, oder wird die Mitgliedschaft sofort aktiv?

A) Sofort aktiv — sobald der Owner die E-Mail-Adresse eingibt und bestätigt, hat der Member Zugriff (vorausgesetzt, der User mit dieser E-Mail ist registriert)
B) Einladung + Akzeptanz-Workflow — der eingeladene User sieht eine ausstehende Einladung und muss sie annehmen oder ablehnen
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 4
Was passiert, wenn der Owner eine E-Mail-Adresse einlädt, die **noch kein Konto** hat?

A) Die Einladung wird abgelehnt / Fehlermeldung: "Diese E-Mail-Adresse ist nicht registriert"
B) Die Einladung wird gespeichert (pending) — sobald sich der User mit dieser E-Mail registriert, wird er automatisch Mitglied
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 5
Soll das System tatsächlich **E-Mails versenden** (z.B. Einladungslink per SMTP/E-Mail-Service)?

A) Nein — keine echten E-Mails; die Einladung erfolgt rein über die App-UI (Owner gibt E-Mail ein, Backend verknüpft die Accounts)
B) Ja — das System soll eine echte Einladungs-E-Mail an die eingeladene Adresse senden (SMTP konfigurieren)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 6
Was darf ein **Mitglied (Member)** konkret tun und was nicht?

Bitte bestätige die folgende Abgrenzung oder korrigiere sie:

A) Member darf: Gerätezustand ändern (ein/aus), Geräte-Werte lesen, Räume und Geräte **ansehen**. Member darf NICHT: Räume erstellen/umbenennen/löschen, Geräte hinzufügen/umbenennen/löschen, Regeln erstellen/bearbeiten/löschen, Zeitpläne erstellen/bearbeiten/löschen
B) Member darf zusätzlich auch Zeitpläne steuern (aber nicht erstellen/löschen)
C) Member darf zusätzlich auch Regeln ansehen (aber nicht bearbeiten)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 7
Wie soll die **Mitgliederverwaltung-UI** für den Owner aussehen?

A) Eigene Einstellungsseite / Settings-Panel mit Liste aller Mitglieder, Einladungsformular und Entfernen-Schaltfläche
B) In der bestehenden Header/Navbar als Dropdown-Menü integriert
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 8
Kann ein Owner **sich selbst als Owner** auch anderen Homes beitreten (also in mehreren Homes Member sein)?

A) Ja — ein User kann Member in beliebig vielen Homes sein und gleichzeitig Owner seines eigenen Homes
B) Nein — ein User kann nur entweder Owner oder Member sein (nicht beides gleichzeitig)
X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 9
Was passiert mit dem Aktivitätsprotokoll (Activity Log), wenn ein Member ein Gerät steuert?

A) Die Aktion wird im Activity Log des Owners protokolliert, mit dem Member-Namen als Akteur
B) Die Aktion wird im Activity Log protokolliert, aber ohne Unterscheidung wer (Owner oder Member) sie ausgeführt hat
C) Das Activity Log ist für diese Phase nicht relevant / Member-Aktionen werden nicht extra protokolliert
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 10
Soll die **Rolle (Owner/Member)** im JWT-Token kodiert werden, oder wird sie bei jeder Anfrage aus der Datenbank gelesen?

A) Rolle wird aus der Datenbank gelesen (sicherer, sofort wirksam bei Widerruf)
B) Rolle wird im JWT-Token mitgegeben (performanter, aber Widerruf wirkt erst beim nächsten Token-Refresh)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

*Wenn du alle Fragen beantwortet hast, teile mir das kurz mit, damit ich mit der Requirements-Analyse fortfahren kann.*
