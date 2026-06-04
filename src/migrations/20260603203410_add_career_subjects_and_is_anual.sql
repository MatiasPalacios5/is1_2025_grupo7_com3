ALTER TABLE subjects ADD COLUMN is_anual INTEGER DEFAULT 0;

CREATE TABLE career_subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_career INTEGER NOT NULL,
    id_subject INTEGER NOT NULL,
    CONSTRAINT fk_cs_career FOREIGN KEY (id_career) REFERENCES careers(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_subject FOREIGN KEY (id_subject) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT unique_career_subject UNIQUE (id_career, id_subject)
);
