CREATE TABLE class_participations (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id),
    student_id UUID NOT NULL REFERENCES students(id),
    CONSTRAINT uq_class_participation_class_student UNIQUE (class_id, student_id)
);

CREATE TABLE class_participation_subjects (
    participation_id UUID NOT NULL REFERENCES class_participations(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    PRIMARY KEY (participation_id, subject_id)
);

CREATE INDEX idx_class_participations_student ON class_participations (student_id);
