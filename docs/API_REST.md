# API REST — Documentación

## Base URL
http://localhost:8080

## Endpoints disponibles

### Estudiantes

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/estudiantes | Lista todos los estudiantes |
| GET | /api/estudiantes?legajo=X | Filtra por legajo |
| GET | /api/estudiantes?carrera=X | Filtra por carrera (nombre o código) |

### Profesores

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/profesores | Lista todos los profesores |

### Materias

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/materias | Lista todas las materias |

### Reportes

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/reportes/resumen | Resumen general del sistema |

> **Nota:** el endpoint `/api/reportes/resumen` devuelve el total real de materias registradas en el sistema.

## Formato de respuesta

Todos los endpoints devuelven JSON con `Content-Type: application/json`.

En caso de error devuelven:
{
  "error": "descripción del error"
}
