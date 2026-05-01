# Requirements — FR-13 & FR-20: Benutzerrollen & Mitgliederverwaltung

## Intent Analysis

| Feld | Inhalt |
|------|--------|
| **User Request** | Einführung von zwei Benutzerrollen (Eigentümer/Mitglied) und einem Einladungsmechanismus per E-Mail |
| **Request Type** | New Feature |
| **Scope Estimate** | Multiple Components (Backend: DB, Domain, Service, Controller; Frontend: Settings-UI, rollenbasierte Sichtbarkeit) |
| **Complexity Estimate** | Moderate — neue Autorisierungslogik quer durch alle bestehenden Controller, neue DB-Tabelle, neuer Service/Controller, Frontend-Anpassungen |

---

## Functional Requirements

### FR-13: Benutzerrollen

**FR-13.1 — Rollenmodell (Exklusiv)**
- Das System kennt genau zwei Rollen: **OWNER** und **MEMBER**
- Ein User ist systemweit entweder OWNER oder MEMBER — nicht beides gleichzeitig
- Jeder neu registrierte User erhält automatisch die Rolle OWNER

**FR-13.2 — Owner-Berechtigungen (vollständiger Zugriff)**
- Räume: erstellen, umbenennen, löschen
- Geräte: hinzufügen, umbenennen, löschen, Zustand ändern
- Regeln: erstellen, bearbeiten, löschen
- Zeitpläne: erstellen, bearbeiten, löschen
- Mitgliederverwaltung: Mitglieder einladen und entfernen

**FR-13.3 — Member-Berechtigungen (nur Steuerung)**
- Räume und Geräte des Owners: **ansehen und Gerätezustand ändern** (ein/aus)
- Member darf **nicht**: Räume erstellen/umbenennen/löschen, Geräte hinzufügen/umbenennen/löschen, Regeln erstellen/bearbeiten/löschen, Zeitpläne erstellen/bearbeiten/löschen
- Member hat **keinen Zugang** zu seiner eigenen Home-Verwaltung (da exklusives Rollenmodell)

**FR-13.4 — Member-Sicht**
- Ein eingeladenes Mitglied sieht **alle** Räume und Geräte des Owners (vollständige Home-Sicht)
- Die Verwaltungs-Buttons (Erstellen, Bearbeiten, Löschen) sind in der UI für Members ausgeblendet

**FR-13.5 — Rollenauflösung**
- Die Rolle wird bei jeder authentifizierten Anfrage aus der Datenbank gelesen (nicht im JWT gespeichert)
- Damit wird ein entzogener Zugang sofort wirksam — ohne Token-Refresh

---

### FR-20: Mitgliederverwaltung durch den Owner

**FR-20.1 — Einladung**
- Der Owner kann ein Mitglied per E-Mail-Adresse einladen
- Voraussetzung: Die eingeladene E-Mail-Adresse muss bereits ein registriertes Konto haben
- Ist die E-Mail nicht registriert: Fehlermeldung "Diese E-Mail-Adresse ist nicht registriert"
- Die Mitgliedschaft wird **sofort aktiv** — kein Akzeptanz-Workflow benötigt
- Es werden **keine E-Mails versendet** — die Einladung erfolgt rein über die App-UI

**FR-20.2 — Einladungseinschränkungen**
- Ein Owner kann sich nicht selbst einladen
- Eine E-Mail-Adresse, die bereits Mitglied ist, kann nicht erneut eingeladen werden
- Ein User, der bereits Owner eines anderen Homes ist, kann nicht als Member eingeladen werden (exklusives Rollenmodell)

**FR-20.3 — Mitglieder-Liste**
- Der Owner sieht eine Liste aller aktuellen Mitglieder (Name + E-Mail)

**FR-20.4 — Zugang widerrufen**
- Der Owner kann den Zugang eines Mitglieds jederzeit widerrufen
- Nach dem Widerruf: Die Rolle des Users ändert sich wieder zu OWNER (eigenes leeres Home) oder bleibt MEMBER falls er in einem anderen Home ist — im exklusiven Modell: zurück zu OWNER
- Der Widerruf wirkt sofort (DB-basierte Rollenauflösung, FR-13.5)

