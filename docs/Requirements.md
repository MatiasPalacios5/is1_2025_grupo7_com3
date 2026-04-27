# Proyecto Integrador — Ingeniería de Software II
**Año 2026 — Grupo 14 | Comisión 3**

---

# Ejercicio 1


## Problema que se quiere resolver

Luego de la reunión que tuvimos con el cliente, nuestro objetivo es que el nuevo sistema creado busque centralizar y optimizar la gestión administrativa de la información académica, permitiendo administrar alumnos, profesores y materias de manera eficiente, con control de acceso por roles, minimizando la complejidad para administrar los datos.

---

## Usuarios del sistema

 **Administrador:** Identificamos como usuario principal al administrador, que tendrá acceso completo al sistema: alta/baja/modificación de alumnos, profesores y materias, asignación de docentes y generación de reportes.
 **Usuario estándar:** También contemplamos un usuario con acceso restringido según los permisos asignados, que podrá consultar información pero no modificarla.

---

## Funcionalidades principales

- **Autenticación y sesiones:** implementamos login/logout con contraseñas hasheadas (BCrypt) y manejo de sesiones por usuario.
- **Gestión de usuarios:** desarrollamos el registro y administración de cuentas con control de acceso.
- **Gestión de profesores:** incorporamos el alta, baja y modificación de profesores, con datos personales (nombre, apellido, DNI, email, carrera) y validación de duplicados por DNI y email.
- **Gestión de alumnos:** incluimos el alta, baja y modificación de alumnos.
- **Gestión de materias/cursos:** agregamos la creación y administración de materias.
- **Asignación de profesores a materias:** permitimos la vinculación entre docentes y cursos.
- **Roles y permisos:** diferenciamos el acceso según el rol del usuario autenticado.
- **API REST:** exponemos endpoints JSON para integración con otros sistemas o clientes.
- **Reportes y listados:** generamos reportes sobre el estado académico del sistema.

---

## Restricciones técnicas

- **Roles de usuario:** diferenciamos tres tipos de usuarios: administrativo, docente y estudiante, cada uno con accesos distintos.
- **Disponibilidad 24/7:** nos comprometemos a que el sistema esté disponible las 24 horas del día.
- **Seguridad en contraseñas:** exigimos contraseñas seguras para proteger los datos personales de los usuarios.
- **Compatibilidad con navegadores:** garantizamos el funcionamiento correcto en los navegadores más utilizados (Chrome, Firefox, etc.).
- **Java 11:** compilamos y configuramos el sistema para correr en Java 11, por lo que no podemos usar features de versiones más nuevas.
- **SQLite como base de datos:** al elegir un archivo local como base de datos, asumimos que no soporta bien muchos usuarios conectados al mismo tiempo.
- **ActiveJDBC como ORM:** debemos respetar un paso especial de compilación que no puede omitirse.
- **Mustache como motor de plantillas:** aceptamos que no nos permite lógica compleja en las vistas.
- **Sin sistema de migraciones:** gestionamos los cambios en la base de datos manualmente con `scheme.sql`.

---

## Tamaño del equipo

Somos un equipo compuesto por 5 integrantes.

---

## Tecnologías elegidas y justificación

| Tecnología | Rol | Justificación |
|---|---|---|
| **Java 11** | Lenguaje principal | Lo elegimos por el conocimiento previo del equipo; su tipado estático y ecosistema maduro nos dan seguridad. |
| **Spark Java 2.9.4** | Framework web | Lo adoptamos por ser ligero y sin configuración XML, ideal para proyectos académicos de mediano tamaño. |
| **SQLite 3.45.1** | Base de datos | Lo usamos porque no necesita servidor externo y es fácil de configurar en nuestros entornos de desarrollo. |
| **ActiveJDBC 3.4-j11** | ORM | Lo incorporamos por su compatibilidad con Java 11 y su sintaxis simple para operaciones CRUD. |
| **Mustache** | Motor de plantillas | Lo elegimos por su lógica mínima en las vistas, lo que nos permite una separación clara entre presentación y lógica de negocio. |
| **Jackson 2.17.1** | Serialización JSON | Lo utilizamos por ser el estándar de la industria para APIs REST en Java. |
| **BCrypt (jbcrypt 0.4)** | Hash de contraseñas | Lo adoptamos por ser un algoritmo seguro y ampliamente utilizado para el almacenamiento de credenciales. |
| **JUnit Jupiter 5.10.0** | Testing | Lo incorporamos por ser un framework moderno para pruebas unitarias en Java. |
| **Maven** | Gestión de dependencias y build | Lo elegimos por ser la convención estándar y facilitar la integración con CI. |
| **Tailwind CSS (CDN)** | Estilos frontend | Lo usamos por sus utilidades CSS rápidas sin necesidad de un pipeline de frontend. |

