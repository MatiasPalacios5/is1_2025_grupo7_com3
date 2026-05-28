# Sistema de Gestión Académica
**Ingeniería de Software II (Cód. 3387) — Año 2026 — Grupo 7 | Comisión 3**

---

## ¿Qué es este proyecto?

Sistema web para centralizar y optimizar la gestión académica de una institución educativa. Permite administrar alumnos, profesores, materias y carreras con control de acceso por roles.

---

## Tecnologías utilizadas

| Tecnología | Versión | Para qué se usa |
|---|---|---|
| Java | 11 | Lenguaje principal |
| Spark Java | 2.9.4 | Framework web |
| SQLite | 3.45.1 | Base de datos |
| ActiveJDBC | 3.4-j11 | ORM (conexión con la BD) |
| Mustache | 2.7.1 | Vistas HTML |
| BCrypt | 0.4 | Hash de contraseñas |
| Jackson | 2.17.1 | Serialización JSON |
| JUnit Jupiter | 5.10.0 | Tests |
| Maven | 3.8+ | Build y dependencias |
| Tailwind CSS | CDN | Estilos |

---

## Requisitos previos

Antes de arrancar necesitás tener instalado:

- **Java 11** → verificar con `java -version`
- **Maven 3.8+** → verificar con `mvn -version`
- **Git** → verificar con `git --version`

---

## Cómo clonar y arrancar el proyecto por primera vez

**1. Clonar el repositorio**
```bash
git clone https://github.com/MatiasPalacios5/is1_2025_grupo7_com3.git
cd is1_2025_grupo7_com3
```

**2. Crear la base de datos con las migraciones**
```bash
mvn db-migrator:migrate
```

**3. Compilar y ejecutar el proyecto**
```bash
mvn clean package -DskipTests
java -Ddb.url=jdbc:sqlite:./db/dev.db -jar target/proye-is-1.0-SNAPSHOT.jar
```

**4. Abrir en el navegador**
```
http://localhost:8080
```

> **¿Por qué este comando?** ActiveJDBC requiere un paso especial de instrumentación de bytecode que solo funciona correctamente cuando se empaqueta el JAR completo con `mvn clean package`. Usar `mvn exec:java` directamente omite ese paso y genera el error `activejdbc_models.properties not found`.

---

## Cómo actualizar el proyecto después de un git pull

Cada vez que alguien del equipo sube cambios, seguí estos pasos:

```bash
# 1. Traer los cambios
git pull origin main

# 2. Aplicar las migraciones nuevas (si las hay)
mvn db-migrator:migrate

# 3. Compilar y ejecutar
mvn clean package -DskipTests
java -Ddb.url=jdbc:sqlite:./db/dev.db -jar target/proye-is-1.0-SNAPSHOT.jar
```

> Si algo se rompe con la base de datos, podés resetearla desde cero:
> ```bash
> rm db/dev.db
> mvn db-migrator:migrate
> ```

---

## Estructura del proyecto

```
is1_2025_grupo7_com3/
│
├── src/
│   ├── main/
│   │   ├── java/com/is1/proyecto/
│   │   │   ├── App.java                  ← Rutas y lógica principal
│   │   │   ├── config/
│   │   │   │   └── DBConfigSingleton.java ← Configuración de la BD
│   │   │   └── models/
│   │   │       ├── User.java             ← Modelo de usuarios
│   │   │       ├── Person.java           ← Modelo de personas
│   │   │       └── Teacher.java          ← Modelo de docentes
│   │   └── resources/
│   │       ├── templates/               ← Vistas Mustache (HTML)
│   │       ├── database.properties      ← Configuración de la BD
│   │       └── scheme.sql               ← Esquema original (no usar)
│   └── test/
│       └── java/com/is1/proyecto/
│           └── AppTest.java             ← Tests
│
├── src/migrations/                      ← Migraciones de la BD
│   └── 20260527192006_create_initial_tables.sql
│
├── db/
│   └── dev.db                           ← Base de datos local (no subir)
│
├── docs/
│   ├── Requirements.md                  ← Ejercicio 1 y 2
│   ├── Backlog.md                       ← Backlog del proyecto
│   ├── SQA.md                           ← Criterios de aceptación
│   └── diagrama_de_clases.md            ← Diagrama UML
│
├── MIGRACIONES.md                       ← Guía de migraciones
├── IMPLEMENTACION.md                    ← Guía de implementación pendiente
├── pom.xml                              ← Configuración de Maven
└── README.md                            ← Este archivo
```

