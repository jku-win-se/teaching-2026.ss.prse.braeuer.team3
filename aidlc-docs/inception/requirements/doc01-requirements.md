# DOC-01: Requirements Document
# Projektdokumentation (technisch + benutzerseitig) + Aktualisierte Architekturdokumentation

**Datum**: 2026-05-03  
**Request Type**: Dokumentationserstellung (neue Deliverables)  
**Scope**: System-wide (alle implementierten FRs)  
**Complexity**: Moderate

> Annahmen (Fragen nicht beantwortet, Defaults verwendet):
> - Sprache: Englisch für technische Doku, Deutsch für Benutzerhandbuch
> - Format: Markdown-Dateien im Repository unter `docs/`
> - Zielgruppe: Bewertungskommission (Professor/Tutor) + Entwickler
> - Architekturdoku: Diagramm + Komponentenbeschreibungen + Key Flows
> - Basis: bestehende README.md / SETUP.md / ARCHITECTURE_UML.md

---

## Functional Requirements

| ID | Beschreibung |
|----|-------------|
| DOC-F1 | `docs/system-architecture.md` erstellen — technische Dokumentation für Entwickler |
| DOC-F2 | `docs/user-handbook.md` erstellen — Benutzerhandbuch auf Deutsch, alle FRs abdeckend |
| DOC-F3 | `ARCHITECTURE_UML.md` aktualisieren — alle aktuellen Controller/Services/Flows einpflegen |
| DOC-F4 | `README.md` Abschnitt "Umgesetzte Anforderungen" ausfüllen |
| DOC-F5 | `SETUP.md` Frontend-Abschnitt korrigieren (Prototype → `frontend/`) |

## Non-Functional Requirements

| ID | Beschreibung |
|----|-------------|
| DOC-NF1 | Alle verlinkten FRs (FR-01 bis FR-16, FR-20) müssen in der Doku erwähnt sein |
| DOC-NF2 | Technische Doku muss Architekturentscheidungen begründen (nicht nur beschreiben) |
| DOC-NF3 | Benutzerhandbuch muss ohne Vorkenntnisse verständlich sein |

## Scope der implementierten Features (Stand 2026-05-03)

Vollständig implementiert:
- FR-01/02: Auth (Register, Login, JWT)
- FR-03/04/05: Räume & Geräte (CRUD)
- FR-06/07: Gerätekontrolle + Echtzeit-State via SSE
- FR-08: Activity Log
- FR-09: Schedules (zeitbasierte Aktionen)
- FR-10/11: Rule Engine (Zeit-, Schwellwert-, State-Trigger)
- FR-12: In-App-Benachrichtigungen (Rule Execution)
- FR-13/20: Rollen (Owner/Member) + Mitglieder einladen/entziehen
- FR-14: Energie-Dashboard (pro Gerät/Raum/Haushalt, täglich/wöchentlich)
- FR-15: Konflikterkennung (US-014)
- FR-16: CSV-Export (Activity Log + Energie)
- FR-17: Szenen (named device state groups)
