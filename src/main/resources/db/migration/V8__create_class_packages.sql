CREATE TABLE class_packages (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id),
    total_classes INTEGER NOT NULL,
    used_classes INTEGER NOT NULL DEFAULT 0,
    price_amount NUMERIC(14, 2) NOT NULL,
    paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    purchased_at DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_class_packages_total CHECK (total_classes > 0),
    CONSTRAINT ck_class_packages_used CHECK (used_classes >= 0 AND used_classes <= total_classes),
    CONSTRAINT ck_class_packages_price CHECK (price_amount >= 0),
    CONSTRAINT ck_class_packages_paid CHECK (paid_amount >= 0 AND paid_amount <= price_amount)
);

CREATE INDEX idx_class_packages_student_date ON class_packages (student_id, purchased_at);
