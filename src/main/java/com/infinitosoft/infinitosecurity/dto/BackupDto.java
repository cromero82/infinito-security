package com.infinitosoft.infinitosecurity.dto;

import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.model.UsuarioPerfil;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupDto {

    private List<Rol> roles;
    private List<Usuario> usuarios;
    private List<UsuarioPerfil> usuarioPerfiles;
}
