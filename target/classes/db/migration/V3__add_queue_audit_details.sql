ALTER TABLE audit_log
    ADD COLUMN audit_order BIGSERIAL NOT NULL,
    ADD COLUMN operation_id UUID,
    ADD COLUMN token_no INTEGER,
    ADD COLUMN before_status VARCHAR(20),
    ADD COLUMN after_status VARCHAR(20),
    ADD COLUMN queue_order_before INTEGER,
    ADD COLUMN queue_order_after INTEGER,
    ADD COLUMN skip_count_before INTEGER,
    ADD COLUMN skip_count_after INTEGER,
    ADD COLUMN called_at_before TIMESTAMPTZ,
    ADD COLUMN called_at_after TIMESTAMPTZ,
    ADD COLUMN started_at_before TIMESTAMPTZ,
    ADD COLUMN started_at_after TIMESTAMPTZ,
    ADD COLUMN completed_at_before TIMESTAMPTZ,
    ADD COLUMN completed_at_after TIMESTAMPTZ,
    ADD COLUMN undone BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uk_audit_log_audit_order ON audit_log (audit_order);
CREATE INDEX idx_audit_log_entity_order ON audit_log (clinic_id, entity_type, entity_id, audit_order DESC);
CREATE INDEX idx_audit_log_operation ON audit_log (clinic_id, operation_id, audit_order);
