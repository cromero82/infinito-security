package com.infinitosoft.infinitosecurity.controller;

import com.infinitosoft.infinitosecurity.dto.LoginRequest;
import com.infinitosoft.infinitosecurity.dto.RegisterRequest;
import com.infinitosoft.infinitosecurity.dto.TokenRequest;
import com.infinitosoft.infinitosecurity.dto.UpdateUserRequest;
import com.infinitosoft.infinitosecurity.dto.UserInfo;
import com.infinitosoft.infinitosecurity.dto.UsuarioRolDto;
import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegisterRequest request) {
        log.info("Petición de registro para el correo: {}", request.getCorreoElectronico());
        try {
            Usuario u = authService.crearUsuario(request.getNombre(), request.getCorreoElectronico(), request.getContrasena(), request.getTelefono());
            log.info("Usuario registrado correctamente con ID: {}", u.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(u.getId());
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al registrar usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al registrar usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Intento de login para el correo: {}", request.getCorreoElectronico());
        try {
            String token = authService.iniciarSesion(request.getCorreoElectronico(), request.getContrasena());
            log.info("Login exitoso para el correo: {}", request.getCorreoElectronico());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("Error en login para el correo {}: {}", request.getCorreoElectronico(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/login-guest")
    public ResponseEntity<?> loginGuest() {
        log.info("Intento de login para invitado");
        try {
            String token = authService.iniciarSesionInvitado();
            log.info("Login de invitado exitoso");
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("Error en login de invitado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody TokenRequest req) {
        log.info("Validando token");
        boolean valid = authService.validarToken(req.getToken());
        log.info("Resultado de validación: {}", valid);
        return ResponseEntity.ok(valid);
    }

    @PostMapping("/is-expired")
    public ResponseEntity<?> isExpired(@RequestBody TokenRequest req) {
        log.info("Verificando expiración de token");
        boolean expired = authService.esTokenExpirado(req.getToken());
        log.info("¿Token expirado?: {}", expired);
        return ResponseEntity.ok(expired);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody TokenRequest req) {
        log.info("Petición de logout recibida");
        authService.finalizarSesion(req.getToken());
        log.info("Sesión finalizada correctamente");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/claims")
    public ResponseEntity<UserInfo> claims(@RequestBody TokenRequest req) {
        log.info("Petición para obtener claims del token");
        UserInfo userInfo = authService.getClaims(req.getToken());
        log.info("Claims obtenidos para el usuario: {}", userInfo.getNombre());
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/usuario-id")
    public ResponseEntity<?> obtenerIdPorCorreo(@RequestParam("correoElectronico") String correoElectronico) {
        log.info("Buscando ID para el correo: {}", correoElectronico);
        try {
            UUID id = authService.obtenerIdPorCorreo(correoElectronico);
            log.info("ID encontrado: {}", id);
            return ResponseEntity.ok(id);
        } catch (IllegalArgumentException e) {
            log.warn("Usuario no encontrado para el correo: {}", correoElectronico);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/usuario/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable("id") UUID id) {
        log.info("Obteniendo información del usuario con ID: {}", id);
        try {
            UserInfo u = authService.obtenerUsuarioPorId(id);
            log.info("Información del usuario obtenida correctamente");
            return ResponseEntity.ok(u);
        } catch (IllegalArgumentException e) {
            log.warn("Usuario no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/restaurar-contrasena")
    public ResponseEntity<?> restaurarContrasena(@RequestParam("correoElectronico") String correoElectronico) {
        log.info("Petición para restaurar contraseña para el correo: {}", correoElectronico);
        try {
            authService.restaurarContrasena(correoElectronico);
            log.info("Correo de restauración enviado correctamente");
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error al restaurar contraseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Error de estado al restaurar contraseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al restaurar contraseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/actualizar-usuario")
    public ResponseEntity<?> actualizarUsuario(@RequestBody UpdateUserRequest request, @RequestHeader("Authorization") String token) {
        log.info("Petición para actualizar información de usuario");
        try {
            Usuario u = authService.actualizarUsuario(request, token);
            log.info("Usuario actualizado correctamente con ID: {}", u.getId());
            return ResponseEntity.ok(u.getId());
        } catch (IllegalArgumentException e) {
            log.warn("Error en los datos de actualización: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al actualizar usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/actualizar-roles-usuarios")
    public ResponseEntity<?> actualizarRolesUsuarios(@RequestBody UsuarioRolDto request) {
        log.info("Petición para actualizar roles de usuarios");
        try {
            authService.actualizarRolesUsuarios(request);
            log.info("Roles actualizados correctamente");
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error al actualizar roles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al actualizar roles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        log.info("Petición para listar todos los usuarios");
        List<Usuario> usuarios = authService.listarUsuarios();
        log.info("Se encontraron {} usuarios", usuarios.size());
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/usuarios-no-admin")
    public ResponseEntity<List<Usuario>> listarUsuariosNoAdmin() {
        log.info("Petición para listar usuarios que no son administradores");
        List<Usuario> usuarios = authService.listarUsuariosNoAdmin();
        log.info("Se encontraron {} usuarios no administradores", usuarios.size());
        return ResponseEntity.ok(usuarios);
    }
}
