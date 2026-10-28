package com.lara.denunciasapp.model;

/** Los nombres coinciden con el JSON de Spring Boot. El DNI conserva ceros iniciales. */
public class Denuncia {
    private Integer id;
    private String dni;
    private String fecha;
    private String tipo;
    private String direccion;
    private String descripcion;
    private int estado;

    public Integer getId() { return id; }
    public String getDni() { return dni; }
    public String getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public String getDireccion() { return direccion; }
    public String getDescripcion() { return descripcion; }
    public int getEstado() { return estado; }
    public String getEstadoTexto() { return estado == 1 ? "Pendiente" : "Atendida"; }
}
