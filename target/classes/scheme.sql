-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

DROP TABLE IF EXISTS persons;
CREATE TABLE persons(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni INTEGER UNIQUE NOT NULL,
    name TEXT NOT NULL
);

DROP TABLE IF EXISTS teachers;
CREATE TABLE teachers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_person INTEGER,
    career TEXT NOT NULL,

    CONSTRAINT fk_id FOREIGN KEY (id_person) REFERENCES persons(id) 
);
