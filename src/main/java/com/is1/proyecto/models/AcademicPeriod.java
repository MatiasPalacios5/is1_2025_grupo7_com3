package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("academic_periods")
public class AcademicPeriod extends Model {
    public Integer getYear() {
        return getInteger("year");
    }
    public Integer getSemester() {
        return getInteger("semester");
    }
}