**FR-20.5 — Mitgliederverwaltungs-UI**
- Eigene Einstellungsseite / Settings-Panel für den Owner
- Enthält: Liste aller Mitglieder, Einladungsformular (E-Mail-Eingabe), Entfernen-Schaltfläche pro Mitglied

---

### FR-Activity-Log-Erweiterung (abgeleitet aus Q9)

**FR-LOG.1**
- Wenn ein Member ein Gerät steuert, wird die Aktion im Activity Log des Owners protokolliert
- Der Akteur (Member-Name) wird im Activity-Log-Eintrag gespeichert

---

## Non-Functional Requirements (Ergänzungen)

**NFR-SEC-01** — Autorisierung serverseitig erzwingen
- Alle OWNER-only Endpunkte müssen serverseitig auf Rolle OWNER prüfen (nicht nur im Frontend verstecken)
- HTTP 403 Forbidden bei unberechtigtem Zugriff eines Members

**NFR-SEC-02** — Mitglied darf nur auf den Home-Kontext seines Owners zugreifen
- Ein Member kann keine Räume/Geräte anderer Owner sehen oder steuern

---

## Data Model (konzeptuell)

### Neue Tabelle: `home_members`

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | BIGSERIAL PK | Primärschlüssel |
| `owner_id` | BIGINT FK → users | Der einladende Owner |
| `member_id` | BIGINT FK → users UNIQUE | Das eingeladene Mitglied (UNIQUE: jeder User kann nur in einem Home Member sein) |
| `joined_at` | TIMESTAMP | Zeitpunkt der Einladung |

**Constraints:**
- `UNIQUE(member_id)` — exklusives Rollenmodell
- `owner_id ≠ member_id` — kein Selbst-Einladen
- `CHECK`: member_id darf nicht selbst owner_id in einer anderen Zeile sein (Anwendungslogik)

---

## API-Endpunkte (konzeptuell)

| Method | Path | Rolle | Beschreibung |
|--------|------|-------|--------------|
| `POST` | `/api/members/invite` | OWNER | Mitglied per E-Mail einladen |
| `GET` | `/api/members` | OWNER | Alle Mitglieder auflisten |
| `DELETE` | `/api/members/{memberId}` | OWNER | Mitglied entfernen |

---

## Betroffene Komponenten

| Schicht | Komponente | Änderungstyp |
|---------|-----------|--------------|
| DB | V9 Flyway Migration | Neu: `home_members` Tabelle |
| Domain | `HomeMember` Entity | Neu |
| Repository | `HomeMemberRepository` | Neu |
| Service | `MemberService` | Neu |
| Controller | `MemberController` | Neu |
| Security | `SecurityConfig` / Auth-Logik | Angepasst: Rollenkontext |
| Service | `DeviceService`, `RoomService`, `RuleService`, `ScheduleService` | Angepasst: Owner-Kontext für Member |
| Domain | `ActivityLog` | Angepasst: Akteur-Feld für Members |
| Frontend | `MemberSettingsComponent` | Neu |
| Frontend | `AuthService` | Angepasst: Rolle aus API-Response |
| Frontend | Bestehende Components | Angepasst: rollenbasierte UI-Sichtbarkeit |

---

## Abnahmekriterien

| ID | Kriterium |
|----|-----------|
| AC-1 | Owner kann eine registrierte E-Mail einladen → Member hat sofort Zugriff auf alle Räume/Geräte des Owners |
| AC-2 | Nicht-registrierte E-Mail einladen → HTTP 404 + Fehlermeldung |
| AC-3 | Member kann Gerätezustand ändern (PATCH /state) → HTTP 200 |
| AC-4 | Member versucht Raum zu löschen (DELETE /rooms) → HTTP 403 |
| AC-5 | Member versucht Gerät hinzuzufügen → HTTP 403 |
| AC-6 | Owner entzieht Member-Zugang → Member-Token liefert beim nächsten Request HTTP 403 |
| AC-7 | Member-Aktion im Gerätezustand erscheint im Activity Log mit Member-Name |
| AC-8 | Owner sieht Liste aller Mitglieder auf der Settings-Seite |
| AC-9 | Ein User mit bestehender Owner-Rolle kann nicht als Member eingeladen werden (exklusiv) |
