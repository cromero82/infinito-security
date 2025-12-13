package com.infinitosoft.infinitosecurity.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String correoElectronico;
    private String contrasena;
}
