-- V8: Add TIME trigger support to rules table (US-012)
-- Makes trigger_device_id nullable for TIME rules (no device triggers them).
-- Adds hour, minute and days-of-week columns used by RuleScheduler.

ALTER TABLE rules
    ALTER COLUMN trigger_device_id DROP NOT NULL;

ALTER TABLE rules
    ADD COLUMN trigger_hour         INTEGER,
    ADD COLUMN trigger_minute       INTEGER,
    ADD COLUMN trigger_days_of_week VARCHAR(100);
