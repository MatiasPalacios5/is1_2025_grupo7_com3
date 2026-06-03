package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teacher_subjects")
public class TeacherSubject extends Model {

    public Integer getIdTeacher() {
        return getInteger("id_teacher");
    }

    public void setIdTeacher(Integer idTeacher) {
        set("id_teacher", idTeacher);
    }

    public Integer getIdSubject() {
        return getInteger("id_subject");
    }

    public void setIdSubject(Integer idSubject) {
        set("id_subject", idSubject);
    }

    public String getPeriodo() {
        return getString("periodo");
    }

    public void setPeriodo(String periodo) {
        set("periodo", periodo);
    }

    // Métodos de conveniencia para obtener objetos relacionados
    public Teacher getTeacher() {
        return Teacher.findById(getIdTeacher());
    }

    public Subject getSubject() {
        return Subject.findById(getIdSubject());
    }
}
