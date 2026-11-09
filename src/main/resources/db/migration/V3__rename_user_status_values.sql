ALTER TABLE users DROP CONSTRAINT ck_users_status;

UPDATE users SET status = 'ACTIVE' WHERE status = 'ENABLED';
UPDATE users SET status = 'INACTIVE' WHERE status = 'DISABLED';

ALTER TABLE users ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
