package com.infinitosoft.infinitosecurity.dto;

import com.infinitosoft.infinitosecurity.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    private String nombre;
    private String correoElectronico;
    private String telefono;
    private List<Rol> roles;
}
