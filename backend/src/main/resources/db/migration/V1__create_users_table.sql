-- V1: Create users table
-- US-001: Registrierung mit gültiger E-Mail und Passwort möglich
--         Doppelte E-Mail-Adressen werden abgelehnt (UNIQUE constraint)
--         Passwort wird mit bcrypt gehasht gespeichert (password_hash column)

CREATE TABLE users
(
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);
