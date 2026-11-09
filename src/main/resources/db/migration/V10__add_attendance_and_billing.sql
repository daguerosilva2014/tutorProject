ALTER TABLE classes ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE class_participations ADD COLUMN billing_mode VARCHAR(20) NOT NULL DEFAULT 'PACKAGE';
ALTER TABLE class_participations ADD COLUMN unit_price_amount NUMERIC(14, 2);
ALTER TABLE class_participations ADD COLUMN paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0;
ALTER TABLE class_participations ADD COLUMN attendance_status VARCHAR(25) NOT NULL DEFAULT 'PENDING';
ALTER TABLE class_participations ADD COLUMN absence_notified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE class_participations ADD COLUMN consumption_status VARCHAR(25) NOT NULL DEFAULT 'PENDING';
ALTER TABLE class_participations ADD COLUMN consumed_package_id UUID REFERENCES class_packages(id);
ALTER TABLE class_participations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE class_participations ADD CONSTRAINT ck_participation_billing
    CHECK ((billing_mode = 'PACKAGE' AND unit_price_amount IS NULL)
        OR (billing_mode = 'PER_CLASS' AND unit_price_amount IS NOT NULL AND unit_price_amount >= 0));
ALTER TABLE class_participations ADD CONSTRAINT ck_participation_paid CHECK (paid_amount >= 0);
ALTER TABLE class_participations ADD CONSTRAINT ck_participation_attendance
    CHECK (attendance_status IN ('PENDING', 'PRESENT', 'ABSENT', 'JUSTIFIED_ABSENCE'));
ALTER TABLE class_participations ADD CONSTRAINT ck_participation_consumption
    CHECK (consumption_status IN ('PENDING', 'CONSUMED', 'NOT_CONSUMED', 'NOT_APPLICABLE'));
