package com.puntocoma.back.dto;

public class RegistroRequest {
    private String nombre;
    private String email;
    private String password;

    // Getters y Setters (Alt + Insert)
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}