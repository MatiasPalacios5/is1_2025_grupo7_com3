package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import org.javalite.activejdbc.annotations.BelongsTo;

@Table("students")
@BelongsTo(foreignKeyName = "id_person", parent = Person.class)
public class Student extends Model {

    public Person getPerson() {
        return parent(Person.class);
    }

    public Integer getIdPerson() {
        return getInteger("id_person");
    }

    public void setIdPerson(Integer idPerson) {
        set("id_person", idPerson);
    }

    public String getLegajo() {
        return getString("legajo");
    }

    public void setLegajo(String legajo) {
        set("legajo", legajo);
    }

    public String getSituacion() {
        return getString("situacion");
    }

    public void setSituacion(String situacion) {
        set("situacion", situacion);
    }

    // Delegación a Person
    public String getName() {
        return getPerson().getString("name");
    }

    public String getApellido() {
        return getPerson().getString("apellido");
    }

    public Integer getDni() {
        return getPerson().getInteger("dni");
    }
}
