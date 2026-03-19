# Requirements Document: SmartHome Orchestrator

> **📝 Team Task:** Replace *SmartHome Orchestrator* throughout this document with your own project name. Choose a name that reflects your team's identity and the product you are building.

## 1. Introduction

This document specifies the requirements for the **SmartHome Orchestrator**, a home automation application developed as part of the Software Engineering Praktikum (SS 2026) at JKU Linz.

The application allows users to manage smart home devices, define automation rules, schedule actions, and monitor energy usage — all through a unified interface. The system operates on **simulated virtual devices** to keep development environment-independent and accessible; integration with real IoT hardware is treated as an optional extension.

This document is the technical and contractual basis for all sprint planning, acceptance criteria, and release evaluations throughout the semester. Requirements are categorized as either **functional (FR)** or **non-functional (NFR)**.

---

## 2. Motivation

Modern households increasingly rely on a growing ecosystem of smart devices — thermostats, smart lights, motion sensors, locks, and appliances — each typically managed through separate proprietary applications. This fragmentation creates friction: users must switch between multiple apps, cannot define cross-device automation rules, and have no unified view of device state or energy consumption.

The **SmartHome Orchestrator** addresses this problem by providing a single platform that:

- **Centralises device management** across rooms and device types, removing the need for separate per-device apps.
- **Empowers non-expert users** through a clear, visual interface requiring no programming knowledge to define automation rules.
- **Increases energy awareness** by tracking and visualising device usage, supporting more sustainable consumption habits.
- **Reduces manual effort** by allowing users to schedule repetitive actions (e.g., turn off all lights at midnight) and set condition-based triggers (e.g., turn on heating when temperature drops below 18 °C).
- **Provides a safe learning environment** for students to practise professional software engineering: requirements engineering, iterative delivery, architecture design, testing, and CI/CD in a realistic, domain-rich application.

---

## 3. Requirements

### Functional Requirements

| ID    | Description |
|-------|-------------|
| FR-01 | The system shall allow a user to register with a unique e-mail address and a password. |
| FR-02 | The system shall allow a registered user to log in and log out securely. |
| FR-03 | The system shall allow authenticated users to create, rename, and delete rooms (e.g., Living Room, Kitchen). |
| FR-04 | The system shall allow users to add virtual smart devices to a room, specifying type (switch, dimmer, thermostat, sensor, cover/blind) and name. |
| FR-05 | The system shall allow users to remove or rename existing devices. |
| FR-06 | The system shall allow users to control a device manually: toggle switches on/off, set dimmer brightness (0–100 %), set thermostat target temperature, open/close covers, and inject a current value into a sensor (e.g., for testing). |
| FR-07 | The system shall maintain and display the current state of each device in real time within the UI. |
| FR-08 | The system shall record an activity log entry for every manual or automated state change, including timestamp, device, and actor (user or rule). |
| FR-09 | The system shall allow users to configure unconditional time-based schedules for device actions (e.g., turn on lamp at 07:00 every weekday). Unlike rule triggers (FR-11), schedules execute a fixed action on a recurring time pattern without evaluating any additional condition. |
| FR-10 | The system shall provide a rule engine that evaluates condition-action rules of the form: IF \<trigger\> THEN \<action\> (e.g., IF motion sensor active THEN turn on hallway light). |
| FR-11 | The system shall support at least three trigger types for rules: time-based, threshold-based (sensor value), and event-based (device state change). |
| FR-12 | The system shall notify the user (in-app notification) when a rule is executed or when a rule execution fails. |
| FR-13 | The system shall support two user roles: **Owner** (full access) and **Member** (control only, no device/rule management). |
| FR-14 | The system shall display an energy usage dashboard showing estimated power consumption per device, per room, and as a household total, aggregated by day and by week. |
| FR-15 | The system shall detect and warn the user about scheduling conflicts (two rules that would put the same device into contradictory states simultaneously). |
| FR-16 | The system shall allow users to export the activity log and energy usage summary as a CSV file. |
| FR-17 | The system shall provide a "scene" feature: a named group of device states that can be activated with a single action (e.g., "Movie Night" dims lights and closes blinds). |
| FR-18 | The system shall support an optional integration layer for one physical IoT protocol (e.g., MQTT) to connect real devices. |
| FR-19 | The system shall allow users to simulate a full day by specifying conditions (e.g., initial sensor values, start time, active rules) and replaying the resulting device state changes in accelerated time-lapse — without affecting the live system. |
| FR-20 | The system shall allow the Owner to invite additional Members by e-mail address and to revoke their access at any time. |
| FR-21 | The system shall allow users to enable a "vacation mode" that automatically applies a named schedule (defined via FR-09) for a specified date range, overriding normal daily schedules for that period. |

### Non-functional Requirements

| ID     | Category     | Description |
|--------|--------------|-------------|
| NFR-01 | Performance  | The system shall respond to any user interaction (button press, form submission) within **2 seconds** under normal load (up to 10 concurrent simulated devices). |
| NFR-02 | Security     | All user passwords shall be stored using a strong one-way hashing algorithm (e.g., bcrypt); plain-text passwords must never be persisted or logged. |
| NFR-03 | Test         | The test suite shall achieve a minimum **line coverage of 75 %** across all non-UI business logic classes. |
| NFR-04 | Code Quality | The codebase shall contain **no critical or high PMD violations**; compliance is enforced automatically on every CI run and a build with critical or high findings shall fail. |
| NFR-05 | Reliability     | The system shall handle individual device failures and invalid device states gracefully, log the error, and continue operating all unaffected devices without requiring a restart. |
| NFR-06 | Documentation   | All public classes, interfaces, and methods that are part of the core domain or API layer shall have Javadoc comments describing their purpose, parameters, and return values. |

---

## 4. Out of Scope

The following items are explicitly **not** in scope for this project unless stated otherwise in a release plan:

- Native mobile application (iOS / Android).
- Real-time push notifications via SMS or e-mail.
- Integration with commercial smart home platforms (Google Home, Amazon Alexa, Apple HomeKit).
- Multi-household / multi-tenancy support.
- Hardware procurement or physical device installation.
