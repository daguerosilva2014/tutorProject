CREATE TABLE class_consumptions (
    id UUID PRIMARY KEY,
    participation_id UUID NOT NULL UNIQUE REFERENCES class_participations(id),
    package_id UUID NOT NULL REFERENCES class_packages(id),
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE per_class_payments (
    id UUID PRIMARY KEY,
    participation_id UUID NOT NULL REFERENCES class_participations(id),
    amount NUMERIC(14, 2) NOT NULL,
    paid_at DATE NOT NULL,
    method VARCHAR(20),
    notes VARCHAR(1000),
    registered_by UUID NOT NULL REFERENCES users(id),
    CONSTRAINT ck_per_class_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_per_class_payments_method CHECK (method IS NULL OR method IN ('CASH', 'TRANSFER', 'OTHER'))
);

CREATE INDEX idx_per_class_payments_participation ON per_class_payments (participation_id, paid_at);
