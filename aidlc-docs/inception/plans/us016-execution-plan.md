# Execution Plan - US-016: Energy Consumption Dashboard

## Detailed Analysis Summary

### Transformation Scope
- **Transformation Type**: Full-stack feature completion in an existing route
- **Primary Changes**: Backend energy endpoint, backend consumption estimates, frontend API integration, room aggregation logic
- **Related Components**: `EnergyController`, `EnergyService`, `EnergyDeviceResponse`, Angular `EnergyService`, `EnergyComponent`, `EnergyDevice`, global dashboard styles

### Change Impact Assessment
- **User-facing changes**: Yes - Energy page now presents household total, room aggregation, device usage and day/week period switching.
- **Structural changes**: Minimal - one new authenticated backend endpoint, no database migration.
- **Data model changes**: New backend response DTO; no persistence changes.
- **API changes**: New `GET /api/energy/devices` endpoint.
- **NFR impact**: Low - simple client-side aggregation over a small list of devices.

### Risk Assessment
- **Risk Level**: Low
- **Rollback Complexity**: Easy - no DB changes; remove endpoint and frontend service integration.
- **Testing Complexity**: Moderate - backend service/controller tests plus Angular build.

## Phases to Execute

### Inception Phase
- [x] Workspace Detection - reuse existing brownfield setup
- [x] Reverse Engineering - reviewed existing energy page, routes, models and styling
- [x] Requirements Analysis - US-016 acceptance criteria mapped to Energy page capabilities
- [ ] User Stories - skip; provided GitHub issue already contains the story
- [x] Workflow Planning - this plan
- [ ] Application Design - skip; existing route and simple service/controller pattern are sufficient
- [x] Units Generation - backend and frontend units

### Construction Phase
- [x] Functional Design - backend estimates device consumption; frontend aggregates rooms and household totals
- [ ] NFR Requirements - skip; existing frontend NFRs apply
- [ ] NFR Design - skip
- [ ] Infrastructure Design - skip
- [x] Code Generation - backend API and frontend dashboard integration
- [x] Build and Test - backend tests, PMD and Angular build succeed; existing initial bundle budget warning remains

## Unit of Work

### Unit 1 - Backend Energy API
**Scope**
- Add `GET /api/energy/devices`.
- Resolve the effective household owner through existing member logic.
- Estimate device-level wattage and day/week kWh from existing device type and state.
- Cover service and controller behavior with tests.

### Unit 2 - Frontend Energy Dashboard
**Scope**
- Load device energy data from the backend.
- Calculate total household consumption from returned device data.
- Aggregate consumption per room from returned device data.
- Apply the selected day/week period to summary cards, room chart and device table.
- Keep the existing `/energy` route and shell navigation.

## Acceptance Criteria Mapping
- **Verbrauch pro Gerät dargestellt**: Backend returns device-level estimates; device table shows wattage and selected-period kWh per device.
- **Verbrauch pro Raum aggregiert dargestellt**: Room chart aggregates device values by room.
- **Gesamtverbrauch des Haushalts dargestellt**: Summary card calculates total from all devices.
- **Tages- und Wochenansicht verfügbar**: Period toggle switches day/week across cards, room chart and device table.

## Quality Gate
- Backend tests and Angular production build succeed.
