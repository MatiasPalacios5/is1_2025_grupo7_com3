package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.BelongsToParents;
import java.time.LocalDate;

@Table("taken_exams")
@BelongsToParents({
    @BelongsTo(foreignKeyName="id_student",parent=Student.class),
    @BelongsTo(foreignKeyName="id_subject",parent=Subject.class)
})
public class TakenExam extends Model {
    
    public Student getStudent() {
        return parent(Student.class);
    }
    
    public Subject getSubject() {
        return parent(Subject.class);
    }
    
    public String getStudentName() {
        Student s = getStudent();
        if (s != null) {
            return s.getApellido() + ", " + s.getName();
        }
        return "Estudiante desconocido";
    }
    
    public String getSubjectName() {
        Subject s = getSubject();
        return (s != null) ? s.getString("nombre") : "Materia desconocida";
    }
    
    public LocalDate getFecha() {
        java.sql.Date sqlDate = getDate("fecha");
        return (sqlDate != null) ? sqlDate.toLocalDate() : null;
    }
    
    public Integer getNota() {
        return getInteger("nota");
    }
}
