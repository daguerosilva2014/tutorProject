CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'TEACHER')),
    CONSTRAINT ck_users_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT uq_users_username UNIQUE (username)
);
