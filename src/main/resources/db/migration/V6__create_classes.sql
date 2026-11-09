CREATE TABLE classes (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES users(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000),
    CONSTRAINT ck_classes_time CHECK (start_time < end_time),
    CONSTRAINT ck_classes_status CHECK (status IN ('SCHEDULED', 'FINALIZED', 'CANCELLED'))
);

CREATE INDEX idx_classes_teacher_date ON classes (teacher_id, date);
