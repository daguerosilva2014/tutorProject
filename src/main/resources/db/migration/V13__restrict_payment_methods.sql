ALTER TABLE payments DROP CONSTRAINT ck_payments_method;
ALTER TABLE payments ADD CONSTRAINT ck_payments_method
    CHECK (method IS NULL OR method IN ('CASH', 'TRANSFER'));

ALTER TABLE per_class_payments DROP CONSTRAINT ck_per_class_payments_method;
ALTER TABLE per_class_payments ADD CONSTRAINT ck_per_class_payments_method
    CHECK (method IS NULL OR method IN ('CASH', 'TRANSFER'));
