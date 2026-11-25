ALTER TABLE clinic_session
    ADD COLUMN morning_booking_override BOOLEAN NOT NULL DEFAULT FALSE;
