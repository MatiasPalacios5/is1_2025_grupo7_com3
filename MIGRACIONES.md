# Guía de Migraciones de Base de Datos

## ¿Por qué usamos migraciones?

Antes cada integrante tenía su propia base de datos local con datos y tablas distintas. Esto generaba confusión porque algo que funcionaba en la computadora de uno, no funcionaba en la de otro.

Las migraciones resuelven ese problema: todos los cambios en la base de datos se guardan como archivos en el repositorio, y cuando cualquier integrante hace `git pull`, su base de datos se actualiza automáticamente.

---

## Arrancar desde cero (primera vez o si algo se rompe)

Si es la primera vez que clonás el proyecto, o si tu base de datos quedó en mal estado, hacé esto:

```bash
# 1. Borrar la base de datos actual
rm db/dev.db

# 2. Aplicar todas las migraciones desde cero
mvn db-migrator:migrate
```

Eso es todo. Tu base de datos va a quedar exactamente igual a la del resto del equipo.

---

## Actualizar la base de datos después de un git pull

Cada vez que alguien del equipo agrega una migración nueva y la sube al repositorio, tenés que aplicarla en tu base de datos local:

```bash
# 1. Traer los cambios del repositorio
git pull origin main

# 2. Aplicar las migraciones pendientes
mvn db-migrator:migrate
```

El plugin es inteligente: solo aplica las migraciones que todavía no tenés. No va a repetir las que ya aplicaste.

---

## Agregar un cambio a la base de datos

Cada vez que necesites modificar la estructura de la base de datos (crear una tabla nueva, agregar una columna, etc.) tenés que crear una migración. **Nunca modifiques el archivo `scheme.sql` directamente.**

**Paso 1: Crear el archivo de migración**
```bash
mvn db-migrator:new -Dname=descripcion_del_cambio
```

Por ejemplo, si querés crear la tabla de estudiantes:
```bash
mvn db-migrator:new -Dname=create_students_table
```

Esto genera un archivo vacío en `src/migrations/` con un nombre parecido a:
```
src/migrations/20260527192006_create_students_table.sql
```

**Paso 2: Escribir el SQL adentro**

Abrí ese archivo y escribí el SQL correspondiente. Por ejemplo:
```sql
CREATE TABLE students (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_person INTEGER NOT NULL,
    legajo TEXT NOT NULL UNIQUE,
    situacion TEXT NOT NULL,
    CONSTRAINT fk_person FOREIGN KEY (id_person)
    REFERENCES persons(id)
    ON DELETE CASCADE
);
```

**Paso 3: Probar la migración localmente**
```bash
mvn db-migrator:migrate
```

Si todo salió bien vas a ver algo como:
```
[INFO] Running migration 20260527192006_create_students_table.sql
[INFO] Migrated database
```

**Paso 4: Subir al repositorio**
```bash
git add src/migrations/
git commit -m "chore: agrego migración para tabla students"
git push origin main
```

A partir de ese momento, cuando cualquier integrante haga `git pull` y ejecute `mvn db-migrator:migrate`, va a tener la tabla `students` en su base de datos automáticamente.

---

## Reglas importantes

- **Nunca modifiques una migración que ya subiste al repositorio.** Si necesitás corregir algo, creá una migración nueva.
- **Nunca uses DROP TABLE en una migración.** Si necesitás eliminar una tabla, consultalo con el equipo primero.
- **Una migración por cada cambio.** No juntes varios cambios en una sola migración.
- **Siempre probá la migración localmente antes de hacer push.**

---

## Qué cambios requieren una migración

| Acción | Ejemplo |
|---|---|
| Crear una tabla nueva | Agregar la tabla `students` |
| Agregar una columna | Agregar `telefono` a `persons` |
| Eliminar una columna | Quitar un campo que ya no se usa |
| Modificar una columna | Cambiar el tipo de un campo |
| Agregar una clave foránea | Vincular `students` con `careers` |

## Qué NO requiere una migración

| Acción |
|---|
| Cambiar lógica de negocio en Java |
| Modificar las vistas Mustache |
| Agregar o modificar rutas en Spark |
| Insertar o modificar datos en las tablas |
