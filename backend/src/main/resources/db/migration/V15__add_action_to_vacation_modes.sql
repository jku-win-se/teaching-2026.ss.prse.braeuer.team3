ALTER TABLE vacation_modes
    ADD COLUMN action          VARCHAR(10) NOT NULL DEFAULT 'ENABLE',
    ADD COLUMN original_enabled BOOLEAN;
