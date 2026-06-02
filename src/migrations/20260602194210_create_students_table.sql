CREATE TABLE students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_person INTEGER NOT NULL,
    legajo TEXT NOT NULL UNIQUE,
    situacion TEXT NOT NULL,
    CONSTRAINT fk_student_person FOREIGN KEY (id_person)
    REFERENCES persons(id)
    ON DELETE CASCADE
);
