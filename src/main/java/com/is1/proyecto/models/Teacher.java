package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teachers") // Esta anotación asocia explícitamente el modelo 'Teacher' con la tabla 'teachers' en la DB.
public class Teacher extends Model {
    
    private Person getPerson() {
        return parent(Person.class);
    }

    public Integer getIdPerson() {
        return getInteger("id_person");
    }

    public void setIdPerson(Integer idPerson) {
        set("id_person", idPerson);
    }

    public String getCareer(){
        return getString("career");
    }

    public void setCareer(String career){
        set("career", career);
    }

    // Delegación simple a Person
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