package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("persons")
public class Person extends Model {

    public Integer getDni() {
        return getInteger("dni");
    }

    public void setDni(Integer dni) {
        set("dni", dni);
    }

    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        set("name", name);
    }

    public String getApellido() {
        return getString("apellido");
    }

    public void setApellido(String apellido) {
        set("apellido", apellido);
    }
}
