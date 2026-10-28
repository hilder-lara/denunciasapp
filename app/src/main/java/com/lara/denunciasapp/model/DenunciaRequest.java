package com.lara.denunciasapp.model;

/** El servidor genera el identificador y establece el estado inicial. */
public class DenunciaRequest {
    private final String dni;
    private final String fecha;
    private final String tipo;
    private final String direccion;
    private final String descripcion;

    public DenunciaRequest(String dni, String fecha, String tipo, String direccion, String descripcion) {
        this.dni = dni;
        this.fecha = fecha;
        this.tipo = tipo;
        this.direccion = direccion;
        this.descripcion = descripcion;
    }
}
