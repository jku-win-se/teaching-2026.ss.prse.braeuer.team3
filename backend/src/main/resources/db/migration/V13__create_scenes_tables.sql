-- V13: Scenes (US-018) — named groups of device states
CREATE TABLE scenes (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    icon    VARCHAR(50)  NOT NULL DEFAULT 'auto_awesome',
    user_id BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- One row per device action inside a scene
CREATE TABLE scene_entries (
    id           BIGSERIAL    PRIMARY KEY,
    scene_id     BIGINT       NOT NULL REFERENCES scenes(id)  ON DELETE CASCADE,
    device_id    BIGINT       NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    action_value VARCHAR(50)  NOT NULL
);
