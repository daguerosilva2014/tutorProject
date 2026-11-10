ALTER TABLE class_participations
    ADD COLUMN recovery_target_id UUID REFERENCES class_participations(id);

ALTER TABLE class_participations
    ADD CONSTRAINT uq_participation_recovery_target UNIQUE (recovery_target_id);
