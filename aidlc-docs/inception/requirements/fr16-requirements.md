# Requirements — FR-16: CSV Export

## Intent Analysis
- **User Request**: Implement FR-16 — CSV export for activity log and energy usage summary
- **Request Type**: New Feature
- **Scope Estimate**: Multiple Components (Backend new endpoints; Frontend download trigger)
- **Complexity Estimate**: Simple

---

## Functional Requirements

### FR-16.1 — Activity Log Export
- `GET /api/activity-log/export` returns the full activity log for the authenticated user as a CSV file.
- The response uses `Content-Type: text/csv` and `Content-Disposition: attachment; filename="activity-log.csv"`.
- **Owner-only** (consistent with FR-13): Members receive 403 Forbidden.
- CSV columns: `Timestamp,Device,Room,Actor,Action`
- Rows are ordered by timestamp ascending (chronological), matching the existing paginated view.
- All entries are exported (no pagination limit).

### FR-16.2 — Energy Usage Export
- `GET /api/energy/export` returns the energy usage summary as a CSV file.
- The response uses `Content-Type: text/csv` and `Content-Disposition: attachment; filename="energy-summary.csv"`.
- **Owner + Member**: Energy dashboard is read-accessible to all authenticated users.
- CSV columns: `Device,Room,Wattage (W),Today (kWh),Week (kWh)`
- Rows are ordered by room name, then device name (matching the existing dashboard order).

### FR-16.3 — Frontend Download Trigger (Activity Log)
- The Activity Log page (`/log`) has an "Export CSV" button.
- Clicking it performs a blob download via the browser (no navigation away from the page).

### FR-16.4 — Frontend Download Trigger (Energy Dashboard)
- The Energy Dashboard page (`/energy`) already has an "Export CSV" button stub.
- The stub is wired up to trigger a real blob download.

---

## Non-functional Requirements

| Requirement | Detail |
|---|---|
| NFR-04 (PMD) | No critical/high violations in generated Java code |
| NFR-06 (Javadoc) | Full Javadoc on all new public classes and methods |
| NFR-03 (Coverage) | New service methods covered by unit tests |

---

## Technical Notes
- No new DB migration or entity changes needed.
- `ActivityLogRepository` needs a non-paginated `findAllByUser` query for full export.
- Frontend: use Angular `HttpClient` with `responseType: 'blob'` and programmatic anchor click to trigger browser download.
- CSV escaping: wrap field values that contain commas or double-quotes in double-quotes; escape inner double-quotes by doubling them.
