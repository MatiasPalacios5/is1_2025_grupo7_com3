## 1. Feature: Autenticación con BCrypt
Tipo: Feature

Prioridad: Crítica

Estimación: -------

Criterios de Aceptación:

El sistema permite registrar un usuario y hashear la clave usando jbcrypt 0.4.

El login valida correctamente las credenciales contra la DB SQLite.

SQA: El código compila incluyendo el paso de instrumentación de ActiveJDBC.

SQA: Se incluyó al menos un test unitario con JUnit para la validación de contraseñas.

------------------------------------------------------------------

## 2. Feature: Gestión de Profesores (CRUD)
Tipo: Feature

Prioridad: Alta

Estimación: ---------

Criterios de Aceptación:

Se pueden crear, editar y eliminar profesores con nombre, apellido, DNI, email y carrera.

El sistema impide el alta si el DNI o Email ya existen en la base de datos.

SQA: Se muestra un mensaje de error claro en la vista Mustache si hay duplicados.

SQA: El código fue revisado por otro integrante del equipo (Pull Request aprobado).

-------------------------------------------------------------------

## 3. Gestión: Configuración de Tests y CI
Tipo: Gestión

Prioridad: Alta

Estimación: -------------

Criterios de Aceptación:

Configuración de JUnit Jupiter 5.10.0 en el archivo pom.xml.

Implementación de una prueba base que verifique la conexión a SQLite.

SQA: La tarea se considera terminada si los tests pasan localmente y en el entorno de GitHub Actions (si usan CI).

SQA: Se documentó en el README cómo ejecutar los tests.

-------------------------------------------------------------------

## 4. Feature: API REST - Endpoints de Alumnos
Tipo: Feature

Prioridad: Media

Estimación: ------

Criterios de Aceptación:

Exposición de un endpoint /api/alumnos que devuelva un JSON usando Jackson 2.17.1.

El endpoint debe respetar los filtros de búsqueda por legajo o carrera.

SQA: El JSON generado cumple con el estándar definido por el equipo.

SQA: El código no utiliza features de versiones superiores a Java 11.

## 5. Feature: Asignación de Materias a Profesores
Tipo: Feature

Prioridad: Alta

Estimación: -------

Criterios de Aceptación:

El sistema permite vincular un Profesor existente con una Materia mediante una tabla intermedia.

La interfaz (Mustache) muestra solo profesores y materias activos para la selección.

SQA: Se valida que un profesor no pueda ser asignado dos veces a la misma materia en el mismo período.

SQA: La relación se persiste correctamente en SQLite y se puede recuperar mediante los métodos parent() o getAll() de ActiveJDBC.

--------------------------------------------------------------

## 6. Gestión: Diseño y Despliegue del Esquema SQL
Tipo: Gestión

Prioridad: Alta

Estimación: -----

Criterios de Aceptación:

Creación del archivo src/main/resources/schema.sql con las sentencias CREATE TABLE para Alumnos, Profesores, Materias y Usuarios.

Inclusión de un script de "Seed" (datos de prueba) para facilitar el testeo inicial.

SQA: El esquema cumple con la 3ra Forma Normal (3FN) para evitar redundancias, atacando el riesgo de integridad de datos.

SQA: Se verificó que los tipos de datos en SQLite (TEXT, INTEGER) coincidan con los tipos de atributos definidos en los modelos Java.
