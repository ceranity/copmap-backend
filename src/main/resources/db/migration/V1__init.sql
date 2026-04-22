-- CopMap initial schema
-- Authored by Krishnamurti

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------- Users ----------
CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username       VARCHAR(64)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(128) NOT NULL,
    badge_number   VARCHAR(32)  UNIQUE,
    phone          VARCHAR(32),
    role           VARCHAR(16)  NOT NULL CHECK (role IN ('OFFICER','PLANNER','SUPERVISOR')),
    station_id     UUID,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_users_role ON users(role);

-- ---------- Patrols ----------
CREATE TABLE patrols (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title          VARCHAR(200) NOT NULL,
    description    VARCHAR(2000),
    beat_name      VARCHAR(128),
    start_at       TIMESTAMPTZ  NOT NULL,
    end_at         TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(16)  NOT NULL CHECK (status IN ('PLANNED','ACTIVE','CLOSED','CANCELLED')),
    created_by     UUID         NOT NULL REFERENCES users(id),
    station_id     UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    closed_at      TIMESTAMPTZ,
    closure_notes  VARCHAR(2000)
);
CREATE INDEX ix_patrols_status ON patrols(status);
CREATE INDEX ix_patrols_start ON patrols(start_at);

CREATE TABLE checkpoints (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patrol_id          UUID         NOT NULL REFERENCES patrols(id) ON DELETE CASCADE,
    sequence           INTEGER      NOT NULL,
    name               VARCHAR(200) NOT NULL,
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    radius_meters      INTEGER      NOT NULL,
    due_offset_minutes INTEGER
);
CREATE INDEX ix_checkpoints_patrol ON checkpoints(patrol_id);

-- ---------- Bandobast / Nakabandi ----------
CREATE TABLE bandobast (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind            VARCHAR(16)  NOT NULL CHECK (kind IN ('BANDOBAST','NAKABANDI')),
    title           VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    venue           VARCHAR(200),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    radius_meters   INTEGER      NOT NULL,
    start_at        TIMESTAMPTZ  NOT NULL,
    end_at          TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL CHECK (status IN ('PLANNED','ACTIVE','CLOSED','CANCELLED')),
    expected_crowd  INTEGER,
    threat_level    VARCHAR(16),
    created_by      UUID         NOT NULL REFERENCES users(id),
    station_id      UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    closed_at       TIMESTAMPTZ,
    closure_notes   VARCHAR(2000)
);
CREATE INDEX ix_bandobast_status ON bandobast(status);
CREATE INDEX ix_bandobast_kind ON bandobast(kind);

-- ---------- Assignments ----------
CREATE TABLE assignments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    officer_id       UUID         NOT NULL REFERENCES users(id),
    patrol_id        UUID REFERENCES patrols(id) ON DELETE CASCADE,
    bandobast_id     UUID REFERENCES bandobast(id) ON DELETE CASCADE,
    role             VARCHAR(64),
    status           VARCHAR(16)  NOT NULL CHECK (status IN ('ASSIGNED','ACKNOWLEDGED','CHECKED_IN','CHECKED_OUT','NO_SHOW')),
    assigned_by      UUID         NOT NULL REFERENCES users(id),
    assigned_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    acknowledged_at  TIMESTAMPTZ,
    checked_in_at    TIMESTAMPTZ,
    checked_out_at   TIMESTAMPTZ,
    CHECK ((patrol_id IS NOT NULL) <> (bandobast_id IS NOT NULL))
);
CREATE INDEX ix_assignments_officer ON assignments(officer_id);
CREATE INDEX ix_assignments_patrol ON assignments(patrol_id);
CREATE INDEX ix_assignments_bandobast ON assignments(bandobast_id);

-- ---------- Location history ----------
CREATE TABLE location_pings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    officer_id       UUID NOT NULL REFERENCES users(id),
    assignment_id    UUID REFERENCES assignments(id) ON DELETE SET NULL,
    latitude         DOUBLE PRECISION NOT NULL,
    longitude        DOUBLE PRECISION NOT NULL,
    accuracy_meters  DOUBLE PRECISION,
    speed_mps        DOUBLE PRECISION,
    battery_pct      INTEGER,
    recorded_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_pings_officer_time ON location_pings(officer_id, recorded_at DESC);

-- ---------- Alerts ----------
CREATE TABLE alerts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(32) NOT NULL CHECK (type IN ('PANIC','GEOFENCE_BREACH','MISSED_CHECKPOINT','NO_LOCATION','MANUAL')),
    severity         VARCHAR(16) NOT NULL CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    officer_id       UUID REFERENCES users(id),
    patrol_id        UUID REFERENCES patrols(id) ON DELETE SET NULL,
    bandobast_id     UUID REFERENCES bandobast(id) ON DELETE SET NULL,
    message          VARCHAR(500),
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    status           VARCHAR(16) NOT NULL CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED')),
    acknowledged_by  UUID REFERENCES users(id),
    acknowledged_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_alerts_created ON alerts(created_at DESC);
CREATE INDEX ix_alerts_status ON alerts(status);

-- ---------- Audit ----------
CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id        UUID,
    actor_username  VARCHAR(64),
    action          VARCHAR(64) NOT NULL,
    entity_type     VARCHAR(64) NOT NULL,
    entity_id       UUID,
    details         VARCHAR(2000),
    at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity ON audit_events(entity_type, entity_id);
CREATE INDEX ix_audit_at ON audit_events(at DESC);

-- Seed users are inserted by DataSeeder at application boot (using the runtime BCrypt encoder)
-- so the hash is always valid regardless of cost factor changes.
