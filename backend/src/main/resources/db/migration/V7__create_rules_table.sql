CREATE TABLE rules (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL,
    trigger_type            VARCHAR(20)  NOT NULL,
    trigger_device_id       BIGINT       NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    trigger_operator        VARCHAR(5),
    trigger_threshold_value DOUBLE PRECISION,
    action_device_id        BIGINT       NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    action_value            VARCHAR(50)  NOT NULL,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    user_id                 BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
