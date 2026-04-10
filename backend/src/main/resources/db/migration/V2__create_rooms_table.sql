-- V2: Create rooms table
-- US-004: Raum mit Name erstellen / umbenennen / löschen möglich

CREATE TABLE rooms
(
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(30)  NOT NULL,
    icon       VARCHAR(50)  NOT NULL DEFAULT 'weekend',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rooms_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_rooms_user_id ON rooms (user_id);
