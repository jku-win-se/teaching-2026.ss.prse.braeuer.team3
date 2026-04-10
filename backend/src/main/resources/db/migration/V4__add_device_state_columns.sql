ALTER TABLE devices
    ADD COLUMN state_on       BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN brightness     INT          NOT NULL DEFAULT 50,
    ADD COLUMN temperature    NUMERIC(4,1) NOT NULL DEFAULT 21.0,
    ADD COLUMN sensor_value   NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN cover_position INT          NOT NULL DEFAULT 0;
