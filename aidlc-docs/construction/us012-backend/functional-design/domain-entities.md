# Domain Entities — US-012 Backend (Unit 1)

## TriggerType (Enum) — Änderungen

```
THRESHOLD  (bestehend)
EVENT      (bestehend)
TIME       (NEU)
```

---

## Rule (Entity) — Änderungen

| Feld                  | Typ         | Nullable | Neu? | Beschreibung                                            |
|-----------------------|-------------|----------|------|---------------------------------------------------------|
| `triggerDevice`       | Device (FK) | **JA**   | -    | null für TIME-Regeln (bisher required)                 |
| `triggerHour`         | Integer     | JA       | NEU  | Stunde 0–23; nur für TIME-Regeln                        |
| `triggerMinute`       | Integer     | JA       | NEU  | Minute 0–59; nur für TIME-Regeln                        |
| `triggerDaysOfWeek`   | String      | JA       | NEU  | Kommasepariert, z.B. `"MONDAY,FRIDAY"` — nur für TIME  |

Alle übrigen Felder unverändert.

**Invarianten**:
- `triggerType == TIME` → `triggerHour` ≠ null, `triggerMinute` ≠ null, `triggerDaysOfWeek` ≠ null, `triggerDevice` == null
- `triggerType == THRESHOLD` → `triggerDevice` ≠ null, `triggerOperator` ≠ null, `triggerThresholdValue` ≠ null
- `triggerType == EVENT` → `triggerDevice` ≠ null

---

## RuleRequest (DTO) — Änderungen

| Feld                | Typ     | Pflicht      | Beschreibung                                   |
|---------------------|---------|--------------|------------------------------------------------|
| `triggerDeviceId`   | Long    | Nein (TIME)  | null erlaubt wenn triggerType == TIME          |
| `triggerHour`       | Integer | Nein         | Pflicht wenn triggerType == TIME               |
| `triggerMinute`     | Integer | Nein         | Pflicht wenn triggerType == TIME               |
| `triggerDaysOfWeek` | String  | Nein         | Pflicht wenn triggerType == TIME (CSV)         |

---

## RuleResponse (DTO) — Änderungen

| Feld                  | Typ     | Beschreibung                          |
|-----------------------|---------|---------------------------------------|
| `triggerDeviceId`     | Long    | null für TIME-Regeln                  |
| `triggerDeviceName`   | String  | null für TIME-Regeln                  |
| `triggerHour`         | Integer | null für THRESHOLD/EVENT-Regeln       |
| `triggerMinute`       | Integer | null für THRESHOLD/EVENT-Regeln       |
| `triggerDaysOfWeek`   | String  | null für THRESHOLD/EVENT-Regeln       |

---

## RuleScheduler (neue Klasse)

- Spring `@Component`, `@EnableScheduling` muss in Application-Klasse aktiv sein (bereits durch FR-09 vorhanden)
- Methode `runDueTimeRules()`: `@Scheduled(cron = "0 * * * * *")`, `@Transactional`
- Analog zu `ScheduleService.runDueSchedules()`

---

## Flyway Migration V8

```sql
ALTER TABLE rules
    ALTER COLUMN trigger_device_id DROP NOT NULL;

ALTER TABLE rules
    ADD COLUMN trigger_hour        INTEGER,
    ADD COLUMN trigger_minute      INTEGER,
    ADD COLUMN trigger_days_of_week VARCHAR(100);
```
