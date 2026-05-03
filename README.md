![Coverage](.github/badges/jacoco.svg)

# SmartHome Orchestrator

A home automation platform for managing smart devices, automation rules, schedules, and energy monitoring — developed as part of the Software Engineering Praktikum (SS 2026) at JKU Linz.

**Tech Stack:** Java 21 · Spring Boot 3 · PostgreSQL (Docker) · Angular

> **New here?** Start with the [Developer Setup Guide](./SETUP.md).

# Umgesetzte Anforderungen

| ID | Beschreibung | Status |
|----|-------------|--------|
| FR-01 | Registrierung mit einzigartiger E-Mail-Adresse | ✅ Umgesetzt |
| FR-02 | Login / Logout (JWT-basiert) | ✅ Umgesetzt |
| FR-03 | Räume erstellen, umbenennen, löschen | ✅ Umgesetzt |
| FR-04 | Virtuelle Geräte zu Räumen hinzufügen (Switch, Dimmer, Thermostat, Sensor, Cover) | ✅ Umgesetzt |
| FR-05 | Geräte entfernen und umbenennen | ✅ Umgesetzt |
| FR-06 | Gerätekontrolle (Toggle, Helligkeit, Temperatur, Sensorwert, Jalousie) | ✅ Umgesetzt |
| FR-07 | Echtzeit-Zustandsanzeige via Server-Sent Events (SSE) | ✅ Umgesetzt |
| FR-08 | Aktivitätslog (jede Zustandsänderung mit Zeitstempel und Actor) | ✅ Umgesetzt |
| FR-09 | Zeitbasierte Zeitpläne (Schedules, täglich/wöchentlich) | ✅ Umgesetzt |
| FR-10 | Rule Engine (IF-THEN-Regeln) | ✅ Umgesetzt |
| FR-11 | Mindestens 3 Trigger-Typen: TIME, THRESHOLD, EVENT | ✅ Umgesetzt |
| FR-12 | In-App-Benachrichtigungen bei Regelausführung | ✅ Umgesetzt |
| FR-13 | Zwei Benutzerrollen: Owner (Vollzugriff) und Member (nur Steuerung) | ✅ Umgesetzt |
| FR-14 | Energie-Dashboard (pro Gerät, Raum, Haushalt; täglich/wöchentlich) | ✅ Umgesetzt |
| FR-15 | Konflikterkennung bei widersprüchlichen Regeln | ✅ Umgesetzt |
| FR-16 | CSV-Export (Aktivitätslog + Energiezusammenfassung) | ✅ Umgesetzt |
| FR-17 | Szenen (benannte Gerätezustandsgruppen, einmalige Aktivierung) | ✅ Umgesetzt |
| FR-18 | Optionale IoT-Integration (MQTT) | ❌ Nicht umgesetzt (out of scope) |
| FR-19 | Tages-Simulation (Zeitraffer) | ❌ Nicht umgesetzt (out of scope) |
| FR-20 | Owner kann Mitglieder per E-Mail einladen und Zugang entziehen | ✅ Umgesetzt |
| FR-21 | Urlaubsmodus (Zeitplan-Override für Datumsbereich) | ❌ Nicht umgesetzt (out of scope) |

# Überblick über die Applikation aus Benutzersicht

[Link zu Benutzerdokumentation](./docs/user-handbook.md)

* Installations- und Startanleitung
* Überblick über die Funktionen der Applikation (z.B. Hauptbildschirm, Unterseiten, Navigation)
* Beschreibung der Funktionalität anhand von Szenarien (z.B. "Wie kann ein Benutzer eine neue Regel erstellen?")
* Bekannte Einschränkungen (z.B. "Die Anwendung unterstützt derzeit nur die Steuerung von bis zu 5 Geräten")

# Überblick über die Applikation aus Entwicklersicht

[Link zu Systemarchitektur-Dokumentation](./docs/system-architecture.md)

* Überblick über die Architektur (z.B. Schichten, Module, wichtige Klassen)
* Beschreibung der wichtigsten Designentscheidungen (z.B. "Warum haben wir uns für eine monolithische Architektur entschieden?")
* Hinweise zu Erweiterungspunkten
* Build und Qualitätssicherung (z.B. verwendete Tools, Testabdeckung, Codequalität)
* Testfallbeschreibung und Testabdeckung (z.B. "Die wichtigsten Testfälle decken die Kernfunktionalität ab, die aktuelle Testabdeckung liegt bei 80 % ohne UI-Klassen")

# JavaDoc für wichtige Klassen, Interfaces und Methoden

[Links zu JavaDoc-Seiten](./docs/javadoc/index.html)
