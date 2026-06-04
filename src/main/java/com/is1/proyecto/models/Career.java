package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("careers")
public class Career extends Model {

    public Integer getCodigo() {
        return getInteger("codigo");
    }

    public void setCodigo(Integer codigo) {
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
