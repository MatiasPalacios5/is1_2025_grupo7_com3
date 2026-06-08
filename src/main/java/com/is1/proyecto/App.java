package com.is1.proyecto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Person;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User;
import com.is1.proyecto.models.Subject;
import com.is1.proyecto.models.AcademicPeriod;
import com.is1.proyecto.models.PeriodTeacherSubject;
import com.is1.proyecto.models.Career;
import com.is1.proyecto.models.StudentCareer;
import com.is1.proyecto.models.StudyPlan;
import com.is1.proyecto.models.TakenExam;
import com.is1.proyecto.models.Prerequisite;

import spark.ModelAndView;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;

public class App {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        port(8080);

        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // Lista de rutas que NO requieren autenticación
        List<String> rutasPublicas = List.of("/", "/login", "/user/new", "/user/create", "/add_users", "/api/estudiantes", "/api/profesores", "/api/materias", "/api/reportes/resumen");

        // ==============================================================================
        // MIDDLEWARES (Filtros de Petición)
        // ==============================================================================
        // Este filtro se ejecuta ANTES de procesar cualquier ruta.
        // Se encarga de abrir la conexión a la base de datos si no está abierta
        // y de verificar si el usuario tiene una sesión activa para acceder a rutas protegidas.
        before((req, res) -> {
            try {
                if (!Base.hasConnection()) {
                    Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                }
                System.out.println(req.url());

                // Verificar sesión en rutas protegidas
                String path = req.pathInfo();
                boolean esRutaPublica = rutasPublicas.stream().anyMatch(path::equals);

                if (!esRutaPublica) {
                    Boolean loggedIn = req.session().attribute("loggedIn");
                    if (loggedIn == null || !loggedIn) {
                        res.redirect("/login?error=" + URLEncoder.encode(
                            "Debes iniciar sesión para acceder.", StandardCharsets.UTF_8));
                        halt();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al abrir conexión con ActiveJDBC: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor.\"}");
            }
        });
        // Este filtro se ejecuta DESPUÉS de procesar cualquier ruta.
        // Se asegura de cerrar la conexión a la base de datos para no dejar conexiones colgadas.
        after((req, res) -> {
            try {
                Base.close();
            } catch (Exception e) {
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });

        // ==============================================================================
        // RUTAS DE USUARIOS Y AUTENTICACIÓN (LOGIN/LOGOUT)
        // ==============================================================================

        // Muestra el formulario para crear un nuevo usuario (generalmente administrador).
        get("/user/create", (req, res) -> {
            // req: el usuario solicita la vista para crear un administrador
            Map<String, Object> model = new HashMap<>();

            // Extraemos los posibles mensajes de éxito o error de la URL
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // res: devolvemos la vista del formulario
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine());

        get("/dashboard", (req, res) -> {
            // req: el usuario quiere acceder al panel principal
            Map<String, Object> model = new HashMap<>();

            // Obtenemos el nombre del usuario desde la sesión actual
            String currentUsername = req.session().attribute("currentUserUsername");
            model.put("username", currentUsername);

            // res: renderizamos el dashboard con los datos del usuario
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine());

        get("/logout", (req, res) -> {
            // req: el usuario solicita cerrar sesión
            // Destruimos la sesión actual en el servidor
            req.session().invalidate();
            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");
            
            // res: redirigimos al inicio de sesión
            res.redirect("/");
            return null;
        });

        get("/", (req, res) -> {
            // req: petición a la raíz de la aplicación (página de login)
            Map<String, Object> model = new HashMap<>();
            
            // Capturamos errores o mensajes de la URL para mostrarlos
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            
            // res: renderizamos la vista de login
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());

        get("/user/new", (req, res) -> {
            // req: solicitud para ver la página vacía de nuevo usuario
            // res: enviamos el formulario
            return new ModelAndView(new HashMap<>(), "user_form.mustache");
        }, new MustacheTemplateEngine());

        post("/user/new", (req, res) -> {
            // req: el usuario envía el formulario para crear un nuevo registro
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // Validamos que los campos no estén vacíos
            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                res.redirect("/user/create?error="
                        + URLEncoder.encode("Nombre y contraseña son requeridos.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Creamos un nuevo usuario en la base de datos
                User ac = new User();
                // Encriptamos la contraseña por seguridad
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                ac.set("name", name);
                ac.set("password", hashedPassword);
                ac.saveIt();

                // res: redirigimos con éxito
                res.status(201);
                res.redirect("/user/create?message="
                        + URLEncoder.encode("Cuenta creada exitosamente para " + name + "!", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                System.err.println("Error al registrar la cuenta: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                res.redirect("/user/create?error=" + URLEncoder
                        .encode("Error interno al crear la cuenta. Intente de nuevo.", StandardCharsets.UTF_8));
                return "";
            }
        });

        post("/login", (req, res) -> {
            // req: intento de inicio de sesión con credenciales
            Map<String, Object> model = new HashMap<>();

            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");

            // Validamos que haya ingresado datos
            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.status(400);
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache");
            }

            // Buscamos al usuario en la BD
            User ac = User.findFirst("name = ?", username);

            if (ac == null) {
                res.status(401);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                return new ModelAndView(model, "login.mustache");
            }

            String storedHashedPassword = ac.getString("password");

            // Comparamos la contraseña en texto plano con la encriptada
            if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
                res.status(200);

                // Guardamos datos en la sesión para mantenerlo logueado
                req.session(true).attribute("currentUserUsername", username);
                req.session().attribute("userId", ac.getId());
                req.session().attribute("loggedIn", true);

                System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
                System.out.println("DEBUG: ID de Sesión: " + req.session().id());

                model.put("username", username);
                // res: enviamos a la pantalla principal
                return new ModelAndView(model, "dashboard.mustache");
            } else {
                res.status(401);
                System.out.println("DEBUG: Intento de login fallido para: " + username);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                // res: devolvemos al login con error
                return new ModelAndView(model, "login.mustache");
            }
        }, new MustacheTemplateEngine());

        post("/add_users", (req, res) -> {
            // req: petición API para agregar usuarios externamente
            // Esta ruta responde con JSON en lugar de HTML
            res.type("application/json");

            String name = req.queryParams("name");
            String password = req.queryParams("password");

            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
            }

            try {
                // Guardamos el usuario en BD
                User newUser = new User();
                newUser.set("name", name);
                newUser.set("password", password); // Nota: esta ruta API no está encriptando, podría ser un bug de seguridad
                newUser.saveIt();

                res.status(201);
                // res: enviamos respuesta JSON exitosa
                return objectMapper.writeValueAsString(
                        Map.of("message", "Usuario '" + name + "' registrado con éxito.", "id", newUser.getId()));

            } catch (Exception e) {
                System.err.println("Error al registrar usuario: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return objectMapper
                        .writeValueAsString(Map.of("error", "Error interno al registrar usuario: " + e.getMessage()));
            }
        });

        // ==============================================================================
        // RUTAS DE PROFESORES
        // Aquí se manejan todas las operaciones CRUD (Crear, Leer, Actualizar, Borrar)
        // relacionadas con los Profesores de la institución.
        // ==============================================================================

        get("/profesor/create", (req, res) -> {
            // req: el usuario solicita el formulario para cargar un profesor
            Map<String, Object> model = new HashMap<>();

            // Extraemos los posibles mensajes desde la URL (éxito o errores de validación)
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            String errorEmail = req.queryParams("errorEmail");
            if (errorEmail != null && !errorEmail.isEmpty()) {
                model.put("errorEmail", errorEmail);
            }

            String errorDni = req.queryParams("errorDni");
            if (errorDni != null && !errorDni.isEmpty()) {
                model.put("errorDni", errorDni);
            }

            // res: renderizamos el formulario (teacher_form.mustache)
            return new ModelAndView(model, "teacher_form.mustache");
        }, new MustacheTemplateEngine());

        post("/profesor/new", (req, res) -> {
            // req: se envían los datos del formulario para guardar un nuevo profesor
            // Obtenemos los campos del formulario
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String career = req.queryParams("career");
            String email = req.queryParams("email");

            // Validamos que ningún campo esté vacío
            if (nombre == null || nombre.isEmpty() ||
                    apellido == null || apellido.isEmpty() ||
                    dni == null || dni.isEmpty() ||
                    career == null || career.isEmpty() ||
                    email == null || email.isEmpty()) {

                res.status(400);
                res.redirect("/profesor/create?error="
                        + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            Integer dniInt;
            try {
                // Verificamos que el DNI sea numérico
                dniInt = Integer.parseInt(dni);
            } catch (NumberFormatException e) {
                res.status(400);
                res.redirect("/profesor/create?error="
                        + URLEncoder.encode("El DNI debe contener solo números.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Verificamos que no exista otra persona con el mismo DNI
                Person existingPerson = Person.findFirst("dni = ?", dniInt);
                if (existingPerson != null) {
                    System.out.println("DEBUG: DNI duplicado encontrado: " + dniInt);
                    res.status(400);
                    res.redirect("/profesor/create?errorDni="
                            + URLEncoder.encode("El DNI ya está registrado en el sistema.", StandardCharsets.UTF_8));
                    return "";
                }

                // Verificamos que el email no esté en uso por otro profesor
                Teacher existingTeacher = Teacher.findFirst("email = ?", email);
                if (existingTeacher != null) {
                    System.out.println("DEBUG: Email duplicado encontrado: " + email);
                    res.status(400);
                    res.redirect("/profesor/create?errorEmail="
                            + URLEncoder.encode("El email introducido ya existe.", StandardCharsets.UTF_8));
                    return "";
                }

                // Primero creamos el registro de la Persona
                Person newPerson = new Person();
                newPerson.set("dni", dniInt);
                newPerson.set("name", nombre);
                newPerson.set("apellido", apellido);
                newPerson.saveIt();

                System.out.println("DEBUG: Person creada con ID: " + newPerson.getId());

                Integer personId = newPerson.getInteger("id");

                // Luego creamos el registro del Profesor vinculado a la Persona
                Teacher newTeacher = new Teacher();
                newTeacher.set("id_person", personId);
                newTeacher.set("career", career);
                newTeacher.set("email", email);
                newTeacher.saveIt();

                System.out.println("DEBUG: Teacher creado exitosamente");

                // res: si todo fue bien, volvemos al formulario con un mensaje de éxito
                res.status(201);
                res.redirect("/profesor/create?message=" + URLEncoder.encode(
                        "Profesor " + nombre + " " + apellido + " registrado exitosamente!", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                System.err.println("ERROR COMPLETO al registrar profesor: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                res.redirect("/profesor/create?error="
                        + URLEncoder.encode("Error interno al registrar el profesor.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/profesor/list", (req, res) -> {
            // req: petición para ver la tabla de todos los profesores
            Map<String, Object> model = new HashMap<>();

            // Mensaje de éxito si lo hay
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Verificamos si hay un término de búsqueda (query string 'q')
            String q = req.queryParams("q");
            if (q != null && !q.isEmpty()) {
                model.put("searchQuery", q);
            }

            List<Teacher> teachers;
            // Si hay búsqueda, usamos SQL para filtrar por apellido, nombre o DNI
            if (q != null && !q.isEmpty()) {
                teachers = Teacher.findBySQL(
                    "SELECT teachers.* FROM teachers " +
                    "JOIN persons ON teachers.id_person = persons.id " +
                    "WHERE persons.apellido LIKE ? OR persons.name LIKE ? OR CAST(persons.dni AS TEXT) LIKE ?", 
                    "%" + q + "%", "%" + q + "%", "%" + q + "%"
                );
            } else {
                // Si no, traemos a todos
                teachers = Teacher.findAll();
            }
            
            // Convertimos la lista de modelos de BD a una lista de mapas para la vista
            List<Map<String, Object>> teachersModel = new ArrayList<>();
            for (Teacher t : teachers) {
                Map<String, Object> tMap = new HashMap<>();
                tMap.put("id", t.getId());
                tMap.put("name", t.getName()); // Usando métodos helper definidos en el modelo Teacher
                tMap.put("apellido", t.getApellido());
                tMap.put("dni", t.getDni());
                tMap.put("email", t.getEmail());
                tMap.put("career", t.getCareer());
                teachersModel.add(tMap);
            }
            model.put("teachers", teachersModel);
            
            // res: renderiza el listado
            return new ModelAndView(model, "teacher_list.mustache");
        }, new MustacheTemplateEngine());

        post("/profesor/delete/:id", (req, res) -> {
            // req: petición para eliminar un profesor según su ID
            try {
                // Parseamos el ID desde la URL
                Integer id = Integer.parseInt(req.params(":id"));
                Teacher teacher = Teacher.findById(id);
                if (teacher != null) {
                    // Eliminamos primero las asignaciones que tenga
                    org.javalite.activejdbc.Base.exec("DELETE FROM period_teacher_subject WHERE id_teacher = ?", teacher.getId());
                    
                    Integer idPerson = teacher.getIdPerson();
                    // Borramos el registro del profesor
                    teacher.delete();
                    
                    // Borramos el registro de la persona base
                    Person person = Person.findById(idPerson);
                    if (person != null) person.delete();
                }
                // res: redirige de vuelta a la lista con un mensaje de éxito
                res.redirect("/profesor/list?message=" +
                    URLEncoder.encode("Profesor eliminado correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Si falla, redirige con error
                res.redirect("/profesor/list?error=" +
                    URLEncoder.encode("Error al eliminar el profesor.", StandardCharsets.UTF_8));
            }
            return "";
        });

        // ==============================================================================
        // RUTAS DE ESTUDIANTES
        // Maneja la información de los alumnos, incluyendo su situación (Ingresante/Efectivo)
        // y a qué carreras están inscriptos.
        // ==============================================================================

        get("/estudiante/perfil/:id", (req, res) -> {
            // req: petición para ver el perfil detallado de un estudiante
            Map<String, Object> model = new HashMap<>();
            Student s = Student.findById(req.params("id"));
            if (s == null) {
                res.redirect("/estudiante/list?error=" + URLEncoder.encode("Estudiante no encontrado.", StandardCharsets.UTF_8));
                return null;
            }

            // Pasamos los datos básicos a la vista
            model.put("name", s.getName());
            model.put("apellido", s.getApellido());
            model.put("dni", s.getDni());
            model.put("legajo", s.getLegajo());
            model.put("situacion", s.getSituacion());

            // Consultamos las carreras a las que está inscrito usando SQL puro
            List<Map> scRows = org.javalite.activejdbc.Base.findAll("SELECT c.nombre FROM careers c JOIN student_careers sc ON c.id = sc.id_career WHERE sc.id_student = ?", s.getId());
            List<String> careerNames = new ArrayList<>();
            for (Map row : scRows) {
                careerNames.add(row.get("nombre").toString());
            }
            model.put("carreras", String.join(", ", careerNames));

            // Obtenemos los exámenes rendidos por el estudiante, ordenados por fecha
            List<TakenExam> examenes = TakenExam.where("id_student = ?", s.getId()).orderBy("fecha DESC");
            List<Map<String, Object>> examenesModel = new ArrayList<>();
            for (TakenExam exam : examenes) {
                Map<String, Object> map = new HashMap<>();
                map.put("subjectName", exam.getSubjectName());
                if (exam.getFecha() != null) map.put("fecha", exam.getFecha().toString());
                map.put("nota", exam.getNota());
                examenesModel.add(map);
            }
            model.put("examenes", examenesModel);

            // res: renderiza la vista de perfil de estudiante
            return new ModelAndView(model, "student_profile.mustache");
        }, new MustacheTemplateEngine());

        get("/estudiante/create", (req, res) -> {
            // req: petición para ver el formulario de alta de un estudiante
            Map<String, Object> model = new HashMap<>();

            // Procesamos los mensajes de error/éxito que vengan en la URL
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            String errorDni = req.queryParams("errorDni");
            if (errorDni != null && !errorDni.isEmpty()) {
                model.put("errorDni", errorDni);
            }

            String errorLegajo = req.queryParams("errorLegajo");
            if (errorLegajo != null && !errorLegajo.isEmpty()) {
                model.put("errorLegajo", errorLegajo);
            }

            // Cargamos la lista de carreras para que se puedan seleccionar en el formulario
            model.put("careers", Career.findAll());

            // res: mostramos el formulario
            return new ModelAndView(model, "student_form.mustache");
        }, new MustacheTemplateEngine());

        post("/estudiante/new", (req, res) -> {
            // req: datos enviados desde el formulario para crear un estudiante
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String legajo = req.queryParams("legajo");
            String situacion = req.queryParams("situacion");
            // Un estudiante puede anotarse a múltiples carreras (checkboxes)
            String[] idCareersStr = req.queryParamsValues("id_careers");

            // Validamos que ningún dato esencial esté faltando
            if (nombre == null || nombre.isEmpty() ||
                apellido == null || apellido.isEmpty() ||
                dni == null || dni.isEmpty() ||
                legajo == null || legajo.isEmpty() ||
                situacion == null || situacion.isEmpty() ||
                idCareersStr == null || idCareersStr.length == 0) {

                res.status(400);
                res.redirect("/estudiante/create?error=" + URLEncoder.encode("Todos los campos (incluyendo al menos una carrera) son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            if (!situacion.equalsIgnoreCase("ingresante") && !situacion.equalsIgnoreCase("efectivo")) {
                res.status(400);
                res.redirect("/estudiante/create?error=" + URLEncoder.encode("La situación debe ser 'ingresante' o 'efectivo'.", StandardCharsets.UTF_8));
                return "";
            }

            Integer dniInt;
            try {
                // Comprobamos que el DNI sea numérico
                dniInt = Integer.parseInt(dni);
            } catch (NumberFormatException e) {
                res.status(400);
                res.redirect("/estudiante/create?error=" + URLEncoder.encode("El DNI debe contener solo números.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Comprobamos que no haya otro registro con el mismo DNI
                Person existingPerson = Person.findFirst("dni = ?", dniInt);
                if (existingPerson != null) {
                    res.status(400);
                    res.redirect("/estudiante/create?errorDni=" + URLEncoder.encode("El DNI ya está registrado en el sistema.", StandardCharsets.UTF_8));
                    return "";
                }

                // Comprobamos que no exista un estudiante con ese mismo Legajo
                Student existingStudent = Student.findFirst("legajo = ?", legajo);
                if (existingStudent != null) {
                    res.status(400);
                    res.redirect("/estudiante/create?errorLegajo=" + URLEncoder.encode("El legajo ya está registrado en el sistema.", StandardCharsets.UTF_8));
                    return "";
                }

                // Guardamos los datos de la Persona primero
                Person newPerson = new Person();
                newPerson.set("dni", dniInt);
                newPerson.set("name", nombre);
                newPerson.set("apellido", apellido);
                newPerson.saveIt();

                Integer personId = newPerson.getInteger("id");

                // Luego creamos el Estudiante, atado a esa Persona
                Student newStudent = new Student();
                newStudent.set("id_person", personId);
                newStudent.set("legajo", legajo);
                newStudent.set("situacion", situacion);
                newStudent.saveIt();

                // Finalmente, guardamos las inscripciones a cada carrera elegida
                for (String idC : idCareersStr) {
                    StudentCareer studentCareer = new StudentCareer();
                    studentCareer.set("id_student", newStudent.getId());
                    studentCareer.set("id_career", Integer.parseInt(idC));
                    studentCareer.saveIt();
                }

                // res: si todo sale bien, redirigimos mostrando un éxito
                res.status(201);
                res.redirect("/estudiante/create?message=" + URLEncoder.encode("Estudiante " + nombre + " " + apellido + " registrado exitosamente!", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                System.err.println("ERROR al registrar estudiante: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                res.redirect("/estudiante/create?error=" + URLEncoder.encode("Error interno al registrar el estudiante.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/estudiante/list", (req, res) -> {
            // req: petición para mostrar la tabla con todos los estudiantes
            Map<String, Object> model = new HashMap<>();

            // Extraemos mensajes informativos de la URL
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Capturamos el término de búsqueda si el usuario usó la barra de búsqueda
            String q = req.queryParams("q");
            if (q != null && !q.isEmpty()) {
                model.put("searchQuery", q);
            }

            List<Student> students;
            if (q != null && !q.isEmpty()) {
                // Buscamos estudiantes cuyo legajo, apellido, nombre o dni coincida con el texto
                students = Student.findBySQL(
                    "SELECT students.* FROM students " +
                    "JOIN persons ON students.id_person = persons.id " +
                    "WHERE students.legajo LIKE ? OR persons.apellido LIKE ? OR persons.name LIKE ? OR CAST(persons.dni AS TEXT) LIKE ?", 
                    "%" + q + "%", "%" + q + "%", "%" + q + "%", "%" + q + "%"
                );
            } else {
                // Si no hay búsqueda, traemos a todos
                students = Student.findAll();
            }

            // Convertimos los resultados a un formato amigable para la vista Mustache
            List<Map<String, Object>> studentsModel = new ArrayList<>();
            for (Student s : students) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("legajo", s.getLegajo());
                sMap.put("situacion", s.getSituacion());
                sMap.put("name", s.getName());
                sMap.put("apellido", s.getApellido());
                sMap.put("dni", s.getDni());
                
                // Buscamos a qué carreras está inscripto este estudiante para mostrarlo en la tabla
                List<Map> scRows = org.javalite.activejdbc.Base.findAll("SELECT c.nombre FROM careers c JOIN student_careers sc ON c.id = sc.id_career WHERE sc.id_student = ?", s.getId());
                List<String> careerNames = new ArrayList<>();
                for (Map row : scRows) {
                    careerNames.add(row.get("nombre").toString());
                }
                sMap.put("carreras", String.join(", ", careerNames));
                studentsModel.add(sMap);
            }

            model.put("students", studentsModel);
            // res: renderiza el listado
            return new ModelAndView(model, "student_list.mustache");
        }, new MustacheTemplateEngine());

        get("/estudiante/edit/:id", (req, res) -> {
            // req: petición para cargar el formulario de edición de un estudiante
            Map<String, Object> model = new HashMap<>();

            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);

            String idStr = req.params(":id");
            // Buscamos el estudiante por su ID
            Student student = Student.findById(Integer.parseInt(idStr));
            if (student == null) {
                res.redirect("/estudiante/list?error=" + URLEncoder.encode("Estudiante no encontrado.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("student", student);
            
            // Configuramos qué botón de radio debe estar marcado por defecto según su situación
            if ("Ingresante".equals(student.getSituacion())) {
                model.put("isIngresante", true);
            } else if ("Efectivo".equals(student.getSituacion())) {
                model.put("isEfectivo", true);
            }

            // Buscamos las carreras actuales en las que está inscripto
            List<StudentCareer> studentCareers = StudentCareer.find("id_student = ?", student.getId());
            List<Integer> currentCareerIds = new ArrayList<>();
            for (StudentCareer sc : studentCareers) {
                currentCareerIds.add(sc.getInteger("id_career"));
            }

            // Cargamos todas las carreras posibles
            List<Career> careers = Career.findAll();
            List<Map<String, Object>> careersModel = new ArrayList<>();
            for (Career c : careers) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("nombre", c.getString("nombre"));
                cMap.put("codigo", c.getString("codigo"));
                // Marcamos como 'selected' las carreras en las que ya estaba inscripto
                if (currentCareerIds.contains(Integer.parseInt(c.getId().toString()))) {
                    cMap.put("selected", true);
                }
                careersModel.add(cMap);
            }
            model.put("careers", careersModel);

            // res: renderizamos el formulario reutilizable con los datos pre-cargados
            return new ModelAndView(model, "student_form.mustache");
        }, new MustacheTemplateEngine());

        post("/estudiante/update/:id", (req, res) -> {
            // req: petición para actualizar los datos de un estudiante existente
            String idStr = req.params(":id");
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String legajo = req.queryParams("legajo");
            String situacion = req.queryParams("situacion");
            String[] idCareersStr = req.queryParamsValues("id_careers");

            // Validamos campos obligatorios
            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() || dni == null || dni.isEmpty() || legajo == null || legajo.isEmpty() || situacion == null || situacion.isEmpty() || idCareersStr == null || idCareersStr.length == 0) {
                res.redirect("/estudiante/edit/" + idStr + "?error=" + URLEncoder.encode("Todos los campos y al menos una carrera son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            // Validamos que la situación sea válida
            if (!situacion.equalsIgnoreCase("ingresante") && !situacion.equalsIgnoreCase("efectivo")) {
                res.redirect("/estudiante/edit/" + idStr + "?error=" + URLEncoder.encode("La situación debe ser 'ingresante' o 'efectivo'.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Buscamos el estudiante a modificar
                Student student = Student.findById(Integer.parseInt(idStr));
                if (student != null) {
                    // Actualizamos primero la persona asociada
                    Person person = Person.findById(student.getIdPerson());
                    if (person != null) {
                        person.set("name", nombre);
                        person.set("apellido", apellido);
                        person.set("dni", Integer.parseInt(dni));
                        person.saveIt();
                    }
                    // Actualizamos el estudiante
                    student.set("legajo", legajo);
                    student.set("situacion", situacion);
                    student.saveIt();

                    // Para las carreras, borramos todas las que tenía e insertamos las nuevas (reemplazo completo)
                    org.javalite.activejdbc.Base.exec("DELETE FROM student_careers WHERE id_student = ?", student.getId());
                    for (String idC : idCareersStr) {
                        StudentCareer studentCareer = new StudentCareer();
                        studentCareer.set("id_student", student.getId());
                        studentCareer.set("id_career", Integer.parseInt(idC));
                        studentCareer.saveIt();
                    }

                    // res: redirigimos a la tabla de alumnos con éxito
                    res.redirect("/estudiante/list?message=" + URLEncoder.encode("Estudiante actualizado correctamente.", StandardCharsets.UTF_8));
                } else {
                    res.redirect("/estudiante/list?error=" + URLEncoder.encode("Estudiante no encontrado.", StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                res.redirect("/estudiante/edit/" + idStr + "?error=" + URLEncoder.encode("Error al actualizar.", StandardCharsets.UTF_8));
            }
            return "";
        });

        post("/estudiante/delete/:id", (req, res) -> {
            // req: petición POST para eliminar un estudiante (enviada por un formulario/botón)
            try {
                String id = req.params(":id");
                Student student = Student.findById(Integer.parseInt(id));
                if (student != null) {
                    Integer idPerson = student.getIdPerson();
                    // Al borrar el estudiante, ActiveJDBC debería borrar o desvincular relaciones si está configurado, o lo hacemos manualmente
                    student.delete();
                    
                    // Borramos la persona base asociada para no dejar datos huérfanos
                    Person person = Person.findById(idPerson);
                    if (person != null) person.delete();
                }
                // res: devolvemos a la lista con un mensaje informativo
                res.redirect("/estudiante/list?message=" + URLEncoder.encode("Estudiante eliminado correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/estudiante/list?error=" + URLEncoder.encode("Error al eliminar el estudiante.", StandardCharsets.UTF_8));
            }
            return "";
        });

        // ==============================================================================
        // RUTAS DE MATERIAS
        // Gestión de asignaturas, incluyendo sus correlativas y a qué plan de estudio pertenecen.
        // ==============================================================================

        // Muestra la información detallada de una materia (correlativas, profesores asignados, alumnos aprobados).
        get("/materia/perfil/:id", (req, res) -> {
            // req: petición para visualizar el perfil completo de una materia
            Map<String, Object> model = new HashMap<>();
            
            // Buscamos la materia por el ID pasado en la URL
            Subject sub = Subject.findById(req.params("id"));
            if (sub == null) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            // Pasamos los datos básicos de la materia a la vista
            model.put("nombre", sub.getString("nombre"));
            // Formateamos el código con 4 dígitos, o mostramos el original si no es número
            Object mainCodeObj = sub.get("codigo");
            if (mainCodeObj != null) {
                try {
                    model.put("codigo", "[" + String.format("%04d", Integer.parseInt(mainCodeObj.toString())) + "]");
                } catch (NumberFormatException e) {
                    model.put("codigo", "[" + mainCodeObj.toString() + "]");
                }
            } else {
                model.put("codigo", "[N/A]");
            }

            model.put("duracion", sub.getString("duracion"));
            model.put("anoDictado", sub.getInteger("año_dictado"));
            model.put("cuatrimestreDictado", sub.getInteger("cuatrimestre_dictado"));
            
            // Buscamos a qué plan de estudios pertenece
            StudyPlan plan = null;
            Object planId = sub.get("id_study_plan");
            if (planId != null) {
                plan = StudyPlan.findById(planId);
            }
            
            if (plan != null) {
                Career c = null;
                Object careerId = plan.get("id_career");
                if (careerId != null) {
                    c = Career.findById(careerId);
                }
                if (c != null) {
                    model.put("planName", c.getString("nombre") + " (Plan " + plan.getInteger("year") + ")");
                } else {
                    model.put("planName", "Plan " + plan.getInteger("year"));
                }
            } else {
                model.put("planName", "N/A");
            }

            // Buscamos las materias correlativas requeridas usando SQL
            List<Map> scRows = org.javalite.activejdbc.Base.findAll(
                "SELECT p.codigo FROM subjects p " +
                "JOIN prerequisites pr ON p.id = pr.id_prerequisite " +
                "WHERE pr.id_subject = ?", sub.getId()
            );
            List<String> corrNames = new ArrayList<>();
            for (Map row : scRows) {
                Object codeObj = row.get("codigo") != null ? row.get("codigo") : row.get("CODIGO");
                if (codeObj != null) {
                    try {
                        corrNames.add("[" + String.format("%04d", Integer.parseInt(codeObj.toString())) + "]");
                    } catch (NumberFormatException e) {
                        corrNames.add("[" + codeObj.toString() + "]");
                    }
                }
            }
            model.put("correlativas", corrNames.isEmpty() ? "Ninguna" : String.join(", ", corrNames));

            // Buscamos a los profesores asignados a dictar esta materia en cada periodo
            List<Map> assignments = org.javalite.activejdbc.Base.findAll(
                "SELECT p.name, p.apellido, a.year, a.semester FROM period_teacher_subject pts " +
                "JOIN teachers t ON pts.id_teacher = t.id " +
                "JOIN persons p ON t.id_person = p.id " +
                "JOIN academic_periods a ON pts.id_academic_period = a.id " +
                "WHERE pts.id_subject = ? " +
                "ORDER BY a.year DESC, a.semester DESC", sub.getId());
            List<Map<String, Object>> profesModel = new ArrayList<>();
            for (Map row : assignments) {
                Map<String, Object> map = new HashMap<>();
                map.put("profeName", row.get("name") + " " + row.get("apellido"));
                map.put("periodo", row.get("year") + " - Cuatrimestre " + row.get("semester"));
                profesModel.add(map);
            }
            model.put("profesores", profesModel);

            // Buscamos el historial de alumnos que aprobaron esta materia (nota >= 6)
            List<Map> examenes = org.javalite.activejdbc.Base.findAll(
                "SELECT p.name, p.apellido, e.fecha, e.nota FROM taken_exams e " +
                "JOIN students s ON e.id_student = s.id " +
                "JOIN persons p ON s.id_person = p.id " +
                "WHERE e.id_subject = ? AND e.nota >= 6 " +
                "ORDER BY e.fecha DESC", sub.getId());
            List<Map<String, Object>> alumnosModel = new ArrayList<>();
            for (Map row : examenes) {
                Map<String, Object> map = new HashMap<>();
                map.put("alumnoName", row.get("name") + " " + row.get("apellido"));
                
                Object fechaObj = row.get("fecha");
                String fechaFormateada = "";
                if (fechaObj != null) {
                    try {
                        long timestamp = Long.parseLong(fechaObj.toString());
                        java.util.Date date = new java.util.Date(timestamp);
                        fechaFormateada = new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
                    } catch (Exception e) {
                        fechaFormateada = fechaObj.toString();
                    }
                }
                
                map.put("fecha", fechaFormateada);
                map.put("nota", row.get("nota"));
                alumnosModel.add(map);
            }
            model.put("alumnosAprobados", alumnosModel);

            // res: mostramos la vista con el perfil de la materia
            return new ModelAndView(model, "subject_profile.mustache");
        }, new MustacheTemplateEngine());

        get("/materia/create", (req, res) -> {
            // req: petición para mostrar el formulario de alta de materia
            Map<String, Object> model = new HashMap<>();

            // Leemos mensajes de URL para feedback al usuario
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null && !errorCodigo.isEmpty()) {
                model.put("errorCodigo", errorCodigo);
            }

            // Cargamos todos los planes de estudio para poder asignarle uno a la materia
            List<StudyPlan> plans = StudyPlan.findAll();
            List<Map<String, Object>> plansModel = new ArrayList<>();
            for (StudyPlan p : plans) {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("id", p.getId());
                pMap.put("year", p.getInteger("year"));
                pMap.put("resolution", p.getString("resolution"));
                Career c = Career.findById(p.getInteger("id_career"));
                if (c != null) pMap.put("careerName", c.getString("nombre") + " (" + c.getString("codigo") + ")");
                plansModel.add(pMap);
            }
            model.put("plans", plansModel);

            // Cargamos todas las materias para poder elegirlas como correlativas
            List<Subject> subjects = Subject.findAll();
            List<Map<String, Object>> subjectsModel = new ArrayList<>();
            for (Subject s : subjects) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("nombre", s.getString("nombre"));
                sMap.put("codigo", s.getString("codigo"));
                sMap.put("id_study_plan", s.getInteger("id_study_plan"));
                subjectsModel.add(sMap);
            }
            model.put("allSubjects", subjectsModel);

            // res: renderiza la vista
            return new ModelAndView(model, "subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/new", (req, res) -> {
            // req: petición POST para crear una materia
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");
            String idStudyPlanStr = req.queryParams("id_study_plan");
            String duracion = req.queryParams("duracion");
            String anoDictadoStr = req.queryParams("ano_dictado");
            String cuatrimestreDictadoStr = req.queryParams("cuatrimestre_dictado");
            String[] idPrerequisitesStr = req.queryParamsValues("id_prerequisites");

            // Validamos que los campos requeridos estén llenos
            if (nombre == null || nombre.isEmpty() ||
                codigo == null || codigo.isEmpty() ||
                duracion == null || duracion.isEmpty() ||
                idStudyPlanStr == null || idStudyPlanStr.isEmpty() ||
                anoDictadoStr == null || anoDictadoStr.isEmpty() ||
                cuatrimestreDictadoStr == null || cuatrimestreDictadoStr.isEmpty()) {
                res.redirect("/materia/create?error=" +
                    URLEncoder.encode("Todos los campos obligatorios deben estar completos.", StandardCharsets.UTF_8));
                return "";
            }

            int codigoInt;
            try {
                // El código de la materia debe ser de 4 dígitos
                codigoInt = Integer.parseInt(codigo);
                if (codigoInt < 1000 || codigoInt > 9999) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/materia/create?errorCodigo=" + URLEncoder.encode("El código debe ser un número entero de 4 dígitos.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Verificamos que no haya otra materia con ese código
                Subject existing = Subject.findFirst("codigo = ?", codigoInt);
                if (existing != null) {
                    res.redirect("/materia/create?errorCodigo=" +
                        URLEncoder.encode("El código ya está registrado.", StandardCharsets.UTF_8));
                    return "";
                }
                
                int anoDictado = Integer.parseInt(anoDictadoStr);
                int cuatrimestreDictado = Integer.parseInt(cuatrimestreDictadoStr);

                // Regla de negocio: si es anual no puede empezar a mitad de año
                if ("Anual".equalsIgnoreCase(duracion) && cuatrimestreDictado == 2) {
                    res.redirect("/materia/create?error=" +
                        URLEncoder.encode("Una materia anual no puede comenzar en el segundo cuatrimestre.", StandardCharsets.UTF_8));
                    return "";
                }

                // Validaciones de correlativas
                if (idPrerequisitesStr != null && idPrerequisitesStr.length > 0) {
                    for (String prereqIdStr : idPrerequisitesStr) {
                        Subject prereq = Subject.findById(Integer.parseInt(prereqIdStr));
                        if (prereq != null) {
                            int pAno = prereq.getAñoDictado() != null ? prereq.getAñoDictado() : 1;
                            int pCuatrimestre = prereq.getCuatrimestreDictado() != null ? prereq.getCuatrimestreDictado() : 1;
                            
                            // No puede tener de correlativa una materia que se cursa después
                            if (pAno > anoDictado) {
                                res.redirect("/materia/create?error=" +
                                    URLEncoder.encode("No puedes tener como correlativa una materia de un año posterior.", StandardCharsets.UTF_8));
                                return "";
                            }
                            // Si son del mismo año, la correlativa tiene que haberse dado un cuatrimestre anterior
                            if (pAno == anoDictado && pCuatrimestre >= cuatrimestreDictado) {
                                res.redirect("/materia/create?error=" +
                                    URLEncoder.encode("Las correlativas del mismo año deben ser de un cuatrimestre estrictamente anterior.", StandardCharsets.UTF_8));
                                return "";
                            }
                        }
                    }
                }

                // Guardamos la nueva materia
                Subject subject = new Subject();
                subject.set("nombre", nombre);
                subject.set("codigo", codigoInt);
                subject.setDuracion(duracion);
                subject.set("id_study_plan", Integer.parseInt(idStudyPlanStr));
                subject.setAñoDictado(anoDictado);
                subject.setCuatrimestreDictado(cuatrimestreDictado);
                subject.saveIt();

                // Guardamos las correlativas requeridas
                if (idPrerequisitesStr != null && idPrerequisitesStr.length > 0) {
                    for (String prereqIdStr : idPrerequisitesStr) {
                        Prerequisite p = new Prerequisite();
                        p.set("id_subject", subject.getId());
                        p.set("id_prerequisite", Integer.parseInt(prereqIdStr));
                        p.saveIt();
                    }
                }

                // res: éxito
                res.redirect("/materia/list?message=" +
                    URLEncoder.encode("Materia " + nombre + " registrada exitosamente.", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                e.printStackTrace();
                res.redirect("/materia/create?error=" +
                    URLEncoder.encode("Error interno al registrar la materia.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/materia/edit/:id", (req, res) -> {
            // req: petición para mostrar el formulario para editar una materia
            Map<String, Object> model = new HashMap<>();
            
            // Buscamos la materia a editar
            Subject subject = Subject.findById(req.params(":id"));
            if (subject == null) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }
            model.put("subject", subject);
            
            // Seteamos las variables booleanas para pre-seleccionar los botones de radio correspondientes a duración
            model.put("isAnual", "Anual".equals(subject.getDuracion()));
            model.put("isCuatrimestral", "Cuatrimestral".equals(subject.getDuracion()));
            model.put("isCuatrimestre1", subject.getCuatrimestreDictado() != null && subject.getCuatrimestreDictado() == 1);
            model.put("isCuatrimestre2", subject.getCuatrimestreDictado() != null && subject.getCuatrimestreDictado() == 2);

            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null) model.put("errorCodigo", errorCodigo);

            // Obtenemos qué materias son actualmente correlativas de ésta
            List<Prerequisite> prereqs = Prerequisite.where("id_subject = ?", subject.getId());
            List<Integer> assignedPrereqs = new ArrayList<>();
            for (Prerequisite p : prereqs) {
                assignedPrereqs.add(p.getInteger("id_prerequisite"));
            }

            // Cargamos la lista de planes de estudio y preseleccionamos el de esta materia
            List<StudyPlan> plans = StudyPlan.findAll();
            List<Map<String, Object>> plansModel = new ArrayList<>();
            for (StudyPlan p : plans) {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("id", p.getId());
                pMap.put("year", p.getInteger("year"));
                pMap.put("resolution", p.getString("resolution"));
                Career c = Career.findById(p.getInteger("id_career"));
                if (c != null) pMap.put("careerName", c.getString("nombre") + " (" + c.getString("codigo") + ")");
                
                if (subject.getInteger("id_study_plan") != null && subject.getInteger("id_study_plan").equals(Integer.parseInt(p.getId().toString()))) {
                    pMap.put("selected", true);
                }
                plansModel.add(pMap);
            }
            model.put("plans", plansModel);

            // Cargamos la lista de todas las materias disponibles como correlativas, descartando la materia actual
            List<Subject> subjects = Subject.findAll();
            List<Map<String, Object>> subjectsModel = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.getId().equals(subject.getId())) continue; // No puede ser correlativa de sí misma
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("nombre", s.getString("nombre"));
                sMap.put("codigo", s.getString("codigo"));
                sMap.put("id_study_plan", s.getInteger("id_study_plan"));
                // Preseleccionamos si ya era correlativa
                if (assignedPrereqs.contains(Integer.parseInt(s.getId().toString()))) {
                    sMap.put("selected", true);
                }
                subjectsModel.add(sMap);
            }
            model.put("allSubjects", subjectsModel);

            // res: enviamos la vista
            return new ModelAndView(model, "subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/update/:id", (req, res) -> {
            // req: el usuario envió un formulario POST a /materia/update/ID_MATERIA
            // Extraemos todos los campos ingresados en el formulario
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");
            String idStudyPlanStr = req.queryParams("id_study_plan");
            String duracion = req.queryParams("duracion");
            String anoDictadoStr = req.queryParams("ano_dictado");
            String cuatrimestreDictadoStr = req.queryParams("cuatrimestre_dictado");
            String[] idPrerequisitesStr = req.queryParamsValues("id_prerequisites");

            // Validamos que ningún campo obligatorio esté vacío
            if (nombre == null || nombre.trim().isEmpty() || 
                codigo == null || codigo.trim().isEmpty() || 
                duracion == null || duracion.trim().isEmpty() || 
                idStudyPlanStr == null || idStudyPlanStr.isEmpty() ||
                anoDictadoStr == null || anoDictadoStr.isEmpty() ||
                cuatrimestreDictadoStr == null || cuatrimestreDictadoStr.isEmpty()) {
                
                // Si falla la validación, redirigimos devolviendo un error en la URL
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("Todos los campos obligatorios deben estar completos.", StandardCharsets.UTF_8));
                return null;
            }

            int codigoInt;
            try {
                // Convertimos el código a número y validamos que sea de 4 dígitos
                codigoInt = Integer.parseInt(codigo);
                if (codigoInt < 1000 || codigoInt > 9999) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código debe ser un número entero de 4 dígitos.", StandardCharsets.UTF_8));
                return null;
            }

            // Buscamos la materia que estamos intentando editar en la BD
            Subject subject = Subject.findById(req.params(":id"));
            if (subject == null) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            // Verificamos que el código nuevo no esté siendo usado por otra materia distinta
            Subject existing = Subject.findFirst("codigo = ?", codigoInt);
            if (existing != null && !existing.getId().equals(subject.getId())) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código ya está en uso.", StandardCharsets.UTF_8));
                return null;
            }
            
            int anoDictado = Integer.parseInt(anoDictadoStr);
            int cuatrimestreDictado = Integer.parseInt(cuatrimestreDictadoStr);

            // Regla de negocio: Materias anuales no pueden empezar en el segundo cuatrimestre
            if ("Anual".equalsIgnoreCase(duracion) && cuatrimestreDictado == 2) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" +
                    URLEncoder.encode("Una materia anual no puede comenzar en el segundo cuatrimestre.", StandardCharsets.UTF_8));
                return null;
            }

            // Validaciones de correlativas (no pueden ser del futuro o del mismo cuatrimestre)
            if (idPrerequisitesStr != null && idPrerequisitesStr.length > 0) {
                for (String prereqIdStr : idPrerequisitesStr) {
                    Subject prereq = Subject.findById(Integer.parseInt(prereqIdStr));
                    if (prereq != null) {
                        int pAno = prereq.getAñoDictado() != null ? prereq.getAñoDictado() : 1;
                        int pCuatrimestre = prereq.getCuatrimestreDictado() != null ? prereq.getCuatrimestreDictado() : 1;
                        
                        if (pAno > anoDictado) {
                            res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" +
                                URLEncoder.encode("No puedes tener como correlativa una materia de un año posterior.", StandardCharsets.UTF_8));
                            return "";
                        }
                        if (pAno == anoDictado && pCuatrimestre >= cuatrimestreDictado) {
                            res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" +
                                URLEncoder.encode("Las correlativas del mismo año deben ser de un cuatrimestre estrictamente anterior.", StandardCharsets.UTF_8));
                            return "";
                        }
                    }
                }
            }

            // Actualizamos los campos de la materia con los datos nuevos
            subject.setNombre(nombre);
            subject.setCodigo(codigoInt);
            subject.setDuracion(duracion);
            subject.set("id_study_plan", Integer.parseInt(idStudyPlanStr));
            subject.setAñoDictado(anoDictado);
            subject.setCuatrimestreDictado(cuatrimestreDictado);
            
            // Guardamos los cambios en la BD
            subject.saveIt();

            // Actualizamos las correlativas: primero borramos las viejas...
            org.javalite.activejdbc.Base.exec("DELETE FROM prerequisites WHERE id_subject = ?", subject.getId());
            
            // ... y luego insertamos las nuevas seleccionadas
            if (idPrerequisitesStr != null && idPrerequisitesStr.length > 0) {
                for (String prereqIdStr : idPrerequisitesStr) {
                    Prerequisite p = new Prerequisite();
                    p.set("id_subject", subject.getId());
                    p.set("id_prerequisite", Integer.parseInt(prereqIdStr));
                    p.saveIt();
                }
            }

            // res: redirigimos al usuario a la lista de materias con un mensaje de éxito
            res.redirect("/materia/list?message=" + URLEncoder.encode("Materia actualizada exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        get("/materia/list", (req, res) -> {
            // req: el usuario hizo GET a /materia/list
            Map<String, Object> model = new HashMap<>();

            // Verificamos si viene un parámetro de éxito en la URL
            // Ej: /materia/list?message=Materia+creada
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            // Cargamos todas las materias de la BD usando el modelo Subject
            List<Subject> subjects = Subject.findAll();
            List<Map<String, Object>> subjectsModel = new ArrayList<>();
            
            // Iteramos sobre cada materia para prepararla para la vista
            for (Subject s : subjects) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("nombre", s.getNombre());
                
                // Formateamos el código con 4 dígitos (ej: [0012])
                sMap.put("codigo", "[" + String.format("%04d", s.getCodigo()) + "]");
                sMap.put("duracion", s.getDuracion());
                sMap.put("anoDictado", s.getAñoDictado());
                
                // Lógica de presentación para el cuatrimestre
                if ("Anual".equalsIgnoreCase(s.getDuracion())) {
                    sMap.put("cuatrimestreDictado", "1 y 2");
                } else {
                    sMap.put("cuatrimestreDictado", s.getCuatrimestreDictado());
                }

                // Buscamos el plan de estudio asociado a esta materia
                StudyPlan p = null;
                Object planId = s.get("id_study_plan");
                if (planId != null) {
                    p = StudyPlan.findById(planId);
                }
                
                if (p != null) {
                    Career c = null;
                    Object careerId = p.get("id_career");
                    if (careerId != null) {
                        c = Career.findById(careerId);
                    }
                    if (c != null) {
                        sMap.put("planName", c.getString("nombre") + " (Plan " + p.getInteger("year") + ")");
                    } else {
                        sMap.put("planName", "Plan " + p.getInteger("year"));
                    }
                }

                // Buscamos las materias correlativas (prerrequisitos)
                List<Prerequisite> prereqs = Prerequisite.where("id_subject = ?", s.getId());
                if (!prereqs.isEmpty()) {
                    List<String> prereqNames = new ArrayList<>();
                    for (Prerequisite pr : prereqs) {
                        Subject prereqSubj = Subject.findById(pr.getInteger("id_prerequisite"));
                        if (prereqSubj != null) {
                            prereqNames.add("[" + String.format("%04d", prereqSubj.getInteger("codigo")) + "]");
                        }
                    }
                    sMap.put("correlativas", String.join(", ", prereqNames));
                } else {
                    sMap.put("correlativas", "Ninguna");
                }
                
                subjectsModel.add(sMap);
            }

            // Agregamos la lista completa al modelo para que Mustache la renderice
            model.put("subjects", subjectsModel);
            
            // res: le mandamos de vuelta la vista renderizada 'subject_list.mustache'
            return new ModelAndView(model, "subject_list.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/delete/:id", (req, res) -> {
            // req: el usuario envió una petición POST para eliminar una materia
            try {
                // Obtenemos el ID de la URL
                Integer id = Integer.parseInt(req.params(":id"));
                
                // Buscamos la materia
                Subject subject = Subject.findById(id);
                if (subject != null) {
                    // Borramos la materia de la base de datos
                    subject.delete();
                    // res: redirigimos con éxito
                    res.redirect("/materia/list?message=" + URLEncoder.encode("Materia eliminada.", StandardCharsets.UTF_8));
                } else {
                    // res: redirigimos con error de no encontrada
                    res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                // res: capturamos excepciones y redirigimos con un error genérico
                res.redirect("/materia/list?error=" + URLEncoder.encode("Error al eliminar la materia.", StandardCharsets.UTF_8));
            }
            return "";
        });

        // --- CARRERAS ---
        // ==============================================================================
        // RUTAS DE CARRERAS
        // Gestión de las carreras que ofrece la institución (Licenciatura, Ingeniería, etc.)
        // ==============================================================================

        // Muestra el formulario para crear una nueva carrera.
        get("/carrera/create", (req, res) -> {
            // req: petición para obtener el formulario de creación de carrera
            Map<String, Object> model = new HashMap<>();
            
            // Verificamos si hubo un error previo para mostrarlo en el formulario
            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null) model.put("errorCodigo", errorCodigo);
            
            // res: renderiza y devuelve la vista del formulario
            return new ModelAndView(model, "career_form.mustache");
        }, new MustacheTemplateEngine());

        post("/carrera/new", (req, res) -> {
            // req: petición POST enviada desde el formulario de carrera
            String codigoStr = req.queryParams("codigo");
            String nombre = req.queryParams("nombre");
            String duracionStr = req.queryParams("duracion");

            // Validación: ninguno de los campos puede estar vacío
            if (codigoStr == null || codigoStr.trim().isEmpty() || nombre == null || nombre.trim().isEmpty() || duracionStr == null || duracionStr.trim().isEmpty()) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            int duracion;
            try {
                // La duración debe ser un número positivo (años o semestres dependiendo de la lógica de negocio, asumimos positivo)
                duracion = Integer.parseInt(duracionStr);
                if (duracion <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("La duración debe ser un número entero positivo.", StandardCharsets.UTF_8));
                return null;
            }

            int codigo;
            try {
                // El código de carrera debe ser un número entero
                codigo = Integer.parseInt(codigoStr);
            } catch (NumberFormatException e) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("El código debe ser numérico.", StandardCharsets.UTF_8));
                return null;
            }

            // Validamos que el código de la carrera sea único
            if (Career.findFirst("codigo = ?", codigo) != null) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("El código de carrera ya existe.", StandardCharsets.UTF_8));
                return null;
            }

            // Creamos y guardamos el registro de la carrera en la BD
            Career career = new Career();
            career.set("codigo", codigo);
            career.set("nombre", nombre);
            career.set("duracion", duracion);
            career.saveIt();

            // res: redirigimos al listado de carreras con mensaje de éxito
            res.redirect("/carrera/list?message=" + URLEncoder.encode("Carrera registrada exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        get("/carrera/list", (req, res) -> {
            // req: petición para ver la lista completa de carreras
            Map<String, Object> model = new HashMap<>();
            
            // Obtenemos todas las carreras usando el ORM
            List<Career> allCareers = Career.findAll();
            List<Map<String, Object>> careersModel = new ArrayList<>();
            
            // Recorremos las carreras para empaquetarlas en un formato amigable para la vista
            for (Career c : allCareers) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("codigo", c.getInteger("codigo"));
                cMap.put("nombre", c.getString("nombre"));
                cMap.put("duracion", c.getInteger("duracion"));
                
                // Buscamos los planes de estudio vinculados a esta carrera
                List<StudyPlan> plans = c.getAll(StudyPlan.class);
                if (plans != null && !plans.isEmpty()) {
                    List<String> planNames = new ArrayList<>();
                    for (StudyPlan sp : plans) {
                        planNames.add(sp.getInteger("year") + " (Res. " + sp.getString("resolution") + ")");
                    }
                    cMap.put("planes", String.join(", ", planNames));
                } else {
                    cMap.put("planes", "Sin planes");
                }
                
                careersModel.add(cMap);
            }
            
            model.put("careers", careersModel);
            
            // Reenviamos los mensajes de alerta (éxito o error)
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);
            
            // res: renderizamos la plantilla con los datos
            return new ModelAndView(model, "career_list.mustache");
        }, new MustacheTemplateEngine());

        get("/carrera/edit/:id", (req, res) -> {
            // req: petición para mostrar formulario de edición de una carrera particular
            Map<String, Object> model = new HashMap<>();
            
            // Buscamos la carrera por ID
            Career career = Career.findById(req.params(":id"));
            if (career == null) {
                // res: si no existe, redirigimos
                res.redirect("/carrera/list?error=" + URLEncoder.encode("Carrera no encontrada.", StandardCharsets.UTF_8));
                return null;
            }
            model.put("career", career);
            
            // Verificamos si hay un mensaje de error por una actualización fallida (ej. código duplicado)
            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null) model.put("errorCodigo", errorCodigo);
            
            // res: renderizamos la vista de edición (que reutiliza el mismo formulario que crear)
            return new ModelAndView(model, "career_form.mustache");
        }, new MustacheTemplateEngine());

        post("/carrera/update/:id", (req, res) -> {
            // req: petición POST con los nuevos datos de la carrera
            String codigoStr = req.queryParams("codigo");
            String nombre = req.queryParams("nombre");
            String duracionStr = req.queryParams("duracion");

            // Validamos campos requeridos
            if (codigoStr == null || codigoStr.trim().isEmpty() || nombre == null || nombre.trim().isEmpty() || duracionStr == null || duracionStr.trim().isEmpty()) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            int duracion;
            try {
                // La duración debe ser válida
                duracion = Integer.parseInt(duracionStr);
                if (duracion <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("La duración debe ser un número entero positivo.", StandardCharsets.UTF_8));
                return null;
            }

            int codigo;
            try {
                // El código debe ser numérico
                codigo = Integer.parseInt(codigoStr);
            } catch (NumberFormatException e) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código debe ser numérico.", StandardCharsets.UTF_8));
                return null;
            }

            // Buscamos la carrera que queremos actualizar
            Career career = Career.findById(req.params(":id"));
            if (career == null) {
                res.redirect("/carrera/list?error=" + URLEncoder.encode("Carrera no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            // Verificamos que el nuevo código ingresado no pertenezca ya a otra carrera
            Career existing = Career.findFirst("codigo = ?", codigo);
            if (existing != null && !existing.getId().equals(career.getId())) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código de carrera ya está en uso.", StandardCharsets.UTF_8));
                return null;
            }

            // Guardamos los cambios
            career.set("codigo", codigo);
            career.set("nombre", nombre);
            career.set("duracion", duracion);
            career.saveIt();

            // res: redirección con éxito
            res.redirect("/carrera/list?message=" + URLEncoder.encode("Carrera actualizada exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        post("/carrera/delete/:id", (req, res) -> {
            // req: petición para eliminar permanentemente una carrera
            Career career = Career.findById(req.params(":id"));
            if (career != null) {
                // ActiveJDBC se encargará de borrar dependiendo de las reglas de base de datos
                career.delete();
                // res: éxito
                res.redirect("/carrera/list?message=" + URLEncoder.encode("Carrera eliminada.", StandardCharsets.UTF_8));
            } else {
                // res: la carrera no existe
                res.redirect("/carrera/list?error=" + URLEncoder.encode("Carrera no encontrada.", StandardCharsets.UTF_8));
            }
            return null;
        });
        // --- PLANES DE ESTUDIO ---
        // ==============================================================================
        // RUTAS DE PLANES DE ESTUDIO
        // Agrupa las materias en un plan de estudio específico para una carrera.
        // ==============================================================================

        // Lista todos los planes de estudio registrados.
        get("/plan/list", (req, res) -> {
            // req: petición para mostrar la lista de planes de estudio
            Map<String, Object> model = new HashMap<>();
            
            // Leemos parámetros de la URL para mostrar mensajes de éxito o error al usuario
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);

            // Obtenemos todos los planes de la base de datos
            List<StudyPlan> plans = StudyPlan.findAll();
            List<Map<String, Object>> plansModel = new ArrayList<>();
            for (StudyPlan plan : plans) {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("id", plan.getId());
                pMap.put("year", plan.getInteger("year"));
                pMap.put("resolution", plan.getString("resolution"));
                
                // Buscamos la carrera a la que pertenece este plan
                Career career = Career.findById(plan.getInteger("id_career"));
                if (career != null) {
                    pMap.put("careerName", career.getString("nombre") + " (" + career.getString("codigo") + ")");
                }
                plansModel.add(pMap);
            }
            model.put("plans", plansModel);
            
            // res: renderizamos la vista pasando los planes y mensajes
            return new ModelAndView(model, "study_plan_list.mustache");
        }, new MustacheTemplateEngine());

        get("/plan/create", (req, res) -> {
            // req: petición para mostrar formulario de creación de plan de estudio
            Map<String, Object> model = new HashMap<>();
            
            // Si hubo error de validación, lo mostramos
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);
            
            // Pasamos todas las carreras para que el usuario pueda elegir a cuál asignar el plan
            model.put("careers", Career.findAll());
            
            // res: renderiza la vista
            return new ModelAndView(model, "study_plan_form.mustache");
        }, new MustacheTemplateEngine());

        post("/plan/new", (req, res) -> {
            // req: petición POST para crear el plan de estudio
            String yearStr = req.queryParams("year");
            String resolution = req.queryParams("resolution");
            String idCareerStr = req.queryParams("id_career");

            // Validamos que todos los campos del formulario tengan valor
            if (yearStr == null || yearStr.trim().isEmpty() || resolution == null || resolution.trim().isEmpty() || idCareerStr == null || idCareerStr.trim().isEmpty()) {
                res.redirect("/plan/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            // Creamos la entidad StudyPlan y asignamos sus atributos
            StudyPlan plan = new StudyPlan();
            plan.set("year", Integer.parseInt(yearStr));
            plan.set("resolution", resolution);
            plan.set("id_career", Integer.parseInt(idCareerStr));
            
            // Guardamos en la BD
            plan.saveIt();

            // res: redirigimos a la lista con un mensaje indicando que se creó exitosamente
            res.redirect("/plan/list?message=" + URLEncoder.encode("Plan creado exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        get("/plan/edit/:id", (req, res) -> {
            // req: petición GET para cargar el formulario de edición de un plan
            Map<String, Object> model = new HashMap<>();
            
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);

            // Buscamos el plan
            StudyPlan plan = StudyPlan.findById(req.params(":id"));
            if (plan == null) {
                res.redirect("/plan/list?error=" + URLEncoder.encode("Plan no encontrado.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("plan", plan);
            
            // Pre-seleccionamos la carrera a la que ya está asignado el plan
            Integer currentCareerId = plan.getInteger("id_career");
            List<Career> careers = Career.findAll();
            List<Map<String, Object>> careersModel = new ArrayList<>();
            for (Career c : careers) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("nombre", c.getString("nombre"));
                if (currentCareerId != null && currentCareerId.equals(c.getId())) {
                    cMap.put("selected", true);
                }
                careersModel.add(cMap);
            }
            model.put("careers", careersModel);

            // res: renderiza
            return new ModelAndView(model, "study_plan_form.mustache");
        }, new MustacheTemplateEngine());

        post("/plan/update/:id", (req, res) -> {
            // req: petición POST con los datos editados
            String yearStr = req.queryParams("year");
            String resolution = req.queryParams("resolution");
            String idCareerStr = req.queryParams("id_career");

            // Validación de los datos
            if (yearStr == null || yearStr.trim().isEmpty() || resolution == null || resolution.trim().isEmpty() || idCareerStr == null || idCareerStr.trim().isEmpty()) {
                res.redirect("/plan/edit/" + req.params(":id") + "?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            // Buscamos el plan, modificamos los datos y guardamos
            StudyPlan plan = StudyPlan.findById(req.params(":id"));
            if (plan != null) {
                plan.set("year", Integer.parseInt(yearStr));
                plan.set("resolution", resolution);
                plan.set("id_career", Integer.parseInt(idCareerStr));
                plan.saveIt();
                // res: redirección a lista con éxito
                res.redirect("/plan/list?message=" + URLEncoder.encode("Plan actualizado exitosamente.", StandardCharsets.UTF_8));
            } else {
                res.redirect("/plan/list?error=" + URLEncoder.encode("Plan no encontrado.", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/plan/delete/:id", (req, res) -> {
            // req: petición POST para eliminar el plan de estudio
            StudyPlan plan = StudyPlan.findById(req.params(":id"));
            if (plan != null) {
                plan.delete();
                res.redirect("/plan/list?message=" + URLEncoder.encode("Plan eliminado.", StandardCharsets.UTF_8));
            } else {
                res.redirect("/plan/list?error=" + URLEncoder.encode("Plan no encontrado.", StandardCharsets.UTF_8));
            }
            return null;
        });


        // --- PERIODOS ACADÉMICOS ---
        // ==============================================================================
        // RUTAS DE PERIODOS ACADÉMICOS
        // Representan los semestres o cuatrimestres de un año (ej. 2025 - Cuatrimestre 1).
        // ==============================================================================

        // Muestra todos los periodos académicos cargados en el sistema.
        get("/periodo/list", (req, res) -> {
            // req: petición para mostrar la lista de periodos académicos
            Map<String, Object> model = new HashMap<>();
            
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);
            
            // Consultamos todos los periodos académicos
            List<AcademicPeriod> periodos = AcademicPeriod.findAll();
            List<Map<String, Object>> periodosModel = new ArrayList<>();
            for (AcademicPeriod p : periodos) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("year", p.getInteger("year"));
                map.put("semester", p.getInteger("semester"));
                periodosModel.add(map);
            }
            model.put("periodos", periodosModel);
            
            // res: enviamos a la vista
            return new ModelAndView(model, "academic_period_list.mustache");
        }, new MustacheTemplateEngine());

        get("/periodo/create", (req, res) -> {
            // req: petición GET para obtener el formulario de creación de periodo
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);
            
            // res: renderiza el formulario
            return new ModelAndView(model, "academic_period_form.mustache");
        }, new MustacheTemplateEngine());

        post("/periodo/new", (req, res) -> {
            // req: petición POST para crear el periodo
            String yearStr = req.queryParams("year");
            String semesterStr = req.queryParams("semester");

            if (yearStr == null || yearStr.trim().isEmpty() || semesterStr == null || semesterStr.trim().isEmpty()) {
                res.redirect("/periodo/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                // Comprobamos que no se esté creando un periodo duplicado
                AcademicPeriod existing = AcademicPeriod.findFirst("year = ? AND semester = ?", Integer.parseInt(yearStr), Integer.parseInt(semesterStr));
                if (existing != null) {
                    res.redirect("/periodo/create?error=" + URLEncoder.encode("El periodo ya existe.", StandardCharsets.UTF_8));
                    return null;
                }
                
                // Guardamos
                AcademicPeriod p = new AcademicPeriod();
                p.set("year", Integer.parseInt(yearStr));
                p.set("semester", Integer.parseInt(semesterStr));
                p.saveIt();
                // res: éxito
                res.redirect("/periodo/list?message=" + URLEncoder.encode("Periodo creado exitosamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/periodo/create?error=" + URLEncoder.encode("Error interno.", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/periodo/delete/:id", (req, res) -> {
            // req: petición para eliminar periodo
            AcademicPeriod p = AcademicPeriod.findById(req.params(":id"));
            if (p != null) {
                p.delete();
                // res: eliminado con éxito
                res.redirect("/periodo/list?message=" + URLEncoder.encode("Periodo eliminado.", StandardCharsets.UTF_8));
            } else {
                res.redirect("/periodo/list?error=" + URLEncoder.encode("Periodo no encontrado.", StandardCharsets.UTF_8));
            }
            return null;
        });

        // --- ASIGNACIONES ---
        // ==============================================================================
        // ASIGNACIONES DE PROFESORES A MATERIAS
        // Vincula a un profesor con una materia específica en un periodo académico determinado.
        // ==============================================================================

        // Muestra las materias a las que un profesor particular está asignado.
        get("/profesor/materias/:id", (req, res) -> {
            // req: petición para mostrar la tabla de materias que dicta un profesor particular
            Map<String, Object> model = new HashMap<>();
            try {
                // Buscamos el profesor
                Integer idTeacher = Integer.parseInt(req.params(":id"));
                Teacher teacher = Teacher.findById(idTeacher);

                if (teacher == null) {
                    res.redirect("/dashboard?error=" + URLEncoder.encode("Profesor no encontrado.", StandardCharsets.UTF_8));
                    return null;
                }

                // Buscamos todas sus asignaciones activas (relación muchos a muchos)
                List<PeriodTeacherSubject> asignaciones = PeriodTeacherSubject.where("id_teacher = ?", idTeacher);
                List<Map<String, Object>> asignacionesModel = new ArrayList<>();
                for (PeriodTeacherSubject pts : asignaciones) {
                    Map<String, Object> map = new HashMap<>();
                    
                    // Buscamos detalles de la materia y el periodo asignado
                    Subject s = Subject.findById(pts.get("id_subject"));
                    AcademicPeriod p = AcademicPeriod.findById(pts.get("id_academic_period"));
                    if (s != null) {
                        map.put("subjectName", s.getString("nombre"));
                        map.put("subjectCode", s.getString("codigo"));
                        map.put("subjectDuracion", s.getDuracion());
                        
                        StudyPlan sp = null;
                        Object planId = s.get("id_study_plan");
                        if (planId != null) {
                            sp = StudyPlan.findById(planId);
                        }
                        if (sp != null) {
                            Career c = null;
                            Object careerId = sp.get("id_career");
                            if (careerId != null) {
                                c = Career.findById(careerId);
                            }
                            if (c != null) {
                                map.put("carreras", c.getString("nombre") + " (Plan " + sp.getInteger("year") + ")");
                            }
                        }
                    }
                    if (p != null) map.put("periodo", p.getInteger("year") + " - " + p.getInteger("semester") + "° Cuatrimestre");
                    asignacionesModel.add(map);
                }
                
                model.put("teacher", teacher);
                model.put("asignaciones", asignacionesModel);
                
                // res: mostrar la plantilla de materias del profe
                return new ModelAndView(model, "teacher_subjects_list.mustache");
            } catch (Exception e) {
                res.redirect("/dashboard?error=" + URLEncoder.encode("Error al obtener materias del profesor.", StandardCharsets.UTF_8));
                return null;
            }
        }, new MustacheTemplateEngine());

        get("/asignacion/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) model.put("errorMessage", errorMessage);

            model.put("teachers", Teacher.findAll());
            model.put("subjects", Subject.findAll());
            model.put("periodos", AcademicPeriod.findAll());
            return new ModelAndView(model, "teacher_subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/asignacion/new", (req, res) -> {
            // req: petición para registrar una nueva asignación de un profesor a una materia en un periodo
            String idTeacher = req.queryParams("id_teacher");
            String idSubject = req.queryParams("id_subject");
            String idPeriodo = req.queryParams("id_academic_period");

            // Validación de campos obligatorios
            if (idTeacher == null || idTeacher.isEmpty() || idSubject == null || idSubject.isEmpty() || idPeriodo == null || idPeriodo.isEmpty()) {
                res.redirect("/asignacion/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Subject subject = Subject.findById(Integer.parseInt(idSubject));
                AcademicPeriod period = AcademicPeriod.findById(Integer.parseInt(idPeriodo));
                if (subject != null && period != null) {
                    // Lógica de negocio: materia anual no puede iniciar en cuatrimestre 2
                    if ("Anual".equals(subject.getDuracion()) && period.getInteger("semester") == 2) {
                        res.redirect("/asignacion/create?error=" + URLEncoder.encode("Una materia anual no puede comenzar a dictarse en el 2° cuatrimestre.", StandardCharsets.UTF_8));
                        return "";
                    }
                }

                // Evitar asignaciones duplicadas de ese profe a la misma materia en el mismo periodo
                PeriodTeacherSubject existing = PeriodTeacherSubject.findFirst("id_teacher = ? AND id_subject = ? AND id_academic_period = ?", Integer.parseInt(idTeacher), Integer.parseInt(idSubject), Integer.parseInt(idPeriodo));
                if (existing != null) {
                    res.redirect("/asignacion/create?error=" + URLEncoder.encode("El profesor ya está asignado a esa materia en ese período.", StandardCharsets.UTF_8));
                    return "";
                }

                // Guardamos
                PeriodTeacherSubject ts = new PeriodTeacherSubject();
                ts.set("id_teacher", Integer.parseInt(idTeacher));
                ts.set("id_subject", Integer.parseInt(idSubject));
                ts.set("id_academic_period", Integer.parseInt(idPeriodo));
                ts.saveIt();

                // res: redireccionamos con éxito
                res.redirect("/asignacion/create?message=" + URLEncoder.encode("Asignación registrada exitosamente.", StandardCharsets.UTF_8));
                return "";
            } catch (Exception e) {
                res.redirect("/asignacion/create?error=" + URLEncoder.encode("Error interno al registrar la asignación.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/asignacion/list", (req, res) -> {
            // req: petición para mostrar todas las asignaciones globales del sistema
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) model.put("successMessage", successMessage);

            // Obtenemos todos los registros tabla puente (PeriodTeacherSubject)
            List<PeriodTeacherSubject> asignaciones = PeriodTeacherSubject.findAll();
            List<Map<String, Object>> asignacionesModel = new ArrayList<>();
            for (PeriodTeacherSubject pts : asignaciones) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", pts.getId());
                
                // Extraemos nombre profe, materia y periodo para mostrar al usuario
                Teacher t = Teacher.findById(pts.get("id_teacher"));
                Subject s = Subject.findById(pts.get("id_subject"));
                AcademicPeriod p = AcademicPeriod.findById(pts.get("id_academic_period"));
                
                if (t != null) {
                    map.put("teacherName", t.getName() + " " + t.getApellido());
                }
                if (s != null) {
                    map.put("subjectName", s.getString("nombre"));
                    map.put("subjectCode", s.getString("codigo"));
                    map.put("subjectDuracion", s.getDuracion());
                }
                if (p != null) map.put("periodo", p.getInteger("year") + " - " + p.getInteger("semester") + "° Cuatrimestre");
                
                asignacionesModel.add(map);
            }
            model.put("asignaciones", asignacionesModel);
            
            // res: renderiza
            return new ModelAndView(model, "teacher_subject_list.mustache");
        }, new MustacheTemplateEngine());

        post("/asignacion/delete/:id", (req, res) -> {
            // req: petición para eliminar una asignación
            try {
                Integer id = Integer.parseInt(req.params(":id"));
                PeriodTeacherSubject ts = PeriodTeacherSubject.findById(id);
                if (ts != null) ts.delete(); // Borramos el registro
                // res: éxito
                res.redirect("/asignacion/list?message=" + URLEncoder.encode("Asignación eliminada correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/asignacion/list?error=" + URLEncoder.encode("Error al eliminar la asignación.", StandardCharsets.UTF_8));
            }
            return "";
        });
        // --- EXÁMENES RENDIDOS ---
        // ==============================================================================
        // RUTAS DE EXÁMENES RENDIDOS
        // Gestión de las notas que sacaron los estudiantes en las materias.
        // ==============================================================================

        // Lista los exámenes rendidos.
        get("/examen/list", (req, res) -> {
            // req: petición para listar los exámenes rendidos, con soporte de filtrado opcional por materia
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);

            String subjectId = req.queryParams("subjectId");
            List<TakenExam> examenes;
            
            // Si el usuario seleccionó una materia para filtrar, buscamos solo esos; si no, buscamos todos
            if (subjectId != null && !subjectId.isEmpty()) {
                examenes = TakenExam.where("id_subject = ?", subjectId).orderBy("fecha DESC");
                model.put("filtering", true);
            } else {
                examenes = TakenExam.findAll().orderBy("fecha DESC");
            }

            List<Map<String, Object>> examenesModel = new ArrayList<>();
            for (TakenExam exam : examenes) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", exam.getId());
                // getStudentName y getSubjectName asumen métodos personalizados en el modelo TakenExam
                map.put("studentName", exam.getStudentName());
                map.put("subjectName", exam.getSubjectName());
                if (exam.getFecha() != null) map.put("fecha", exam.getFecha().toString());
                map.put("nota", exam.getNota());
                examenesModel.add(map);
            }
            model.put("examenes", examenesModel);

            // Cargamos la lista de materias para poblar el <select> de filtrado
            List<Subject> allSubjects = Subject.findAll();
            List<Map<String, Object>> subjectsFilter = new ArrayList<>();
            for (Subject sub : allSubjects) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", sub.getId());
                map.put("nombre", sub.getString("nombre"));
                map.put("codigo", sub.getString("codigo"));
                if (subjectId != null && subjectId.equals(sub.getId().toString())) {
                    map.put("selected", true);
                }
                subjectsFilter.add(map);
            }
            model.put("subjectsFilter", subjectsFilter);

            // res: renderizamos la vista de la grilla de exámenes
            return new ModelAndView(model, "taken_exam_list.mustache");
        }, new MustacheTemplateEngine());

        get("/examen/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);
            
            List<Student> students = Student.findAll();
            List<Map<String, Object>> studentsModel = new ArrayList<>();
            for (Student s : students) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("name", s.getApellido() + ", " + s.getName());
                studentsModel.add(map);
            }
            
            model.put("students", studentsModel);
            model.put("subjects", Subject.findAll());
            return new ModelAndView(model, "taken_exam_form.mustache");
        }, new MustacheTemplateEngine());

        post("/examen/new", (req, res) -> {
            // req: el usuario envió un formulario POST para registrar la nota de un examen
            String idStudent = req.queryParams("id_student");
            String idSubject = req.queryParams("id_subject");
            String fecha = req.queryParams("fecha");
            String notaStr = req.queryParams("nota");

            // Validamos que se envíen todos los datos
            if (idStudent == null || idStudent.isEmpty() || idSubject == null || idSubject.isEmpty() ||
                fecha == null || fecha.isEmpty() || notaStr == null || notaStr.isEmpty()) {
                res.redirect("/examen/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                // La nota de la facultad debe ser un valor del 1 al 10
                int nota = Integer.parseInt(notaStr);
                if (nota < 1 || nota > 10) {
                    res.redirect("/examen/create?error=" + URLEncoder.encode("La nota debe estar entre 1 y 10.", StandardCharsets.UTF_8));
                    return null;
                }

                // Guardamos el registro del examen
                TakenExam exam = new TakenExam();
                exam.set("id_student", Integer.parseInt(idStudent));
                exam.set("id_subject", Integer.parseInt(idSubject));
                // Convertimos el string de fecha ("YYYY-MM-DD") al formato SQL
                exam.set("fecha", java.sql.Date.valueOf(java.time.LocalDate.parse(fecha)));
                exam.set("nota", nota);
                exam.saveIt();

                // res: redireccionamos a la lista de exámenes con éxito
                res.redirect("/examen/list?message=" + URLEncoder.encode("Examen registrado exitosamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/examen/create?error=" + URLEncoder.encode("Error interno al registrar el examen. Verifique el formato de fecha (YYYY-MM-DD).", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/examen/delete/:id", (req, res) -> {
            // req: petición para eliminar un registro de examen
            try {
                TakenExam exam = TakenExam.findById(req.params(":id"));
                if (exam != null) exam.delete();
                // res: éxito
                res.redirect("/examen/list?message=" + URLEncoder.encode("Examen eliminado.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/examen/list?error=" + URLEncoder.encode("Error al eliminar examen.", StandardCharsets.UTF_8));
            }
            return null;
        });
        get("/api/estudiantes", (req, res) -> {
            // req: Endpoint GET de la API para devolver JSON con los estudiantes (útil para integraciones o AJAX)
            res.type("application/json");

            try {
                String legajo = req.queryParams("legajo");
                String carrera = req.queryParams("carrera");
                List<Student> students;

                // Búsqueda cruzada por legajo y carrera al mismo tiempo usando consultas SQL nativas con JOIN
                if (legajo != null && !legajo.isEmpty() && carrera != null && !carrera.isEmpty()) {
                    students = Student.findBySQL(
                        "SELECT students.* FROM students " +
                        "JOIN student_careers ON students.id = student_careers.id_student " +
                        "JOIN careers ON student_careers.id_career = careers.id " +
                        "WHERE students.legajo = ? AND (careers.nombre LIKE ? OR careers.codigo = ?)",
                        legajo, "%" + carrera + "%", carrera
                    );
                // Búsqueda solo por legajo (exacto)
                } else if (legajo != null && !legajo.isEmpty()) {
                    students = Student.where("legajo = ?", legajo);
                // Búsqueda solo por nombre o código de carrera
                } else if (carrera != null && !carrera.isEmpty()) {
                    students = Student.findBySQL(
                        "SELECT students.* FROM students " +
                        "JOIN student_careers ON students.id = student_careers.id_student " +
                        "JOIN careers ON student_careers.id_career = careers.id " +
                        "WHERE careers.nombre LIKE ? OR careers.codigo = ?",
                        "%" + carrera + "%", carrera
                    );
                // Si no hay parámetros, devuelve todos
                } else {
                    students = Student.findAll();
                }

                // Armamos el diccionario de datos para serializar a JSON
                List<Map<String, Object>> result = new ArrayList<>();
                for (Student s : students) {
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("id", s.getId());
                    studentMap.put("nombre", s.getName());
                    studentMap.put("apellido", s.getApellido());
                    studentMap.put("dni", s.getDni());
                    studentMap.put("legajo", s.getLegajo());
                    studentMap.put("situacion", s.getSituacion());
                    result.add(studentMap);
                }

                // res: Devolvemos código 200 y el JSON parseado por la librería Jackson (objectMapper)
                res.status(200);
                return objectMapper.writeValueAsString(result);

            } catch (Exception e) {
                // res: Si falla, 500 error en JSON
                res.status(500);
                return objectMapper.writeValueAsString(Map.of("error", "Error al obtener estudiantes: " + e.getMessage()));
            }
        });

        get("/api/profesores", (req, res) -> {
            res.type("application/json");

            try {
                List<Teacher> teachers = Teacher.findAll();
                List<Map<String, Object>> result = new ArrayList<>();

                for (Teacher t : teachers) {
                    Map<String, Object> teacherMap = new HashMap<>();
                    teacherMap.put("id", t.getId());
                    teacherMap.put("nombre", t.getName());
                    teacherMap.put("apellido", t.getApellido());
                    teacherMap.put("dni", t.getDni());
                    teacherMap.put("email", t.getEmail());
                    teacherMap.put("carrera", t.getCareer());
                    result.add(teacherMap);
                }

                res.status(200);
                return objectMapper.writeValueAsString(result);

            } catch (Exception e) {
                res.status(500);
                return objectMapper.writeValueAsString(Map.of("error", "Error al obtener profesores: " + e.getMessage()));
            }
        });

        get("/api/materias", (req, res) -> {
            res.type("application/json");

            try {
                List<Subject> subjects = Subject.findAll();
                List<Map<String, Object>> result = new ArrayList<>();

                for (Subject s : subjects) {
                    Map<String, Object> subjectMap = new HashMap<>();
                    subjectMap.put("id", s.getId());
                    subjectMap.put("nombre", s.getNombre());
                    subjectMap.put("codigo", s.getCodigo());
                    result.add(subjectMap);
                }

                res.status(200);
                return objectMapper.writeValueAsString(result);

            } catch (Exception e) {
                res.status(500);
                return objectMapper.writeValueAsString(
                    Map.of("error", "Error al obtener materias: " + e.getMessage()));
            }
        });

        get("/api/reportes/resumen", (req, res) -> {
            res.type("application/json");

            try {
                Map<String, Object> resumen = new HashMap<>();
                resumen.put("total_estudiantes", Student.count());
                resumen.put("total_profesores", Teacher.count());
                resumen.put("total_materias", Subject.count());

                res.status(200);
                return objectMapper.writeValueAsString(resumen);

            } catch (Exception e) {
                res.status(500);
                return objectMapper.writeValueAsString(Map.of("error", "Error al generar reporte: " + e.getMessage()));
            }
        });
    }
}