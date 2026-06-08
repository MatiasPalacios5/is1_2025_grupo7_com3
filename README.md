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

**2. Ejecutar el script de inicio (Modo Desarrollo)**
Este script corre automáticamente las migraciones, instrumenta las clases y levanta el servidor:
```bash
./run.sh
```

**3. (Opcional) Cargar datos de prueba iniciales**
Si necesitás datos de prueba (materias, profesores, etc.) ya cargados con el formato correcto:
```bash
sqlite3 ./db/dev.db < seed.sql
```

**5. Abrir en el navegador**
```
http://localhost:8080
```

> **¿Por qué usamos estos comandos específicos?** ActiveJDBC requiere un paso especial de instrumentación de bytecode (modificar las clases después de compilarlas) que ocurre en la fase `process-classes`. Si se usa un simple `mvn compile exec:java`, se omite la instrumentación y los modelos (la base de datos) fallan con `InitException`.

---

## 📝 Reglas de Negocio Actualizadas (Importante)
Tené en cuenta los siguientes formatos requeridos por el backend:
- **Materias:** El `código` debe ser estrictamente un **entero de 4 dígitos** (Ej: `1001`).
- **Estudiantes:** La `situación` acepta únicamente los valores **"Ingresante"** o **"Efectivo"**.
- Al eliminar un docente, se aplica **borrado en cascada** para sus asignaciones (`period_teacher_subject`).

---

## Cómo actualizar el proyecto después de un git pull

Cada vez que alguien del equipo sube cambios, seguí estos pasos:

```bash
# 1. Traer los cambios
git pull origin main

# 2. Migrar, compilar y ejecutar
./run.sh
```

> Si algo se rompe con la base de datos, podés resetearla desde cero:
> ```bash
> rm db/dev.db
> mvn db-migrator:migrate
> ```

---

## Estructura del proyecto

```text
is1_2025_grupo7_com3/
│
├── src/
│   ├── main/
│   │   ├── java/com/is1/proyecto/
│   │   │   ├── App.java                   ← Rutas y lógica principal de Spark
│   │   │   ├── config/
│   │   │   │   └── DBConfigSingleton.java ← Configuración de la BD (Singleton)
│   │   │   └── models/                    ← Modelos ActiveJDBC
│   │   │       ├── AcademicPeriod.java
│   │   │       ├── Career.java
│   │   │       ├── PeriodTeacherSubject.java
│   │   │       ├── Person.java
│   │   │       ├── Prerequisite.java
│   │   │       ├── Student.java
│   │   │       ├── StudentCareer.java
│   │   │       ├── StudyPlan.java
│   │   │       ├── Subject.java
│   │   │       ├── TakenExam.java
│   │   │       ├── Teacher.java
│   │   │       └── User.java
│   │   └── resources/
│   │       ├── templates/                 ← Vistas en Mustache (Formularios, Listados, Perfiles)
│   │       └── database.properties        ← Configuración de la BD
│   └── test/
│       └── java/com/is1/proyecto/
│           └── AppTest.java               ← Tests
│
├── src/migrations/                        ← Migraciones de esquema
│
├── db/
│   └── dev.db                             ← Base de datos local (ignorada por Git)
│
├── docs/                                  ← Documentación y requerimientos
│   ├── API_REST.md                        ← Detalle de endpoints
│   ├── Backlog.md                         ← Backlog del proyecto
│   ├── diagrama_de_clases.md              ← Diagrama UML
│   ├── Guia_Carga_Manual.md               ← Guía para cargar datos (NUEVO)
│   ├── MANEJO_ERRORES_MUSTACHE.md         
│   ├── Requirements.md                    
│   └── SQA.md                             ← Criterios de aceptación
│
├── seed.sql                               ← Script opcional para cargar datos iniciales
├── MIGRACIONES.md                         ← Guía de migraciones
├── pom.xml                                ← Configuración de Maven
└── README.md                              ← Este archivo
```

---

## Cómo agregar un cambio a la base de datos

Cada vez que necesitás crear o modificar una tabla, usá el sistema de migraciones.

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
./run.sh
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
| `docs/API_REST.md` | Detalle de los endpoints de la API web |
| `docs/Backlog.md` | Backlog con todas las tareas |
| `docs/diagrama_de_clases.md` | Diagrama UML del sistema |
| `docs/Guia_Carga_Manual.md` | Paso a paso para cargar datos al sistema (restricciones y orden) |
| `docs/MANEJO_ERRORES_MUSTACHE.md` | Convenciones para renderizar errores en las vistas Mustache |
| `docs/Practico_2_Documentacion.pdf` | Documentación formal del práctico (PDF) |
| `docs/Requirements.md` | Descripción del proyecto y análisis de riesgos |
| `docs/SQA.md` | Criterios de aceptación por tarea |
| `MIGRACIONES.md` | Guía completa para manejar la base de datos y migraciones |

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
