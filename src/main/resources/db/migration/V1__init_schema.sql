CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    ref        VARCHAR(64)  NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL,
    telephone  VARCHAR(32),
    nom        VARCHAR(128) NOT NULL,
    prenom     VARCHAR(128) NOT NULL,
    role       VARCHAR(16)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'MANAGER'))
);

CREATE TABLE departments (
    id    BIGSERIAL PRIMARY KEY,
    code  VARCHAR(32)  NOT NULL UNIQUE,
    label VARCHAR(128) NOT NULL
);

CREATE TABLE appointments (
    id            BIGSERIAL PRIMARY KEY,
    ref_rdv       VARCHAR(64)  NOT NULL UNIQUE,
    manager_id    BIGINT       NOT NULL REFERENCES users (id),
    department_id BIGINT       NOT NULL REFERENCES departments (id),
    date_rdv      TIMESTAMP    NOT NULL,
    slot_start    TIMESTAMP    NOT NULL,
    motif_rdv     VARCHAR(512),
    status        VARCHAR(16)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_appointments_status CHECK (status IN ('BOOKED', 'CANCELLED'))
);

-- Enforces "a manager cannot have more than one appointment per time slot".
-- Partial index so a cancelled appointment frees the slot for re-booking.
-- This is the race-proof backstop against concurrent double-booking.
CREATE UNIQUE INDEX uq_manager_slot_active
    ON appointments (manager_id, slot_start)
    WHERE status = 'BOOKED';

CREATE INDEX idx_appointments_manager ON appointments (manager_id);
CREATE INDEX idx_appointments_date_rdv ON appointments (date_rdv);

CREATE TABLE appointment_participant (
    appointment_id BIGINT NOT NULL REFERENCES appointments (id) ON DELETE CASCADE,
    client_id      BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (appointment_id, client_id)
);
