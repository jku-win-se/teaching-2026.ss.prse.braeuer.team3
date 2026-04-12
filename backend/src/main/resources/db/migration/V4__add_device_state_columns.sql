ALTER TABLE devices
    ADD COLUMN state_on       BOOLEAN          NOT NULL DEFAULT FALSE,
    ADD COLUMN brightness     INT              NOT NULL DEFAULT 50,
    ADD COLUMN temperature    DOUBLE PRECISION NOT NULL DEFAULT 21.0,
    ADD COLUMN sensor_value   DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN cover_position INT              NOT NULL DEFAULT 0;
