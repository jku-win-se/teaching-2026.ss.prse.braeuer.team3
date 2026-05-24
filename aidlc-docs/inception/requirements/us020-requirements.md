# Requirements — US-020: Day Simulation

## Intent Analysis
- **User Request**: Als Benutzer möchte ich einen vollständigen Tag simulieren können, indem ich Bedingungen festlege und die resultierenden Gerätezustandsänderungen im Zeitraffer sehe, damit ich meine Automatisierungen testen kann.
- **Request Type**: New Feature
- **Scope Estimate**: Multiple Components (Backend service + controller + DTOs + tests; Frontend component + service + route)
- **Complexity Estimate**: Moderate

## Functional Requirements

### FR-SIM-01: Start Conditions
The user must be able to define initial device states before starting the simulation. For each device in their home, they can set: stateOn, brightness, temperature, sensorValue, coverPosition.

### FR-SIM-02: Day-of-Week Selection
The user must be able to select which day of the week to simulate (MONDAY–SUNDAY). This determines which TIME rules fire (their `triggerDaysOfWeek` must contain the selected day).

### FR-SIM-03: Simulation Execution (In-Memory)
The backend runs the full 24-hour simulation in-memory (00:00–23:59), minute by minute:
- For each minute, evaluate all enabled TIME rules that match the selected day and the current simulated hour/minute.
- When a TIME rule fires and changes a device's state, evaluate THRESHOLD and EVENT rules whose trigger device was just changed — potentially causing cascade effects.
- All state manipulation is purely in-memory; the real device states in the database are **never modified**.

### FR-SIM-04: Result — Event Timeline
The simulation returns an ordered list of `SimulationEvent` objects, each recording: simulated time (hour:minute), the device affected, the action applied, and which rule triggered it.

### FR-SIM-05: Live System Isolation
The simulation must be fully isolated from the live system. No device states, activity log entries, or WebSocket broadcasts must be produced during simulation.

## Non-Functional Requirements
- **NFR-04 (PMD)**: All Java code must be PMD-clean (no critical/high violations).
- **NFR-06 (Javadoc)**: All public classes and methods in service/controller layer must have Javadoc.
- **Performance**: Simulation of 1440 minutes with typical rule counts (≤100 rules) must complete in under 2 seconds.

## Acceptance Criteria
1. Simulation startet mit benutzerdefinierten Startbedingungen ✓
2. Zustandsänderungen werden im Zeitraffer wiedergegeben ✓
3. Simulation beeinflusst das Live-System nicht ✓
