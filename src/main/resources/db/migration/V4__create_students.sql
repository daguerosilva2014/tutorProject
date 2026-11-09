CREATE TABLE students (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    contact_number VARCHAR(30),
    notes VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_students_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
