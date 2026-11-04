ALTER TABLE clinic
    ADD COLUMN slug VARCHAR(100);

UPDATE clinic
SET slug = 'clinic-' || id::TEXT
WHERE slug IS NULL;

ALTER TABLE clinic
    ALTER COLUMN slug SET NOT NULL,
    ADD CONSTRAINT uk_clinic_slug UNIQUE (slug);
