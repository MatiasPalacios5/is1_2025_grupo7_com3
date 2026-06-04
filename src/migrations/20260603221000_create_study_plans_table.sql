CREATE TABLE study_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    year INTEGER NOT NULL,
    resolution TEXT NOT NULL,
    id_career INTEGER NOT NULL,
    CONSTRAINT fk_study_plans_career FOREIGN KEY (id_career)
    REFERENCES careers(id) ON DELETE CASCADE
);
