CREATE TABLE taken_exams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_student INTEGER NOT NULL,
    id_subject INTEGER NOT NULL,
    fecha DATE NOT NULL,
    nota INTEGER NOT NULL CHECK (nota >= 1 AND nota <= 10),
    CONSTRAINT fk_te_student FOREIGN KEY (id_student) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_te_subject FOREIGN KEY (id_subject) REFERENCES subjects(id) ON DELETE CASCADE
);
