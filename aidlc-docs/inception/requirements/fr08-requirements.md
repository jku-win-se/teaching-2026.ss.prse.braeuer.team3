# Requirements — FR-08 Aktivitätsprotokoll

## Intent Analysis
- **Request Type**: New Feature
- **Scope**: Multiple components — backend entity, repository, service, controller, DB migration, frontend component
- **Complexity**: Moderate

## Functional Requirements

| ID | Requirement |
|----|-------------|
| FR-08-1 | Every manual device state change is logged with timestamp, device name, room name, and actor name |
| FR-08-2 | Actor name = user's display name for manual changes; "System — {rule name}" for automated (future) |
| FR-08-3 | For this sprint: only manual state changes are logged (FR-09/10/11 not yet implemented) |
| FR-08-4 | Log is global per user — all devices across all rooms in one view |
| FR-08-5 | Log is filterable by date range (from/to) AND by specific device (dropdown) |
| FR-08-6 | Log is paginated (configurable page size, default 20) |
| FR-08-7 | New log entries appear in real-time in the UI via WebSocket |
| FR-08-8 | Log entries are kept until manually deleted by the owner |
| FR-08-9 | User can delete individual log entries |

## Non-Functional Requirements

- **NFR-03**: Test coverage ≥ 75% on new backend classes
- **NFR-04**: No critical/high PMD violations
- **NFR-06**: Javadoc on all public classes and methods in domain/service/repository/controller layers

## Data Model

### ActivityLog Entity
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| timestamp | Instant | When the change occurred |
| device | Device (ManyToOne) | The affected device |
| user | User (ManyToOne) | The owner (for scoping) |
| actorName | String | Display name of actor |
| action | String | Human-readable description of the change |

## API
- `GET /api/activity-log?page=0&size=20&from=...&to=...&deviceId=...` — paginated, filtered log
- `DELETE /api/activity-log/{id}` — delete a specific entry (owner only)

## Frontend
- New `ActivityLogComponent` accessible from main navigation
- Date range pickers (from/to) + device dropdown filter
- Paginator (20 entries/page default)
- Real-time prepend of new entries via WebSocket
