# Guía de Carga Manual de Datos

Para que el sistema funcione correctamente y no haya errores de integridad (debido a las validaciones y claves foráneas), es fundamental cargar los datos en un orden específico.

## Orden de Carga Recomendado

### 1. Usuarios y Personas (Base del Sistema)
- Primero, cargá las Personas (alumnos, docentes, etc.).
- Luego, podés crear Usuarios y vincularlos a esas Personas para que tengan acceso al sistema con su rol correspondiente (`Admin`, `Docente`, `Alumno`).

### 2. Estructura Académica (Carreras y Planes)
- Creá las **Carreras** (ej. "Ingeniería en Sistemas").
- Creá los **Planes de Estudio** (ej. "Plan 2026") y asocialos a las carreras creadas anteriormente.

### 3. Materias y Correlativas
- Creá las **Materias** asociándolas al Plan de Estudio.
  - **Importante:** El **código** de la materia debe ser obligatoriamente un número entero de 4 dígitos (Ej: `1001`, `1002`).
  - La **duración** debe ser "Cuatrimestral" o "Anual".
- Una vez que existan varias materias, podés configurar las **Correlativas** (asignar qué materia es requisito para otra).

### 4. Períodos y Docentes
- Creá los **Docentes**, asociándolos a Personas que ya existan.
- Creá los **Períodos Académicos** (ej. "Primer Cuatrimestre 2026" con sus fechas de inicio y fin).
- Finalmente, podés realizar la **Asignación Docente**, vinculando un Docente a una Materia en un Período Académico específico.

### 5. Estudiantes y Exámenes
- Creá los **Estudiantes**, asociándolos a Personas que ya existan.
  - **Importante:** La **situación** del estudiante debe ser obligatoriamente `Ingresante` o `Efectivo`.
- Inscribí a los estudiantes a sus respectivas Carreras (Estudiante-Carrera).
- Finalmente, podés registrar **Exámenes Rendidos** vinculando a los estudiantes con las materias que rindieron, cargando su nota y fecha.

---

## ⚠️ Reglas de Negocio a Recordar
1. **Códigos Numéricos:** Las materias no aceptan letras en su código.
2. **Tipos Exactos:** Respetá las mayúsculas iniciales en estados como "Ingresante", "Efectivo", "Cuatrimestral", "Anual".
3. **Borrado en Cascada:** Al eliminar un docente desde la interfaz, todas sus asignaciones a materias se eliminarán automáticamente.
4. **Dependencias (Foreign Keys):** No podés asignar un estudiante a un examen si ese estudiante no fue creado previamente, ni podés crear una materia sin antes tener un plan de estudio. ¡Seguí siempre el orden de esta guía!