---

## Cómo agregar un cambio a la base de datos

Cada vez que necesitás crear o modificar una tabla, usá el sistema de migraciones. **Nunca modifiques `scheme.sql` directamente.**

```bash
# 1. Crear la migración
mvn db-migrator:new -Dname=descripcion_del_cambio

# 2. Escribir el SQL en el archivo generado (src/migrations/)

# 3. Probar localmente
mvn db-migrator:migrate

# 4. Subir al repo
git add src/migrations/
git commit -m "chore: agrego migración para [descripción]"
git push origin main
```

Para más detalles leé el archivo **MIGRACIONES.md**.

---

## Cómo correr los tests

```bash
mvn test
```

---

## Perfiles de base de datos

El proyecto tiene tres perfiles configurados en el `pom.xml`:

| Perfil | Base de datos | Cuándo usarlo |
|---|---|---|
| `dev` (default) | `db/dev.db` | Desarrollo diario |
| `test` | `target/test.db` | Correr tests |
| `prod` | `db/prod.db` | Entrega final |

Para ejecutar con un perfil específico:
```bash
# Desarrollo (default)
java -Ddb.url=jdbc:sqlite:./db/dev.db -jar target/proye-is-1.0-SNAPSHOT.jar

# Producción
java -Ddb.url=jdbc:sqlite:./db/prod.db -jar target/proye-is-1.0-SNAPSHOT.jar
```

---

## Flujo de trabajo del equipo

**Antes de empezar a trabajar:**
```bash
git pull origin main
mvn db-migrator:migrate
mvn clean package -DskipTests
java -Ddb.url=jdbc:sqlite:./db/dev.db -jar target/proye-is-1.0-SNAPSHOT.jar
```

**Al terminar una tarea:**
```bash
git add .
git commit -m "feat: descripción del cambio - closes #N"
git push origin main
```

> Reemplazá `#N` con el número del issue que estás resolviendo.

**Si hay conflictos al hacer push:**
```bash
git config pull.rebase false
git pull origin main
# resolver conflictos si los hay
git push origin main
```

---

## Convenciones para los commits

Usar el siguiente formato para los mensajes de commit:

| Prefijo | Cuándo usarlo |
|---|---|
| `feat:` | Nueva funcionalidad |
| `fix:` | Corrección de un bug |
| `chore:` | Configuración, migraciones, dependencias |
| `docs:` | Cambios en documentación |
| `test:` | Agregar o modificar tests |
| `refactor:` | Mejora de código sin cambiar funcionalidad |

Ejemplos:
```bash
git commit -m "feat: implemento CRUD de Materia - closes #9"
git commit -m "fix: corrijo validación de DNI duplicado - closes #4"
git commit -m "chore: agrego migración para tabla students"
git commit -m "docs: actualizo diagrama de clases"
```

---

## Documentación del proyecto

| Archivo | Contenido |
|---|---|
| `docs/Requirements.md` | Descripción del proyecto y análisis de riesgos |
| `docs/Backlog.md` | Backlog con todas las tareas |
| `docs/SQA.md` | Criterios de aceptación por tarea |
| `docs/diagrama_de_clases.md` | Diagrama UML del sistema |
| `MIGRACIONES.md` | Guía completa para manejar la base de datos |
| `IMPLEMENTACION.md` | Instrucciones para implementar las clases pendientes |

---

## Backlog y issues

El backlog del proyecto está en **GitHub Projects**:
👉 [Ver tablero](https://github.com/MatiasPalacios5/is1_2025_grupo7_com3/projects)

Cada integrante debe:
1. Tomar un issue asignado
2. Implementarlo
3. Hacer commit referenciando el issue (`closes #N`)
4. Registrar el tiempo real en el issue y compararlo con la estimación

---

## Contacto del equipo

Comunicación interna por **Discord**.
Repositorio: [github.com/MatiasPalacios5/is1_2025_grupo7_com3](https://github.com/MatiasPalacios5/is1_2025_grupo7_com3)
