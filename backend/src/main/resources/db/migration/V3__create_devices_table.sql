-- V3: Create devices table
-- FR-04: Gerät hinzufügen (type, name, room)

CREATE OR REPLACE TABLE devices
(
    id         BIGSERIAL   PRIMARY KEY,
    room_id    BIGINT      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    name       VARCHAR(50) NOT NULL,
    type       VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_devices_room_name UNIQUE (room_id, name)
);

CREATE INDEX idx_devices_room_id ON devices (room_id);
