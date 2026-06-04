package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("careers")
public class Career extends Model {

    public String getCodigo() {
        return getString("codigo");
    }

    public void setCodigo(String codigo) {
        set("codigo", codigo);
    }

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public Integer getDuracion() {
        return getInteger("duracion");
    }

    public void setDuracion(Integer duracion) {
        set("duracion", duracion);
    }
}
