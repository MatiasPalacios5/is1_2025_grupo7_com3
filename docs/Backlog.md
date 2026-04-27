## 1. Gestión (Configuración Inicial)
Título: Configurar entorno de compilación para ActiveJDBC

Tipo de tarea: Gestión

Descripción: Configurar el plugin de Maven para el "paso especial de compilación" que requiere ActiveJDBC para funcionar correctamente.

Estimación: -----

Prioridad: Alta.

Responsable: -----
------------------------------------------------------------------
## 2. Feature (Funcionalidad Principal)
Título: Implementar Gestión de Alumnos (CRUD)

Tipo de tarea: Feature

Descripción: Desarrollar las rutas en Spark Java y los modelos para el Alta, Baja y Modificación de alumnos en la base de datos SQLite.

Estimación: -----

Prioridad: Alta.

Responsable: -----

------------------------------------------------------------------

## 3. Feature (Seguridad)
Título: Sistema de Autenticación con BCrypt

Tipo de tarea: Feature

Descripción: Implementar el Login/Logout de usuarios. Las contraseñas deben guardarse hasheadas usando la librería jbcrypt 0.4.

Estimación: ------

Prioridad: Alta.

Responsable: ------

-------------------------------------------------------------------

## 4. Investigación / Riesgo Técnico
Título: Investigación: Manejo de errores en vistas Mustache

Tipo de tarea: Investigación

Descripción: Investigar la mejor forma de pasar mensajes de error (ej: "DNI duplicado") desde el controlador de Spark hacia las plantillas de Mustache, dado que el motor tiene lógica limitada.

Estimación: ------

Prioridad: Media.

Responsable: ------

-------------------------------------------------------------------

## 5. Feature (API)
Título: Exponer Endpoints JSON para Reportes Académicos

Tipo de tarea: Feature

Descripción: Utilizar Jackson 2.17.1 para serializar la información de materias y alumnos en formato JSON para consumo externo.

Estimación: ------

Prioridad: Baja.

Responsable: ------

-------------------------------------------------------------------

## 6. Feature (Modelo de Datos)
Título: Implementar Relación Profesor-Materia (ActiveJDBC)

Tipo de tarea: Feature

Descripción: Definir la tabla intermedia y la lógica de modelos para que un profesor pueda ser asignado a una o más materias. Se debe asegurar que la relación se persista correctamente en la base de datos SQLite.

Estimación: -------

Prioridad: Alta.

Responsable: -------

-------------------------------------------------------------------

## 7. Gestión (Base de Datos)
Título: Creación de Scripts de Migración SQLite

Tipo de tarea: Gestión

Descripción: Crear el archivo .sql inicial con el esquema de tablas (Alumnos, Profesores, Materias, Usuarios) e insertar datos de prueba para que todo el equipo trabaje sobre la misma base.

Estimación: -------

Prioridad: Alta.

Responsable: -------

-------------------------------------------------------------------

## 8. Feature (Interfaz de Usuario)
Título: Formulario de Asignación de Cargos Docentes

Tipo de tarea: Feature

Descripción: Crear la vista en Mustache que permita seleccionar un Profesor de un desplegable y asignarlo a una Materia específica, validando que el profesor no tenga conflictos de horario.

Estimación: -------

Prioridad: Media.

Responsable: -------

-------------------------------------------------------------------
