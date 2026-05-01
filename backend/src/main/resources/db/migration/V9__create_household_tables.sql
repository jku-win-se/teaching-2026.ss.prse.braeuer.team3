-- V9: Create household, household_member and invitation tables for US-015 RBAC

CREATE TABLE household (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE household_member (
    id           BIGSERIAL   PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL,
    joined_at    TIMESTAMP   NOT NULL,
    UNIQUE (user_id)
);

CREATE TABLE invitation (
    id             BIGSERIAL    PRIMARY KEY,
    household_id   BIGINT       NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    invited_email  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL
);
