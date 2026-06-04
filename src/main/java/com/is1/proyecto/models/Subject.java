package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("subjects")
public class Subject extends Model {

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public Integer getCodigo() {
        return getInteger("codigo");
    }

    public void setCodigo(Integer codigo) {
        set("codigo", codigo);
    }

    public String getDuracion() {
        return getString("duracion");
    }

    public void setDuracion(String duracion) {
        set("duracion", duracion);
    }

    public Integer getAñoDictado() {
        return getInteger("año_dictado");
    }

    public void setAñoDictado(Integer añoDictado) {
        set("año_dictado", añoDictado);
    }

    public Integer getCuatrimestreDictado() {
        return getInteger("cuatrimestre_dictado");
    }

    public void setCuatrimestreDictado(Integer cuatrimestreDictado) {
        set("cuatrimestre_dictado", cuatrimestreDictado);
    }
}
