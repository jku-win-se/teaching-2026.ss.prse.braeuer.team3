# Unit of Work Plan — FR-07: Echtzeit-Zustandsanzeige

**Datum**: 2026-04-12  
**Basis**: application-design.md — alle Komponentengrenzen bereits festgelegt  
**Keine offenen Fragen** — Scope, Reihenfolge und Grenzen sind eindeutig.

## Artefakte zu erstellen

- [x] `unit-of-work.md` — Unit-Definitionen und Verantwortlichkeiten
- [x] `unit-of-work-dependency.md` — Abhängigkeitsmatrix zwischen Units
- [x] `unit-of-work-story-map.md` — Story-zu-Unit-Mapping

## Gewählte Zerlegung

**2 Units**, sequentiell (Backend zuerst, dann Frontend):

| Unit | Name | Schicht |
|------|------|---------|
| Unit 1 | `sse-backend` | Spring Boot — neue + modifizierte Klassen |
| Unit 2 | `sse-frontend` | Angular — neuer Service + modifizierte Komponenten |

**Begründung**: Frontend-Unit benötigt lauffähigen SSE-Endpunkt → sequentielle Reihenfolge erzwungen.
