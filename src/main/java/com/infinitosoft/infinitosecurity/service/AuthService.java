package com.infinitosoft.infinitosecurity.service;

import com.infinitosoft.infinitosecurity.dto.UserInfo;
import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.repository.RolRepository;
import com.infinitosoft.infinitosecurity.repository.UsuarioRepository;
import com.infinitosoft.infinitosecurity.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public Usuario crearUsuario(String nombre, String correoElectronico, String contrasena, String telefono) {
        if (usuarioRepository.existsByCorreoElectronico(correoElectronico)) {
            throw new DataIntegrityViolationException("El correo electrónico ya existe");
        }
        // Asegurar que el rol 'invitado' exista; si no, crearlo de forma idempotente
        Rol invitado = rolRepository.findBySigla("invitado")
                .orElseGet(() -> rolRepository.save(Rol.builder()
                        .nombre("Invitado")
                        .sigla("invitado")
                        .build()));

        Usuario usuario = Usuario.builder()
                .nombre(nombre)
                .correoElectronico(correoElectronico)
                .contrasena(passwordEncoder.encode(contrasena))
                .telefono(telefono)
                .activo(true)
                .build();
        usuario.setRoles(Set.of(invitado));
        return usuarioRepository.save(usuario);
    }

    public String iniciarSesion(String correoElectronico, String contrasena) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña inválidos"));
        if (!usuario.getActivo()) {
            throw new IllegalStateException("Usuario inactivo");
        }
        if (!passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            throw new IllegalArgumentException("Usuario o contraseña inválidos");
        }

        String jti = UUID.randomUUID().toString();
        return jwtService.generateToken(jti, usuario);
    }

    public boolean validarToken(String token) {
        return jwtService.isTokenValid(token);
    }

    public boolean esTokenExpirado(String token) {
        return jwtService.isTokenExpired(token);
    }

    public void finalizarSesion(String token) {
        // Sin almacenamiento de tokens, finalizar sesión es un no-op.
    }

    public UserInfo getClaims(String token) {
        // Obtener claims desde el token (compatible con tokens nuevos y legados)
        UserInfo info = jwtService.getUserInfo(token);

        // Enriquecer roles con el nombre desde BD cuando venga nulo (tokens legados)
        var roles = info.getRoles();
        if (roles != null && !roles.isEmpty()) {
            roles.forEach(r -> {
                if (r != null && r.getNombre() == null && r.getSigla() != null) {
                    rolRepository.findBySigla(r.getSigla())
                            .ifPresent(dbRol -> r.setNombre(dbRol.getNombre()));
                }
            });
        }

        return info;
    }
}
