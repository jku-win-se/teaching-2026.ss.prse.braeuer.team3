CREATE TABLE vacation_modes (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id),
    schedule_id BIGINT    NOT NULL REFERENCES schedules(id),
    name        VARCHAR(100) NOT NULL,
    start_date  DATE      NOT NULL,
    end_date    DATE      NOT NULL,
    deactivated BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
