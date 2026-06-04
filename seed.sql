-- seed.sql
-- Limpiar tablas
DELETE FROM taken_exams;
DELETE FROM period_teacher_subject;
DELETE FROM academic_periods;
DELETE FROM teachers;
DELETE FROM student_careers;
DELETE FROM students;
DELETE FROM prerequisites;
DELETE FROM subjects;
DELETE FROM study_plans;
DELETE FROM careers;
-- Eliminar personas (usuarios en tabla users son independientes)
DELETE FROM persons;

-- Carreras
INSERT INTO careers (id, nombre, codigo, duracion) VALUES 
(1, 'Ingeniería en Sistemas', 10, 5),
(2, 'Licenciatura en Informática', 20, 4);

-- Planes de Estudio
INSERT INTO study_plans (id, year, resolution, id_career) VALUES 
(1, 2025, 'RES-2025-01', 1),
(2, 2020, 'RES-2020-05', 2);

-- Materias (Ingeniería)
INSERT INTO subjects (id, nombre, codigo, duracion, año_dictado, cuatrimestre_dictado, id_study_plan) VALUES 
(1, 'Matemática 1', 1001, 'Cuatrimestral', 1, 1, 1),
(2, 'Programación 1', 1002, 'Cuatrimestral', 1, 1, 1),
(3, 'Matemática 2', 1003, 'Cuatrimestral', 1, 2, 1),
(4, 'Programación 2', 1004, 'Cuatrimestral', 1, 2, 1),
(5, 'Física', 1005, 'Anual', 2, 0, 1);

-- Correlativas
INSERT INTO prerequisites (id_subject, id_prerequisite) VALUES 
(3, 1), -- Mat2 requiere Mat1
(4, 2); -- Prog2 requiere Prog1

-- Personas (Estudiantes y Profesores)
INSERT INTO persons (id, name, apellido, dni) VALUES 
(100, 'Matias', 'Palacios', 12345678),
(101, 'Lucia', 'Gomez', 87654321),
(102, 'Jorge', 'Perez', 33333333),
(103, 'Ana', 'Martinez', 44444444);

-- Estudiantes
INSERT INTO students (id, legajo, situacion, id_person) VALUES 
(1, 'LEG001', 'Efectivo', 100),
(2, 'LEG002', 'Ingresante', 101);

-- Inscripción a carreras
INSERT INTO student_careers (id_student, id_career) VALUES 
(1, 1),
(2, 2);

-- Profesores
INSERT INTO teachers (id, id_person, career, email) VALUES 
(1, 102, 'Ingeniero en Sistemas', 'jorge@test.com'),
(2, 103, 'Licenciada en Matemática', 'ana@test.com');

-- Períodos Académicos
INSERT INTO academic_periods (id, year, semester) VALUES 
(1, 2025, 1),
(2, 2025, 2);

-- Asignaciones Docentes (Profe 1 dicta Prog1 y Prog2, Profe 2 dicta Mat1 y Mat2)
INSERT INTO period_teacher_subject (id_subject, id_teacher, id_academic_period) VALUES 
(2, 1, 1),
(4, 1, 2),
(1, 2, 1),
(3, 2, 2);

-- Exámenes Rendidos (Matias rindió Mat1 y Prog1, Lucia rindió Prog1)
INSERT INTO taken_exams (id_student, id_subject, fecha, nota) VALUES 
(1, 1, '2025-07-10', 8),
(1, 2, '2025-07-12', 9),
(2, 2, '2025-07-12', 6);
