CREATE TABLE teacher_subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_teacher INTEGER NOT NULL,
    id_subject INTEGER NOT NULL,
    periodo TEXT NOT NULL,
    CONSTRAINT fk_ts_teacher FOREIGN KEY (id_teacher)
    REFERENCES teachers(id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_subject FOREIGN KEY (id_subject)
    REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT unique_teacher_subject_period
    UNIQUE (id_teacher, id_subject, periodo)
);
