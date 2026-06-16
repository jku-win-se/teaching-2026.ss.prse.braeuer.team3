# Präsentationsplan — SmartHome Orchestrator (10 min)

## Ziel

Zwei Zielgruppen gleichzeitig überzeugen:
- **Anwender**: "Das macht mein Leben einfacher"
- **Entwickler**: "Das kann ich gut weiterentwickeln"

---

## Block 1 — Hook (30 Sek.)

**"Warum brauche ich das?"**

Ein Satz, der alles erklärt: *"Stell dir vor, dein Haus denkt mit — Lichter gehen von selbst aus wenn du in Urlaub fährst, Geräte reagieren auf Regeln die du selbst definierst, und du siehst live wie viel Energie du verbrauchst."*

Kein Slide, kein Login zeigen — direkt in die App springen, die schon eingeloggt ist.

---

## Block 2 — Live Demo (4 Min.)

**Die 4 Wow-Momente — alle live in der UI**

| # | Feature | Warum interessant |
|---|---------|------------------|
| 1 | **Device Control** | Gerät schalten → Status ändert sich live via WebSocket/MQTT |
| 2 | **Automation Rules** | If-Then-Regel zeigen: "Wenn Temperatur > 25° → Klimaanlage an" |
| 3 | **Vacation Mode** | Ein Klick → ganze Logik läuft automatisch |
| 4 | **Energy Dashboard** | Visuell: Wer/was verbraucht wieviel |

> **Tipp:** App vorher starten, Demo-Daten vorbereiten — keine Zeit für Ladezeiten verschwenden.

---

## Block 3 — Dokumentation (1.5 Min.)

**Für den Anwender + Entwickler**

- **User Handbook** kurz aufblättern: vollständige deutsche Doku, 15 Kapitel, direkt verwendbar
- **Architecture Docs** zeigen: UML + System-Architektur-Overview → für Entwickler sofort verständlich
- **Javadoc**: 1 Klasse aufmachen → jede public Methode dokumentiert

---

## Block 4 — Code-Qualität (1.5 Min.)

**Für den Entwickler**

- **PMD**: `ruleset.xml` zeigen, CI schlägt bei Violations fehl
- **CI/CD Pipeline**: `Continuous Integration.yaml` — automatisch bei jedem Commit
- **Clean Architecture**: kurz die Schichten zeigen (`controller/`, `service/`, `domain/`) — jemand der einsteigt, weiß sofort wo was hingehört

---

## Block 5 — Tests (1.5 Min.)

**Für Vertrauen bei beiden Zielgruppen**

- **33 Backend JUnit Tests** (Controller + Service Layer) — Screenshot oder kurz Maven Output zeigen
- **Playwright E2E Tests** live ausführen oder Report zeigen: `device-control.spec.ts`, `rules.spec.ts` → testet echte User-Flows durch die UI

---

## Block 6 — Abschluss (30 Sek.)

**"Reif für den Einsatz — und leicht erweiterbar"**

- Als Anwender: läuft per Docker, User Handbook liegt bereit
- Als Entwickler: saubere Architektur, dokumentiert, getestet, CI schützt vor Regressionen

---

## Zeitplan auf einen Blick

```
0:00 – 0:30   Hook
0:30 – 4:30   Live Demo (4 Features)
4:30 – 6:00   Dokumentation
6:00 – 7:30   Code-Qualität
7:30 – 9:00   Tests
9:00 – 9:30   Abschluss
              Puffer: 30 Sek.
```
