package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teachers") // Esta anotación asocia explícitamente el modelo 'Teacher' con la tabla 'teachers' en la DB.
public class Teacher extends Model {
    
    public String getName(){
        return getString("name");
    }

    public void setName(String name){
        set("name", name);
    }

    public String getCareer(){
        return getString("career");
    }

    public void setCareer(String career){
        set("career", career);
    }

}   