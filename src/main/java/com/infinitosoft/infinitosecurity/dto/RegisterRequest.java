package com.infinitosoft.infinitosecurity.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String nombre;
    private String correoElectronico;
    private String contrasena;
    private String telefono;
}
