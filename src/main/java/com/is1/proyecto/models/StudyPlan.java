package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import org.javalite.activejdbc.annotations.BelongsTo;

@Table("study_plans")
@BelongsTo(foreignKeyName = "id_career", parent = Career.class)
public class StudyPlan extends Model {

    public Integer getYear() {
        return getInteger("year");
    }

    public Career getCareer() {
        return parent(Career.class);
    }

    public String getCareerName() {
        Career career = getCareer();
        if (career != null) {
            return career.getString("nombre");
        }
        return "Carrera Desconocida";
    }
}
