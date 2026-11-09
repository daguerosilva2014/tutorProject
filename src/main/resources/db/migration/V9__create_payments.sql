CREATE TABLE payments (
    id UUID PRIMARY KEY,
    package_id UUID NOT NULL REFERENCES class_packages(id),
    amount NUMERIC(14, 2) NOT NULL,
    paid_at DATE NOT NULL,
    method VARCHAR(20),
    notes VARCHAR(1000),
    registered_by UUID NOT NULL REFERENCES users(id),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_method CHECK (method IS NULL OR method IN ('CASH', 'TRANSFER', 'OTHER'))
);

CREATE INDEX idx_payments_package_date ON payments (package_id, paid_at);
