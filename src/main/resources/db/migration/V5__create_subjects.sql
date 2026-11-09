CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_subjects_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
