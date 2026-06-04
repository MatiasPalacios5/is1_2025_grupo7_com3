CREATE TABLE student_careers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_student INTEGER NOT NULL,
    id_career INTEGER NOT NULL,
    CONSTRAINT fk_sc_student FOREIGN KEY (id_student)
    REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_career FOREIGN KEY (id_career)
    REFERENCES careers(id) ON DELETE CASCADE
);
