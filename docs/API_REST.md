# API REST — Documentación

## Base URL
http://localhost:8080

## Endpoints disponibles

### Estudiantes

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/estudiantes | Lista todos los estudiantes |
| GET | /api/estudiantes?legajo=X | Filtra por legajo |

### Profesores

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/profesores | Lista todos los profesores |

### Reportes

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/reportes/resumen | Resumen general del sistema |

## Formato de respuesta

Todos los endpoints devuelven JSON con `Content-Type: application/json`.

En caso de error devuelven:
{
  "error": "descripción del error"
}