---

## Plazo estimado

Desarrollamos el proyecto a lo largo del cuatrimestre académico 2026.

---

## Problemas encontrados

- **Configuración del entorno de compilación:** una de las librerías que usamos para conectarnos a la base de datos necesita un paso especial durante la compilación. Al principio eso rompía el proceso de construcción del proyecto y tuvimos que investigar cómo configurarlo correctamente.
- **Actualización manual de la base de datos:** cada vez que alguien cambia la estructura de la base de datos (por ejemplo, agrega una columna), tiene que avisarle manualmente al resto del equipo para que actualicen su copia. Esto puede generar errores si alguien se olvida o lo hace de forma distinta.
- **Limitaciones para mostrar errores en las vistas:** el sistema de plantillas que usamos para las páginas web es muy básico y no permite mucha lógica. Para mostrar mensajes de error específicos (como "el DNI ya existe" o "el email ya está registrado") tuvimos que escribir código extra en el servidor, lo que complica un poco el mantenimiento.
- **Base de datos no apta para muchos usuarios simultáneos:** la base de datos que elegimos guarda todo en un único archivo. Eso está bien para desarrollar, pero cuando varias personas intentan usarla al mismo tiempo puede generar conflictos o errores.
- **Falta de pruebas automatizadas:** por ahora el proyecto tiene un único test que no prueba nada real. Eso significa que si alguien rompe algo sin querer, el sistema no lo va a detectar automáticamente. Es algo que el equipo tiene pendiente mejorar.
- **Falta de funcionalidades principales** el proyecto cuenta con solo las funcionalidades de acceso y alta de un docente, por lo que restarían completar otras características principales.
---

## Forma de organización del equipo

El grupo se dividió las tareas simples para agilizar el trabajo y reducir el tiempo de finalización del proyecto, pero las actividades mas complejas se realizaron entre todos los integrantes para obtener diferentes puntos de vista y poder llevar a cabo un mejor trabajo. 

- **Repositorio compartido en GitHub** con control de versiones por ramas.
- **Backlog en GitHub Projects** con issues clasificados por tipo, prioridad y responsable.
- **Entornos (Discord)** reuniones periódicas para el avance del proyecto.

---


# Ejercicio 2


## Clasificación de riesgos

### Riesgos Técnicos

| # | Descripción | Probabilidad | Impacto | Identificado por |
|---|---|---|---|---|
| RT-01 | La base de datos SQLite no está pensada para soportar muchos usuarios al mismo tiempo. Si varias personas usan el sistema a la vez, puede generar errores o pérdida de datos. | Alta | Alto | Equipo |
| RT-02 | El proyecto casi no tiene pruebas automatizadas. Si alguien modifica algo sin querer, nadie va a detectarlo hasta que el error ya esté en el sistema. | Alta | Alto | Equipo |
| RT-03 | La librería ActiveJDBC tiene poca documentación y es poco conocida. Si aparece un problema complejo, va a ser difícil encontrar soluciones en internet. | Media | Alto | Equipo |
| RT-04 | El motor de plantillas Mustache es muy limitado. A medida que el sistema crezca y se necesite mostrar información más compleja, esto puede volverse un obstáculo importante. | Media | Medio | IA |
| RT-05 | No tienen un sistema automático para actualizar la base de datos. Si el equipo crece o se incorporan cambios frecuentes al esquema, el riesgo de que alguien trabaje con una versión desactualizada es alto. | Alta | Medio | IA |
| RT-06 | Muchas funcionalidades principales todavía no están desarrolladas. Si el ritmo de avance no aumenta, es probable que no se llegue a terminar todo en el plazo del cuatrimestre. | Alta | Crítico | IA |
| RT-07 | El sistema no tiene un entorno de pruebas separado del entorno de desarrollo. Cualquier cambio que se haga puede afectar directamente la versión que se usa para trabajar. | Media | Medio | IA | 

