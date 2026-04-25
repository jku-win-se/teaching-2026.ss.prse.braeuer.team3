CREATE TABLE schedules (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    device_id      BIGINT       NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    days_of_week   VARCHAR(100) NOT NULL,
    hour           INT          NOT NULL CHECK (hour   BETWEEN 0 AND 23),
    minute         INT          NOT NULL CHECK (minute BETWEEN 0 AND 59),
    action_payload TEXT         NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE
);
