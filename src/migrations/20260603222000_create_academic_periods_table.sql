CREATE TABLE academic_periods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    year INTEGER NOT NULL,
    semester INTEGER NOT NULL,
    CONSTRAINT unique_period UNIQUE (year, semester)
);

CREATE TABLE period_teacher_subject (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_academic_period INTEGER NOT NULL,
    id_teacher INTEGER NOT NULL,
    id_subject INTEGER NOT NULL,
    CONSTRAINT fk_pts_period FOREIGN KEY (id_academic_period) REFERENCES academic_periods(id) ON DELETE CASCADE,
    CONSTRAINT fk_pts_teacher FOREIGN KEY (id_teacher) REFERENCES teachers(id) ON DELETE CASCADE,
    CONSTRAINT fk_pts_subject FOREIGN KEY (id_subject) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT unique_assignment UNIQUE (id_academic_period, id_teacher, id_subject)
);

DROP TABLE IF EXISTS teacher_subjects;
