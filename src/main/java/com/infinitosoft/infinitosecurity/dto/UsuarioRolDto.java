package com.infinitosoft.infinitosecurity.dto;

import lombok.Data;
import java.util.List;

@Data
public class UsuarioRolDto {
    private String correoElectronico;
    private List<String> roles;
}
