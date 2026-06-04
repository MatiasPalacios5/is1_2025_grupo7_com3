ALTER TABLE subjects ADD COLUMN año_dictado INTEGER DEFAULT 1;
ALTER TABLE subjects ADD COLUMN cuatrimestre_dictado INTEGER DEFAULT 1;

DROP TABLE IF EXISTS career_subjects;

CREATE TABLE prerequisites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_subject INTEGER NOT NULL,
    id_prerequisite INTEGER NOT NULL,
    CONSTRAINT fk_prereq_subject FOREIGN KEY (id_subject) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_prereq_prereq FOREIGN KEY (id_prerequisite) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT unique_prereq UNIQUE (id_subject, id_prerequisite)
);
