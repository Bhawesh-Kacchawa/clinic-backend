CREATE TABLE clinic (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE staff_user (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL REFERENCES clinic (id),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('DOCTOR', 'RECEPTIONIST')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_staff_user_email UNIQUE (email)
);

CREATE TABLE clinic_session (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL REFERENCES clinic (id),
    doctor_id UUID NOT NULL REFERENCES staff_user (id),
    session_type VARCHAR(20) NOT NULL CHECK (session_type IN ('MORNING', 'EVENING')),
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_appointments INTEGER NOT NULL CHECK (max_appointments > 0),
    booking_opens_at TIMESTAMPTZ NOT NULL,
    booking_status VARCHAR(20) NOT NULL
        CHECK (booking_status IN ('SCHEDULED', 'OPEN', 'PAUSED', 'CLOSED')),
    avg_minutes INTEGER NOT NULL CHECK (avg_minutes > 0)
);

CREATE TABLE booking (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES clinic_session (id),
    token_no INTEGER NOT NULL CHECK (token_no > 0),
    queue_order INTEGER NOT NULL CHECK (queue_order > 0),
    patient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    reason VARCHAR(1000),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('BOOKED', 'CALLED', 'SERVING', 'DONE', 'SKIPPED', 'NO_SHOW', 'CANCELLED')),
    source VARCHAR(20) NOT NULL CHECK (source IN ('ONLINE', 'WALK_IN')),
    skip_count INTEGER NOT NULL DEFAULT 0 CHECK (skip_count >= 0),
    called_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uk_booking_session_token UNIQUE (session_id, token_no)
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    clinic_id UUID REFERENCES clinic (id),
    actor_id UUID REFERENCES staff_user (id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_staff_user_clinic_id ON staff_user (clinic_id);
CREATE INDEX idx_clinic_session_clinic_date ON clinic_session (clinic_id, session_date);
CREATE INDEX idx_clinic_session_doctor_id ON clinic_session (doctor_id);
CREATE INDEX idx_booking_session_queue_order ON booking (session_id, queue_order);
CREATE INDEX idx_audit_log_clinic_created_at ON audit_log (clinic_id, created_at);
