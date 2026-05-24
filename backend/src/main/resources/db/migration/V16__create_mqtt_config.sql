CREATE TABLE mqtt_configs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    broker_url  VARCHAR(512) NOT NULL DEFAULT '',
    topic       VARCHAR(255) NOT NULL DEFAULT 'smarthome',
    connected   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
