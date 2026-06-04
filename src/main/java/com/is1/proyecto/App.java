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

        get("/profesor/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();



            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String q = req.queryParams("q");
            if (q != null && !q.isEmpty()) {
                model.put("searchQuery", q);
            }

            List<Teacher> teachers;
            if (q != null && !q.isEmpty()) {
                teachers = Teacher.findBySQL(
                    "SELECT teachers.* FROM teachers " +
                    "JOIN persons ON teachers.id_person = persons.id " +
                    "WHERE persons.apellido LIKE ? OR persons.name LIKE ? OR CAST(persons.dni AS TEXT) LIKE ?", 
                    "%" + q + "%", "%" + q + "%", "%" + q + "%"
                );
            } else {
                teachers = Teacher.findAll();
            }
            List<Map<String, Object>> teachersModel = new ArrayList<>();
            for (Teacher t : teachers) {
                Map<String, Object> tMap = new HashMap<>();
                tMap.put("id", t.getId());
                tMap.put("name", t.getName());
                tMap.put("apellido", t.getApellido());
                tMap.put("dni", t.getDni());
                tMap.put("email", t.getEmail());
                tMap.put("career", t.getCareer());
                teachersModel.add(tMap);
            }
            model.put("teachers", teachersModel);
            return new ModelAndView(model, "teacher_list.mustache");
        }, new MustacheTemplateEngine());

        post("/profesor/delete/:id", (req, res) -> {


            try {
                Integer id = Integer.parseInt(req.params(":id"));
                Teacher teacher = Teacher.findById(id);
                if (teacher != null) {
                    org.javalite.activejdbc.Base.exec("DELETE FROM period_teacher_subject WHERE id_teacher = ?", teacher.getId());
                    Integer idPerson = teacher.getIdPerson();
                    teacher.delete();
                    Person person = Person.findById(idPerson);
                    if (person != null) person.delete();
                }
                res.redirect("/profesor/list?message=" +
                    URLEncoder.encode("Profesor eliminado correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/profesor/list?error=" +
                    URLEncoder.encode("Error al eliminar el profesor.", StandardCharsets.UTF_8));
            }
            return "";
        });

        get("/estudiante/perfil/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            Student s = Student.findById(req.params("id"));
            if (s == null) {
                res.redirect("/estudiante/list?error=" + URLEncoder.encode("Estudiante no encontrado.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("name", s.getName());
            model.put("apellido", s.getApellido());
            model.put("dni", s.getDni());
            model.put("legajo", s.getLegajo());
            model.put("situacion", s.getSituacion());

            List<Map> scRows = org.javalite.activejdbc.Base.findAll("SELECT c.nombre FROM careers c JOIN student_careers sc ON c.id = sc.id_career WHERE sc.id_student = ?", s.getId());
            List<String> careerNames = new ArrayList<>();
            for (Map row : scRows) {
                careerNames.add(row.get("nombre").toString());
            }
            model.put("carreras", String.join(", ", careerNames));

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

            return new ModelAndView(model, "student_profile.mustache");
        }, new MustacheTemplateEngine());

        get("/estudiante/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

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

            model.put("careers", Career.findAll());

            return new ModelAndView(model, "student_form.mustache");
        }, new MustacheTemplateEngine());

        post("/estudiante/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String legajo = req.queryParams("legajo");
            String situacion = req.queryParams("situacion");
            String[] idCareersStr = req.queryParamsValues("id_careers");

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

                for (String idC : idCareersStr) {
                    StudentCareer studentCareer = new StudentCareer();
                    studentCareer.set("id_student", newStudent.getId());
                    studentCareer.set("id_career", Integer.parseInt(idC));
                    studentCareer.saveIt();
                }

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

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            String errorMessage = req.queryParams("error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.put("errorMessage", errorMessage);
            }

            String q = req.queryParams("q");
            if (q != null && !q.isEmpty()) {
                model.put("searchQuery", q);
            }

            List<Student> students;
            if (q != null && !q.isEmpty()) {
                students = Student.findBySQL(
                    "SELECT students.* FROM students " +
                    "JOIN persons ON students.id_person = persons.id " +
                    "WHERE students.legajo LIKE ? OR persons.apellido LIKE ? OR persons.name LIKE ? OR CAST(persons.dni AS TEXT) LIKE ?", 
                    "%" + q + "%", "%" + q + "%", "%" + q + "%", "%" + q + "%"
                );
            } else {
                students = Student.findAll();
            }
            List<Map<String, Object>> studentsModel = new ArrayList<>();
            for (Student s : students) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("legajo", s.getLegajo());
                sMap.put("situacion", s.getSituacion());
                sMap.put("name", s.getName());
                sMap.put("apellido", s.getApellido());
                sMap.put("dni", s.getDni());
                
                List<Map> scRows = org.javalite.activejdbc.Base.findAll("SELECT c.nombre FROM careers c JOIN student_careers sc ON c.id = sc.id_career WHERE sc.id_student = ?", s.getId());
                List<String> careerNames = new ArrayList<>();
                for (Map row : scRows) {
                    careerNames.add(row.get("nombre").toString());
                }
                sMap.put("carreras", String.join(", ", careerNames));
                studentsModel.add(sMap);
            }

            model.put("students", studentsModel);
            return new ModelAndView(model, "student_list.mustache");
        }, new MustacheTemplateEngine());

        get("/estudiante/edit/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);

            String idStr = req.params(":id");
            Student student = Student.findById(Integer.parseInt(idStr));
            if (student == null) {
                res.redirect("/estudiante/list?error=" + URLEncoder.encode("Estudiante no encontrado.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("student", student);
            
            if ("Ingresante".equals(student.getSituacion())) {
                model.put("isIngresante", true);
            } else if ("Efectivo".equals(student.getSituacion())) {
                model.put("isEfectivo", true);
            }

            List<StudentCareer> studentCareers = StudentCareer.find("id_student = ?", student.getId());
            List<Integer> currentCareerIds = new ArrayList<>();
            for (StudentCareer sc : studentCareers) {
                currentCareerIds.add(sc.getInteger("id_career"));
            }

            List<Career> careers = Career.findAll();
            List<Map<String, Object>> careersModel = new ArrayList<>();
            for (Career c : careers) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("nombre", c.getString("nombre"));
                cMap.put("codigo", c.getString("codigo"));
                if (currentCareerIds.contains(Integer.parseInt(c.getId().toString()))) {
                    cMap.put("selected", true);
                }
                careersModel.add(cMap);
            }
            model.put("careers", careersModel);

            return new ModelAndView(model, "student_form.mustache");
        }, new MustacheTemplateEngine());

        post("/estudiante/update/:id", (req, res) -> {
            String idStr = req.params(":id");
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String dni = req.queryParams("dni");
            String legajo = req.queryParams("legajo");
            String situacion = req.queryParams("situacion");
            String[] idCareersStr = req.queryParamsValues("id_careers");

            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty() || dni == null || dni.isEmpty() || legajo == null || legajo.isEmpty() || situacion == null || situacion.isEmpty() || idCareersStr == null || idCareersStr.length == 0) {
                res.redirect("/estudiante/edit/" + idStr + "?error=" + URLEncoder.encode("Todos los campos y al menos una carrera son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            if (!situacion.equalsIgnoreCase("ingresante") && !situacion.equalsIgnoreCase("efectivo")) {
                res.redirect("/estudiante/edit/" + idStr + "?error=" + URLEncoder.encode("La situación debe ser 'ingresante' o 'efectivo'.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Student student = Student.findById(Integer.parseInt(idStr));
                if (student != null) {
                    Person person = Person.findById(student.getIdPerson());
                    if (person != null) {
                        person.set("name", nombre);
                        person.set("apellido", apellido);
                        person.set("dni", Integer.parseInt(dni));
                        person.saveIt();
                    }
                    student.set("legajo", legajo);
                    student.set("situacion", situacion);
                    student.saveIt();

                    org.javalite.activejdbc.Base.exec("DELETE FROM student_careers WHERE id_student = ?", student.getId());
                    for (String idC : idCareersStr) {
                        StudentCareer studentCareer = new StudentCareer();
                        studentCareer.set("id_student", student.getId());
                        studentCareer.set("id_career", Integer.parseInt(idC));
                        studentCareer.saveIt();
                    }

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

        get("/materia/perfil/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            Subject sub = Subject.findById(req.params("id"));
            if (sub == null) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("nombre", sub.getString("nombre"));
            model.put("codigo", "[" + String.format("%04d", sub.getInteger("codigo")) + "]");
            model.put("duracion", sub.getString("duracion"));
            model.put("anoDictado", sub.getInteger("año_dictado"));
            model.put("cuatrimestreDictado", sub.getInteger("cuatrimestre_dictado"));
            
            StudyPlan plan = StudyPlan.findById(sub.get("id_study_plan"));
            model.put("planName", plan != null ? plan.getString("nombre") : "N/A");

            List<Map> scRows = org.javalite.activejdbc.Base.findAll(
                "SELECT p.codigo FROM subjects p " +
                "JOIN prerequisites pr ON p.id = pr.id_prerequisite " +
                "WHERE pr.id_subject = ?", sub.getId()
            );
            List<String> corrNames = new ArrayList<>();
            for (Map row : scRows) {
                Object codeObj = row.get("codigo") != null ? row.get("codigo") : row.get("CODIGO");
                if (codeObj != null) {
                    corrNames.add("[" + String.format("%04d", Integer.parseInt(codeObj.toString())) + "]");
                }
            }
            model.put("correlativas", corrNames.isEmpty() ? "Ninguna" : String.join(", ", corrNames));

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
                map.put("fecha", row.get("fecha") != null ? row.get("fecha").toString() : "");
                map.put("nota", row.get("nota"));
                alumnosModel.add(map);
            }
            model.put("alumnosAprobados", alumnosModel);

            return new ModelAndView(model, "subject_profile.mustache");
        }, new MustacheTemplateEngine());

        get("/materia/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

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

            return new ModelAndView(model, "subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/new", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");
            String idStudyPlanStr = req.queryParams("id_study_plan");
            String duracion = req.queryParams("duracion");
            String anoDictadoStr = req.queryParams("ano_dictado");
            String cuatrimestreDictadoStr = req.queryParams("cuatrimestre_dictado");
            String[] idPrerequisitesStr = req.queryParamsValues("id_prerequisites");

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
                codigoInt = Integer.parseInt(codigo);
                if (codigoInt < 1000 || codigoInt > 9999) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/materia/create?errorCodigo=" + URLEncoder.encode("El código debe ser un número entero de 4 dígitos.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Subject existing = Subject.findFirst("codigo = ?", codigoInt);
                if (existing != null) {
                    res.redirect("/materia/create?errorCodigo=" +
                        URLEncoder.encode("El código ya está registrado.", StandardCharsets.UTF_8));
                    return "";
                }
                
                int anoDictado = Integer.parseInt(anoDictadoStr);
                int cuatrimestreDictado = Integer.parseInt(cuatrimestreDictadoStr);

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
                            
                            if (pAno > anoDictado) {
                                res.redirect("/materia/create?error=" +
                                    URLEncoder.encode("No puedes tener como correlativa una materia de un año posterior.", StandardCharsets.UTF_8));
                                return "";
                            }
                            if (pAno == anoDictado && pCuatrimestre >= cuatrimestreDictado) {
                                res.redirect("/materia/create?error=" +
                                    URLEncoder.encode("Las correlativas del mismo año deben ser de un cuatrimestre estrictamente anterior.", StandardCharsets.UTF_8));
                                return "";
                            }
                        }
                    }
                }

                Subject subject = new Subject();
                subject.set("nombre", nombre);
                subject.set("codigo", codigoInt);
                subject.setDuracion(duracion);
                subject.set("id_study_plan", Integer.parseInt(idStudyPlanStr));
                subject.setAñoDictado(anoDictado);
                subject.setCuatrimestreDictado(cuatrimestreDictado);
                subject.saveIt();

                if (idPrerequisitesStr != null && idPrerequisitesStr.length > 0) {
                    for (String prereqIdStr : idPrerequisitesStr) {
                        Prerequisite p = new Prerequisite();
                        p.set("id_subject", subject.getId());
                        p.set("id_prerequisite", Integer.parseInt(prereqIdStr));
                        p.saveIt();
                    }
                }

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
            Map<String, Object> model = new HashMap<>();
            Subject subject = Subject.findById(req.params(":id"));
            if (subject == null) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }
            model.put("subject", subject);
            model.put("isAnual", "Anual".equals(subject.getDuracion()));
            model.put("isCuatrimestral", "Cuatrimestral".equals(subject.getDuracion()));
            model.put("isCuatrimestre1", subject.getCuatrimestreDictado() != null && subject.getCuatrimestreDictado() == 1);
            model.put("isCuatrimestre2", subject.getCuatrimestreDictado() != null && subject.getCuatrimestreDictado() == 2);

            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null) model.put("errorCodigo", errorCodigo);

            List<Prerequisite> prereqs = Prerequisite.where("id_subject = ?", subject.getId());
            List<Integer> assignedPrereqs = new ArrayList<>();
            for (Prerequisite p : prereqs) {
                assignedPrereqs.add(p.getInteger("id_prerequisite"));
            }

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

            List<Subject> subjects = Subject.findAll();
            List<Map<String, Object>> subjectsModel = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.getId().equals(subject.getId())) continue; // No puede ser correlativa de sí misma
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("nombre", s.getString("nombre"));
                sMap.put("codigo", s.getString("codigo"));
                sMap.put("id_study_plan", s.getInteger("id_study_plan"));
                if (assignedPrereqs.contains(Integer.parseInt(s.getId().toString()))) {
                    sMap.put("selected", true);
                }
                subjectsModel.add(sMap);
            }
            model.put("allSubjects", subjectsModel);

            return new ModelAndView(model, "subject_form.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/update/:id", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");
            String idStudyPlanStr = req.queryParams("id_study_plan");
            String duracion = req.queryParams("duracion");
            String anoDictadoStr = req.queryParams("ano_dictado");
            String cuatrimestreDictadoStr = req.queryParams("cuatrimestre_dictado");
            String[] idPrerequisitesStr = req.queryParamsValues("id_prerequisites");

            if (nombre == null || nombre.trim().isEmpty() || 
                codigo == null || codigo.trim().isEmpty() || 
                duracion == null || duracion.trim().isEmpty() || 
                idStudyPlanStr == null || idStudyPlanStr.isEmpty() ||
                anoDictadoStr == null || anoDictadoStr.isEmpty() ||
                cuatrimestreDictadoStr == null || cuatrimestreDictadoStr.isEmpty()) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("Todos los campos obligatorios deben estar completos.", StandardCharsets.UTF_8));
                return null;
            }

            int codigoInt;
            try {
                codigoInt = Integer.parseInt(codigo);
                if (codigoInt < 1000 || codigoInt > 9999) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código debe ser un número entero de 4 dígitos.", StandardCharsets.UTF_8));
                return null;
            }

            Subject subject = Subject.findById(req.params(":id"));
            if (subject == null) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            Subject existing = Subject.findFirst("codigo = ?", codigoInt);
            if (existing != null && !existing.getId().equals(subject.getId())) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código ya está en uso.", StandardCharsets.UTF_8));
                return null;
            }
            
            int anoDictado = Integer.parseInt(anoDictadoStr);
            int cuatrimestreDictado = Integer.parseInt(cuatrimestreDictadoStr);

            if ("Anual".equalsIgnoreCase(duracion) && cuatrimestreDictado == 2) {
                res.redirect("/materia/edit/" + req.params(":id") + "?errorCodigo=" +
                    URLEncoder.encode("Una materia anual no puede comenzar en el segundo cuatrimestre.", StandardCharsets.UTF_8));
                return null;
            }

            // Validaciones de correlativas
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

            subject.setNombre(nombre);
            subject.setCodigo(codigoInt);
            subject.setDuracion(duracion);
            subject.set("id_study_plan", Integer.parseInt(idStudyPlanStr));
            subject.setAñoDictado(anoDictado);
            subject.setCuatrimestreDictado(cuatrimestreDictado);
            subject.saveIt();

            org.javalite.activejdbc.Base.exec("DELETE FROM prerequisites WHERE id_subject = ?", subject.getId());
            if (idPrerequisitesStr != null && idPrerequisitesStr.length > 0) {
                for (String prereqIdStr : idPrerequisitesStr) {
                    Prerequisite p = new Prerequisite();
                    p.set("id_subject", subject.getId());
                    p.set("id_prerequisite", Integer.parseInt(prereqIdStr));
                    p.saveIt();
                }
            }

            res.redirect("/materia/list?message=" + URLEncoder.encode("Materia actualizada exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        get("/materia/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) {
                model.put("successMessage", successMessage);
            }

            List<Subject> subjects = Subject.findAll();
            List<Map<String, Object>> subjectsModel = new ArrayList<>();
            for (Subject s : subjects) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("id", s.getId());
                sMap.put("nombre", s.getNombre());
                sMap.put("codigo", "[" + String.format("%04d", s.getCodigo()) + "]");
                sMap.put("duracion", s.getDuracion());
                sMap.put("anoDictado", s.getAñoDictado());
                if ("Anual".equalsIgnoreCase(s.getDuracion())) {
                    sMap.put("cuatrimestreDictado", "1 y 2");
                } else {
                    sMap.put("cuatrimestreDictado", s.getCuatrimestreDictado());
                }

                StudyPlan p = StudyPlan.findById(s.getInteger("id_study_plan"));
                if (p != null) {
                    Career c = Career.findById(p.getInteger("id_career"));
                    if (c != null) {
                        sMap.put("planName", c.getString("nombre") + " (Plan " + p.getInteger("year") + ")");
                    } else {
                        sMap.put("planName", "Plan " + p.getInteger("year"));
                    }
                }

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

            model.put("subjects", subjectsModel);
            return new ModelAndView(model, "subject_list.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/delete/:id", (req, res) -> {
            try {
                Integer id = Integer.parseInt(req.params(":id"));
                Subject subject = Subject.findById(id);
                if (subject != null) {
                    subject.delete();
                    res.redirect("/materia/list?message=" + URLEncoder.encode("Materia eliminada.", StandardCharsets.UTF_8));
                } else {
                    res.redirect("/materia/list?error=" + URLEncoder.encode("Materia no encontrada.", StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                res.redirect("/materia/list?error=" + URLEncoder.encode("Error al eliminar la materia.", StandardCharsets.UTF_8));
            }
            return "";
        });

        // --- CARRERAS ---
        get("/carrera/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null) model.put("errorCodigo", errorCodigo);
            return new ModelAndView(model, "career_form.mustache");
        }, new MustacheTemplateEngine());

        post("/carrera/new", (req, res) -> {
            String codigoStr = req.queryParams("codigo");
            String nombre = req.queryParams("nombre");
            String duracionStr = req.queryParams("duracion");

            if (codigoStr == null || codigoStr.trim().isEmpty() || nombre == null || nombre.trim().isEmpty() || duracionStr == null || duracionStr.trim().isEmpty()) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            int duracion;
            try {
                duracion = Integer.parseInt(duracionStr);
                if (duracion <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("La duración debe ser un número entero positivo.", StandardCharsets.UTF_8));
                return null;
            }

            int codigo;
            try {
                codigo = Integer.parseInt(codigoStr);
            } catch (NumberFormatException e) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("El código debe ser numérico.", StandardCharsets.UTF_8));
                return null;
            }

            if (Career.findFirst("codigo = ?", codigo) != null) {
                res.redirect("/carrera/create?errorCodigo=" + URLEncoder.encode("El código de carrera ya existe.", StandardCharsets.UTF_8));
                return null;
            }

            Career career = new Career();
            career.set("codigo", codigo);
            career.set("nombre", nombre);
            career.set("duracion", duracion);
            career.saveIt();

            res.redirect("/carrera/list?message=" + URLEncoder.encode("Carrera registrada exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        get("/carrera/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            
            List<Career> allCareers = Career.findAll();
            List<Map<String, Object>> careersModel = new ArrayList<>();
            for (Career c : allCareers) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("id", c.getId());
                cMap.put("codigo", c.getInteger("codigo"));
                cMap.put("nombre", c.getString("nombre"));
                cMap.put("duracion", c.getInteger("duracion"));
                
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
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);
            return new ModelAndView(model, "career_list.mustache");
        }, new MustacheTemplateEngine());

        get("/carrera/edit/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            Career career = Career.findById(req.params(":id"));
            if (career == null) {
                res.redirect("/carrera/list?error=" + URLEncoder.encode("Carrera no encontrada.", StandardCharsets.UTF_8));
                return null;
            }
            model.put("career", career);
            String errorCodigo = req.queryParams("errorCodigo");
            if (errorCodigo != null) model.put("errorCodigo", errorCodigo);
            return new ModelAndView(model, "career_form.mustache");
        }, new MustacheTemplateEngine());

        post("/carrera/update/:id", (req, res) -> {
            String codigoStr = req.queryParams("codigo");
            String nombre = req.queryParams("nombre");
            String duracionStr = req.queryParams("duracion");

            if (codigoStr == null || codigoStr.trim().isEmpty() || nombre == null || nombre.trim().isEmpty() || duracionStr == null || duracionStr.trim().isEmpty()) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            int duracion;
            try {
                duracion = Integer.parseInt(duracionStr);
                if (duracion <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("La duración debe ser un número entero positivo.", StandardCharsets.UTF_8));
                return null;
            }

            int codigo;
            try {
                codigo = Integer.parseInt(codigoStr);
            } catch (NumberFormatException e) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código debe ser numérico.", StandardCharsets.UTF_8));
                return null;
            }

            Career career = Career.findById(req.params(":id"));
            if (career == null) {
                res.redirect("/carrera/list?error=" + URLEncoder.encode("Carrera no encontrada.", StandardCharsets.UTF_8));
                return null;
            }

            Career existing = Career.findFirst("codigo = ?", codigo);
            if (existing != null && !existing.getId().equals(career.getId())) {
                res.redirect("/carrera/edit/" + req.params(":id") + "?errorCodigo=" + URLEncoder.encode("El código de carrera ya está en uso.", StandardCharsets.UTF_8));
                return null;
            }

            career.set("codigo", codigo);
            career.set("nombre", nombre);
            career.set("duracion", duracion);
            career.saveIt();

            res.redirect("/carrera/list?message=" + URLEncoder.encode("Carrera actualizada exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        post("/carrera/delete/:id", (req, res) -> {
            Career career = Career.findById(req.params(":id"));
            if (career != null) {
                career.delete();
                res.redirect("/carrera/list?message=" + URLEncoder.encode("Carrera eliminada.", StandardCharsets.UTF_8));
            } else {
                res.redirect("/carrera/list?error=" + URLEncoder.encode("Carrera no encontrada.", StandardCharsets.UTF_8));
            }
            return null;
        });
        // --- PLANES DE ESTUDIO ---
        get("/plan/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);

            List<StudyPlan> plans = StudyPlan.findAll();
            List<Map<String, Object>> plansModel = new ArrayList<>();
            for (StudyPlan plan : plans) {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("id", plan.getId());
                pMap.put("year", plan.getInteger("year"));
                pMap.put("resolution", plan.getString("resolution"));
                Career career = Career.findById(plan.getInteger("id_career"));
                if (career != null) {
                    pMap.put("careerName", career.getString("nombre") + " (" + career.getString("codigo") + ")");
                }
                plansModel.add(pMap);
            }
            model.put("plans", plansModel);
            return new ModelAndView(model, "study_plan_list.mustache");
        }, new MustacheTemplateEngine());

        get("/plan/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);
            model.put("careers", Career.findAll());
            return new ModelAndView(model, "study_plan_form.mustache");
        }, new MustacheTemplateEngine());

        post("/plan/new", (req, res) -> {
            String yearStr = req.queryParams("year");
            String resolution = req.queryParams("resolution");
            String idCareerStr = req.queryParams("id_career");

            if (yearStr == null || yearStr.trim().isEmpty() || resolution == null || resolution.trim().isEmpty() || idCareerStr == null || idCareerStr.trim().isEmpty()) {
                res.redirect("/plan/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            StudyPlan plan = new StudyPlan();
            plan.set("year", Integer.parseInt(yearStr));
            plan.set("resolution", resolution);
            plan.set("id_career", Integer.parseInt(idCareerStr));
            plan.saveIt();

            res.redirect("/plan/list?message=" + URLEncoder.encode("Plan creado exitosamente.", StandardCharsets.UTF_8));
            return null;
        });

        get("/plan/edit/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);

            StudyPlan plan = StudyPlan.findById(req.params(":id"));
            if (plan == null) {
                res.redirect("/plan/list?error=" + URLEncoder.encode("Plan no encontrado.", StandardCharsets.UTF_8));
                return null;
            }

            model.put("plan", plan);
            
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

            return new ModelAndView(model, "study_plan_form.mustache");
        }, new MustacheTemplateEngine());

        post("/plan/update/:id", (req, res) -> {
            String yearStr = req.queryParams("year");
            String resolution = req.queryParams("resolution");
            String idCareerStr = req.queryParams("id_career");

            if (yearStr == null || yearStr.trim().isEmpty() || resolution == null || resolution.trim().isEmpty() || idCareerStr == null || idCareerStr.trim().isEmpty()) {
                res.redirect("/plan/edit/" + req.params(":id") + "?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            StudyPlan plan = StudyPlan.findById(req.params(":id"));
            if (plan != null) {
                plan.set("year", Integer.parseInt(yearStr));
                plan.set("resolution", resolution);
                plan.set("id_career", Integer.parseInt(idCareerStr));
                plan.saveIt();
                res.redirect("/plan/list?message=" + URLEncoder.encode("Plan actualizado exitosamente.", StandardCharsets.UTF_8));
            } else {
                res.redirect("/plan/list?error=" + URLEncoder.encode("Plan no encontrado.", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/plan/delete/:id", (req, res) -> {
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
        get("/periodo/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);
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
            return new ModelAndView(model, "academic_period_list.mustache");
        }, new MustacheTemplateEngine());

        get("/periodo/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            if (error != null) model.put("errorMessage", error);
            return new ModelAndView(model, "academic_period_form.mustache");
        }, new MustacheTemplateEngine());

        post("/periodo/new", (req, res) -> {
            String yearStr = req.queryParams("year");
            String semesterStr = req.queryParams("semester");

            if (yearStr == null || yearStr.trim().isEmpty() || semesterStr == null || semesterStr.trim().isEmpty()) {
                res.redirect("/periodo/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                AcademicPeriod existing = AcademicPeriod.findFirst("year = ? AND semester = ?", Integer.parseInt(yearStr), Integer.parseInt(semesterStr));
                if (existing != null) {
                    res.redirect("/periodo/create?error=" + URLEncoder.encode("El periodo ya existe.", StandardCharsets.UTF_8));
                    return null;
                }
                AcademicPeriod p = new AcademicPeriod();
                p.set("year", Integer.parseInt(yearStr));
                p.set("semester", Integer.parseInt(semesterStr));
                p.saveIt();
                res.redirect("/periodo/list?message=" + URLEncoder.encode("Periodo creado exitosamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/periodo/create?error=" + URLEncoder.encode("Error interno.", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/periodo/delete/:id", (req, res) -> {
            AcademicPeriod p = AcademicPeriod.findById(req.params(":id"));
            if (p != null) {
                p.delete();
                res.redirect("/periodo/list?message=" + URLEncoder.encode("Periodo eliminado.", StandardCharsets.UTF_8));
            } else {
                res.redirect("/periodo/list?error=" + URLEncoder.encode("Periodo no encontrado.", StandardCharsets.UTF_8));
            }
            return null;
        });

        // --- ASIGNACIONES ---
        get("/profesor/materias/:id", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            try {
                Integer idTeacher = Integer.parseInt(req.params(":id"));
                Teacher teacher = Teacher.findById(idTeacher);

                if (teacher == null) {
                    res.redirect("/dashboard?error=" + URLEncoder.encode("Profesor no encontrado.", StandardCharsets.UTF_8));
                    return null;
                }

                List<PeriodTeacherSubject> asignaciones = PeriodTeacherSubject.where("id_teacher = ?", idTeacher);
                List<Map<String, Object>> asignacionesModel = new ArrayList<>();
                for (PeriodTeacherSubject pts : asignaciones) {
                    Map<String, Object> map = new HashMap<>();
                    Subject s = Subject.findById(pts.get("id_subject"));
                    AcademicPeriod p = AcademicPeriod.findById(pts.get("id_academic_period"));
                    if (s != null) {
                        map.put("subjectName", s.getString("nombre"));
                        map.put("subjectCode", s.getString("codigo"));
                        map.put("subjectDuracion", s.getDuracion());
                        
                        StudyPlan sp = StudyPlan.findById(s.getInteger("id_study_plan"));
                        if (sp != null) {
                            Career c = Career.findById(sp.getInteger("id_career"));
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
            String idTeacher = req.queryParams("id_teacher");
            String idSubject = req.queryParams("id_subject");
            String idPeriodo = req.queryParams("id_academic_period");

            if (idTeacher == null || idTeacher.isEmpty() || idSubject == null || idSubject.isEmpty() || idPeriodo == null || idPeriodo.isEmpty()) {
                res.redirect("/asignacion/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return "";
            }

            try {
                Subject subject = Subject.findById(Integer.parseInt(idSubject));
                AcademicPeriod period = AcademicPeriod.findById(Integer.parseInt(idPeriodo));
                if (subject != null && period != null) {
                    if ("Anual".equals(subject.getDuracion()) && period.getInteger("semester") == 2) {
                        res.redirect("/asignacion/create?error=" + URLEncoder.encode("Una materia anual no puede comenzar a dictarse en el 2° cuatrimestre.", StandardCharsets.UTF_8));
                        return "";
                    }
                }

                PeriodTeacherSubject existing = PeriodTeacherSubject.findFirst("id_teacher = ? AND id_subject = ? AND id_academic_period = ?", Integer.parseInt(idTeacher), Integer.parseInt(idSubject), Integer.parseInt(idPeriodo));
                if (existing != null) {
                    res.redirect("/asignacion/create?error=" + URLEncoder.encode("El profesor ya está asignado a esa materia en ese período.", StandardCharsets.UTF_8));
                    return "";
                }

                PeriodTeacherSubject ts = new PeriodTeacherSubject();
                ts.set("id_teacher", Integer.parseInt(idTeacher));
                ts.set("id_subject", Integer.parseInt(idSubject));
                ts.set("id_academic_period", Integer.parseInt(idPeriodo));
                ts.saveIt();

                res.redirect("/asignacion/create?message=" + URLEncoder.encode("Asignación registrada exitosamente.", StandardCharsets.UTF_8));
                return "";
            } catch (Exception e) {
                res.redirect("/asignacion/create?error=" + URLEncoder.encode("Error interno al registrar la asignación.", StandardCharsets.UTF_8));
                return "";
            }
        });

        get("/asignacion/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null && !successMessage.isEmpty()) model.put("successMessage", successMessage);

            List<PeriodTeacherSubject> asignaciones = PeriodTeacherSubject.findAll();
            List<Map<String, Object>> asignacionesModel = new ArrayList<>();
            for (PeriodTeacherSubject pts : asignaciones) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", pts.getId());
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
            return new ModelAndView(model, "teacher_subject_list.mustache");
        }, new MustacheTemplateEngine());

        post("/asignacion/delete/:id", (req, res) -> {
            try {
                Integer id = Integer.parseInt(req.params(":id"));
                PeriodTeacherSubject ts = PeriodTeacherSubject.findById(id);
                if (ts != null) ts.delete();
                res.redirect("/asignacion/list?message=" + URLEncoder.encode("Asignación eliminada correctamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/asignacion/list?error=" + URLEncoder.encode("Error al eliminar la asignación.", StandardCharsets.UTF_8));
            }
            return "";
        });
        // --- EXÁMENES RENDIDOS ---
        get("/examen/list", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            if (successMessage != null) model.put("successMessage", successMessage);
            String errorMessage = req.queryParams("error");
            if (errorMessage != null) model.put("errorMessage", errorMessage);

            String subjectId = req.queryParams("subjectId");
            List<TakenExam> examenes;
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
                map.put("studentName", exam.getStudentName());
                map.put("subjectName", exam.getSubjectName());
                if (exam.getFecha() != null) map.put("fecha", exam.getFecha().toString());
                map.put("nota", exam.getNota());
                examenesModel.add(map);
            }
            model.put("examenes", examenesModel);

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
            String idStudent = req.queryParams("id_student");
            String idSubject = req.queryParams("id_subject");
            String fecha = req.queryParams("fecha");
            String notaStr = req.queryParams("nota");

            if (idStudent == null || idStudent.isEmpty() || idSubject == null || idSubject.isEmpty() ||
                fecha == null || fecha.isEmpty() || notaStr == null || notaStr.isEmpty()) {
                res.redirect("/examen/create?error=" + URLEncoder.encode("Todos los campos son obligatorios.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                int nota = Integer.parseInt(notaStr);
                if (nota < 1 || nota > 10) {
                    res.redirect("/examen/create?error=" + URLEncoder.encode("La nota debe estar entre 1 y 10.", StandardCharsets.UTF_8));
                    return null;
                }

                TakenExam exam = new TakenExam();
                exam.set("id_student", Integer.parseInt(idStudent));
                exam.set("id_subject", Integer.parseInt(idSubject));
                exam.set("fecha", java.sql.Date.valueOf(java.time.LocalDate.parse(fecha)));
                exam.set("nota", nota);
                exam.saveIt();

                res.redirect("/examen/list?message=" + URLEncoder.encode("Examen registrado exitosamente.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/examen/create?error=" + URLEncoder.encode("Error interno al registrar el examen. Verifique el formato de fecha (YYYY-MM-DD).", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/examen/delete/:id", (req, res) -> {
            try {
                TakenExam exam = TakenExam.findById(req.params(":id"));
                if (exam != null) exam.delete();
                res.redirect("/examen/list?message=" + URLEncoder.encode("Examen eliminado.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                res.redirect("/examen/list?error=" + URLEncoder.encode("Error al eliminar examen.", StandardCharsets.UTF_8));
            }
            return null;
        });
        get("/api/estudiantes", (req, res) -> {
            res.type("application/json");

            try {
                String legajo = req.queryParams("legajo");
                String carrera = req.queryParams("carrera");
                List<Student> students;

                if (legajo != null && !legajo.isEmpty() && carrera != null && !carrera.isEmpty()) {
                    students = Student.findBySQL(
                        "SELECT students.* FROM students " +
                        "JOIN student_careers ON students.id = student_careers.id_student " +
                        "JOIN careers ON student_careers.id_career = careers.id " +
                        "WHERE students.legajo = ? AND (careers.nombre LIKE ? OR careers.codigo = ?)",
                        legajo, "%" + carrera + "%", carrera
                    );
                } else if (legajo != null && !legajo.isEmpty()) {
                    students = Student.where("legajo = ?", legajo);
                } else if (carrera != null && !carrera.isEmpty()) {
                    students = Student.findBySQL(
                        "SELECT students.* FROM students " +
                        "JOIN student_careers ON students.id = student_careers.id_student " +
                        "JOIN careers ON student_careers.id_career = careers.id " +
                        "WHERE careers.nombre LIKE ? OR careers.codigo = ?",
                        "%" + carrera + "%", carrera
                    );
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