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
import com.is1.proyecto.models.TeacherSubject;

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

        before((req, res) -> {
            try {
                if (!Base.hasConnection()) {
                    Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                }
                System.out.println(req.url());
            } catch (Exception e) {
                System.err.println("Error al abrir conexión con ActiveJDBC: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}");
            }
        });
        after((req, res) -> {
            try {
                Base.close();
            } catch (Exception e) {
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });

        get("/user/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine());

        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");

            if (currentUsername == null || loggedIn == null || !loggedIn) {
                System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión para acceder a esta página.",
                        StandardCharsets.UTF_8));
                return null;
            }

            model.put("username", currentUsername);

            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine());

        get("/logout", (req, res) -> {
            req.session().invalidate();
            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");
            res.redirect("/");
            return null;
        });

        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());

        get("/user/new", (req, res) -> {
            return new ModelAndView(new HashMap<>(), "user_form.mustache");
        }, new MustacheTemplateEngine());

        post("/user/new", (req, res) -> {
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                res.redirect("/user/create?error="
                        + URLEncoder.encode("Nombre y contraseña son requeridos.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                User ac = new User();
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                ac.set("name", name);
                ac.set("password", hashedPassword);
                ac.saveIt();

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
            Map<String, Object> model = new HashMap<>();

            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");

            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.status(400);
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache");
            }

            User ac = User.findFirst("name = ?", username);

            if (ac == null) {
                res.status(401);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                return new ModelAndView(model, "login.mustache");
            }

            String storedHashedPassword = ac.getString("password");

            if (BCrypt.checkpw(plainTextPassword, storedHashedPassword)) {
                res.status(200);

                req.session(true).attribute("currentUserUsername", username);
                req.session().attribute("userId", ac.getId());
                req.session().attribute("loggedIn", true);

                System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
                System.out.println("DEBUG: ID de Sesión: " + req.session().id());

                model.put("username", username);
                return new ModelAndView(model, "dashboard.mustache");
            } else {
                res.status(401);
                System.out.println("DEBUG: Intento de login fallido para: " + username);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                return new ModelAndView(model, "login.mustache");
            }
        }, new MustacheTemplateEngine());

        post("/add_users", (req, res) -> {
            res.type("application/json");

            String name = req.queryParams("name");
            String password = req.queryParams("password");

            if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
            }

            try {
                User newUser = new User();
                newUser.set("name", name);
                newUser.set("password", password);
                newUser.saveIt();

                res.status(201);
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

        get("/profesor/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error="
                        + URLEncoder.encode("Debes iniciar sesión para acceder.", StandardCharsets.UTF_8));
                return null;
            }

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

            return new ModelAndView(model, "teacher_form.mustache");
        }, new MustacheTemplateEngine());

        post("/profesor/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String career = req.queryParams("career");
            String email = req.queryParams("email");

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
                dniInt = Integer.parseInt(dni);
            } catch (NumberFormatException e) {
                res.status(400);
                res.redirect("/profesor/create?error="
                        + URLEncoder.encode("El DNI debe contener solo números.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Person existingPerson = Person.findFirst("dni = ?", dniInt);
                if (existingPerson != null) {
                    System.out.println("DEBUG: DNI duplicado encontrado: " + dniInt);
                    res.status(400);
                    res.redirect("/profesor/create?errorDni="
                            + URLEncoder.encode("El DNI ya está registrado en el sistema.", StandardCharsets.UTF_8));
                    return "";
                }

                Teacher existingTeacher = Teacher.findFirst("email = ?", email);
                if (existingTeacher != null) {
                    System.out.println("DEBUG: Email duplicado encontrado: " + email);
                    res.status(400);
                    res.redirect("/profesor/create?errorEmail="
                            + URLEncoder.encode("El email introducido ya existe.", StandardCharsets.UTF_8));
                    return "";
                }

                Person newPerson = new Person();
                newPerson.set("dni", dniInt);
                newPerson.set("name", nombre);
                newPerson.set("apellido", apellido);
                newPerson.saveIt();

                System.out.println("DEBUG: Person creada con ID: " + newPerson.getId());

                Integer personId = newPerson.getInteger("id");

                Teacher newTeacher = new Teacher();
                newTeacher.set("id_person", personId);
                newTeacher.set("career", career);
                newTeacher.set("email", email);
                newTeacher.saveIt();

                System.out.println("DEBUG: Teacher creado exitosamente");

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

        get("/estudiante/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión para acceder.", StandardCharsets.UTF_8));
                return null;
            }

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

            return new ModelAndView(model, "student_form.mustache");
        }, new MustacheTemplateEngine());

        post("/estudiante/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String legajo = req.queryParams("legajo");
            String situacion = req.queryParams("situacion");

            if (nombre == null || nombre.isEmpty() ||
                apellido == null || apellido.isEmpty() ||
                dni == null || dni.isEmpty() ||
                legajo == null || legajo.isEmpty() ||
                situacion == null || situacion.isEmpty()) {

                res.status(400);
                res.redirect("/estudiante/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            Integer dniInt;
            try {
                dniInt = Integer.parseInt(dni);
            } catch (NumberFormatException e) {
                res.status(400);
                res.redirect("/estudiante/create?error=" + URLEncoder.encode("El DNI debe contener solo números.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Person existingPerson = Person.findFirst("dni = ?", dniInt);
                if (existingPerson != null) {
                    res.status(400);
                    res.redirect("/estudiante/create?errorDni=" + URLEncoder.encode("El DNI ya está registrado en el sistema.", StandardCharsets.UTF_8));
                    return "";
                }

                Student existingStudent = Student.findFirst("legajo = ?", legajo);
                if (existingStudent != null) {
                    res.status(400);
                    res.redirect("/estudiante/create?errorLegajo=" + URLEncoder.encode("El legajo ya está registrado en el sistema.", StandardCharsets.UTF_8));
                    return "";
                }

                Person newPerson = new Person();
                newPerson.set("dni", dniInt);
                newPerson.set("name", nombre);
                newPerson.set("apellido", apellido);
                newPerson.saveIt();

                Integer personId = newPerson.getInteger("id");

                Student newStudent = new Student();
                newStudent.set("id_person", personId);
                newStudent.set("legajo", legajo);
                newStudent.set("situacion", situacion);
                newStudent.saveIt();

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
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión para acceder.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("students", Student.findAll());
            return new ModelAndView(model, "student_list.mustache");
        }, new MustacheTemplateEngine());

        post("/estudiante/delete/:id", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión para acceder.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                String id = req.params(":id");
                Student student = Student.findById(Integer.parseInt(id));
                if (student != null) {
                    Integer idPerson = student.getIdPerson();
                    student.delete();
                    Person person = Person.findById(idPerson);
                    if (person != null) person.delete();
                }
                res.redirect("/estudiante/list?message=" + URLEncoder.encode("Estudiante eliminado correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/estudiante/list?error=" + URLEncoder.encode("Error al eliminar el estudiante.", StandardCharsets.UTF_8));
            }
            return "";
        });

        get("/materia/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

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

            return new ModelAndView(model, "subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");

            if (nombre == null || nombre.isEmpty() ||
                codigo == null || codigo.isEmpty()) {
                res.redirect("/materia/create?error=" +
                    URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Subject existing = Subject.findFirst("codigo = ?", codigo);
                if (existing != null) {
                    res.redirect("/materia/create?errorCodigo=" +
                        URLEncoder.encode("El código ya está registrado.", StandardCharsets.UTF_8));
                    return "";
                }

                Subject subject = new Subject();
                subject.set("nombre", nombre);
                subject.set("codigo", codigo);
                subject.saveIt();

                res.redirect("/materia/create?message=" +
                    URLEncoder.encode("Materia " + nombre + " registrada exitosamente.", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                System.err.println("ERROR al registrar materia: " + e.getMessage());
                res.redirect("/materia/create?error=" +
                    URLEncoder.encode("Error interno al registrar la materia.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/materia/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            model.put("subjects", Subject.findAll());
            return new ModelAndView(model, "subject_list.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/delete/:id", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                Integer id = Integer.parseInt(req.params(":id"));
                Subject subject = Subject.findById(id);
                if (subject != null) {
                    subject.delete();
                }
                res.redirect("/materia/list?message=" +
                    URLEncoder.encode("Materia eliminada correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/materia/list?error=" +
                    URLEncoder.encode("Error al eliminar la materia.", StandardCharsets.UTF_8));
            }
            return "";
        });

        get("/profesor/materias/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                Integer idTeacher = Integer.parseInt(req.params(":id"));
                Teacher teacher = Teacher.findById(idTeacher);

                if (teacher == null) {
                    res.redirect("/dashboard?error=" +
                        URLEncoder.encode("Profesor no encontrado.", StandardCharsets.UTF_8));
                    return null;
                }

                List<TeacherSubject> asignaciones = TeacherSubject.where("id_teacher = ?", idTeacher);
                model.put("teacher", teacher);
                model.put("asignaciones", asignaciones);

                return new ModelAndView(model, "teacher_subjects_list.mustache");
            } catch (Exception e) {
                res.redirect("/dashboard?error=" +
                    URLEncoder.encode("Error al obtener materias del profesor.", StandardCharsets.UTF_8));
                return null;
            }
        }, new MustacheTemplateEngine());

        get("/asignacion/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            // Cargar profesores y materias para los desplegables
            model.put("teachers", Teacher.findAll());
            model.put("subjects", Subject.findAll());

            return new ModelAndView(model, "teacher_subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/asignacion/new", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            String idTeacher = req.queryParams("id_teacher");
            String idSubject = req.queryParams("id_subject");
            String periodo   = req.queryParams("periodo");

            if (idTeacher == null || idTeacher.isEmpty() ||
                idSubject == null || idSubject.isEmpty() ||
                periodo == null || periodo.isEmpty()) {

                res.redirect("/asignacion/create?error=" +
                    URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                // Validar que no exista ya esa asignación en el mismo período
                TeacherSubject existing = TeacherSubject.findFirst(
                    "id_teacher = ? AND id_subject = ? AND periodo = ?",
                    Integer.parseInt(idTeacher),
                    Integer.parseInt(idSubject),
                    periodo
                );

                if (existing != null) {
                    res.redirect("/asignacion/create?error=" +
                        URLEncoder.encode("El profesor ya está asignado a esa materia en ese período.", StandardCharsets.UTF_8));
                    return "";
                }

                TeacherSubject ts = new TeacherSubject();
                ts.set("id_teacher", Integer.parseInt(idTeacher));
                ts.set("id_subject", Integer.parseInt(idSubject));
                ts.set("periodo", periodo);
                ts.saveIt();

                res.redirect("/asignacion/create?message=" +
                    URLEncoder.encode("Asignación registrada exitosamente.", StandardCharsets.UTF_8));
                return "";

            } catch (Exception e) {
                System.err.println("ERROR al registrar asignación: " + e.getMessage());
                res.redirect("/asignacion/create?error=" +
                    URLEncoder.encode("Error interno al registrar la asignación.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/asignacion/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            model.put("asignaciones", TeacherSubject.findAll());
            return new ModelAndView(model, "teacher_subject_list.mustache");
        }, new MustacheTemplateEngine());

        post("/asignacion/delete/:id", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/login?error=" + URLEncoder.encode("Debes iniciar sesión.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                Integer id = Integer.parseInt(req.params(":id"));
                TeacherSubject ts = TeacherSubject.findById(id);
                if (ts != null) {
                    ts.delete();
                }
                res.redirect("/asignacion/list?message=" +
                    URLEncoder.encode("Asignación eliminada correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/asignacion/list?error=" +
                    URLEncoder.encode("Error al eliminar la asignación.", StandardCharsets.UTF_8));
            }
            return "";
        });

        get("/api/estudiantes", (req, res) -> {
            res.type("application/json");

            try {
                String legajo = req.queryParams("legajo");
                List<Student> students;

                if (legajo != null && !legajo.isEmpty()) {
                    students = Student.where("legajo = ?", legajo);
                } else {
                    students = Student.findAll();
                }

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

                res.status(200);
                return objectMapper.writeValueAsString(result);

            } catch (Exception e) {
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