---

### Riesgos Organizacionales

| # | Descripción | Probabilidad | Impacto | Identificado por |
|---|---|---|---|---|
| RO-01 | Al dividirnos las tareas entre los integrantes, puede pasar que dos personas trabajen sobre el mismo archivo al mismo tiempo y se generen conflictos en el código, esto requiere una comunicacion "critica". | Media | Medio | Equipo |
| RO-02 | No tenemos definido quién es el responsable de cada área del proyecto. Si no hay un rol definido, algunas tareas pueden no implementarse ya que no tienen una persona asignada. | Media | Alto | Equipo |
| RO-03 | Las decisiones importantes se toman entre todos. Una de las desventajas es que el equipo tarde mucho en ponerse de acuerdo y se pierda tiempo. | Media | Medio | Equipo |
| RO-04 | El backlog en GitHub Projects puede quedar desactualizado si el equipo no lo mantiene con disciplina. Eso hace que nadie sepa realmente en qué estado está el proyecto. | Alta | Alto | IA |
| RO-05 | La comunicación se hace por Discord, lo que es informal. Decisiones importantes pueden perderse en el chat o no quedar registradas en ningún lado. | Media | Medio | IA |

---

### Riesgos de Planificación

| # | Descripción | Probabilidad | Impacto | Identificado por |
|---|---|---|---|---|
| RP-01 | El proyecto tiene muchas funcionalidades por desarrollar y el plazo es el cuatrimestre. Si las estimaciones no son precisas, es muy probable que no lleguemos a terminar todo a tiempo. | Alta | Crítico | Equipo |
| RP-02 | Las tareas complejas las realizamos entre todos los integrantes. Si no las planificamos bien, puede ser difícil coordinar y esas tareas pueden retrasarse. | Media | Alto | Equipo |
| RP-03 | No hay fechas intermedias definidas para cada funcionalidad. Sin hitos claros, es difícil darse cuenta a tiempo si el proyecto está atrasado. | Alta | Alto | IA |
| RP-04 | Durante el cuatrimestre hay parciales, entregas de otras materias y obligaciones académicas que pueden reducir el tiempo disponible del equipo de forma imprevista. | Alta | Alto | IA |
| RP-05 | El alcance del proyecto es amplio (alumnos, profesores, materias, reportes, API, roles). Si no se prioriza bien, el equipo puede gastar tiempo en detalles y dejar sin hacer lo más importante. | Media | Alto | IA |

---

### Riesgos Humanos

| # | Descripción | Probabilidad | Impacto | Identificado por |
|---|---|---|---|---|
| RH-01 | Si uno de los integrantes se desvincula del proyecto ya sea de manera temporal o definitiva, el equipo tiene que asumir sus tareas. Generando mas responsabilidades en un mismo tiempo/plazo establecido. | Baja | Crítico | Equipo |
| RH-02 | Algunos integrantes pueden tener más conocimiento técnico que otros. Si las tareas no se distribuyen teniendo eso en cuenta, algunos pueden quedar bloqueados sin poder avanzar. | Media | Alto | IA |
| RH-03 | El equipo no tiene experiencia previa con todas las tecnologías usadas (ActiveJDBC, Spark Java). Aprender mientras se desarrolla consume tiempo y puede generar errores. | Alta | Alto | IA |
| RH-04 | Si el equipo no se comunica bien, puede pasar que alguien desarrolle algo que otro ya hizo, o que haya decisiones tomadas que no todos conocen. | Media | Medio | Equipo |
| RH-05 | La motivación del equipo puede bajar en las etapas finales del cuatrimestre, cuando hay más presión académica y el proyecto todavía tiene mucho por hacer. | Media | Alto | IA |

---
