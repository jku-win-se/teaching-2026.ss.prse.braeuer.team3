CREATE TABLE activity_logs (
    id          BIGSERIAL PRIMARY KEY,
    timestamp   TIMESTAMPTZ NOT NULL DEFAULT now(),
    device_id   BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_name  VARCHAR(255) NOT NULL,
    action      VARCHAR(500) NOT NULL
);

CREATE INDEX idx_activity_logs_user_id   ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_device_id ON activity_logs(device_id);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp DESC);
