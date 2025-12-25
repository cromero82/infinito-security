package com.infinitosoft.infinitosecurity.controller;

import com.infinitosoft.infinitosecurity.dto.LoginRequest;
import com.infinitosoft.infinitosecurity.dto.RegisterRequest;
import com.infinitosoft.infinitosecurity.dto.TokenRequest;
import com.infinitosoft.infinitosecurity.dto.UpdateUserRequest;
import com.infinitosoft.infinitosecurity.dto.UserInfo;
import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.service.AuthService;
import lombok.RequiredArgsConstructor;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegisterRequest request) {
        try {
            Usuario u = authService.crearUsuario(request.getNombre(), request.getCorreoElectronico(), request.getContrasena(), request.getTelefono());
            return ResponseEntity.status(HttpStatus.CREATED).body(u.getId());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.iniciarSesion(request.getCorreoElectronico(), request.getContrasena());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody TokenRequest req) {
        boolean valid = authService.validarToken(req.getToken());
        return ResponseEntity.ok(valid);
    }

    @PostMapping("/is-expired")
    public ResponseEntity<?> isExpired(@RequestBody TokenRequest req) {
        boolean expired = authService.esTokenExpirado(req.getToken());
        return ResponseEntity.ok(expired);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody TokenRequest req) {
        authService.finalizarSesion(req.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/claims")
    public ResponseEntity<UserInfo> claims(@RequestBody TokenRequest req) {
        return ResponseEntity.ok(authService.getClaims(req.getToken()));
    }

    @GetMapping("/usuario-id")
    public ResponseEntity<?> obtenerIdPorCorreo(@RequestParam("correoElectronico") String correoElectronico) {
        try {
            UUID id = authService.obtenerIdPorCorreo(correoElectronico);
            return ResponseEntity.ok(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/restaurar-contrasena")
    public ResponseEntity<?> restaurarContrasena(@RequestParam("correoElectronico") String correoElectronico) {
        try {
            authService.restaurarContrasena(correoElectronico);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/actualizar-usuario")
    public ResponseEntity<?> actualizarUsuario(@RequestBody UpdateUserRequest request, @RequestHeader("Authorization") String token) {
        try {
            Usuario u = authService.actualizarUsuario(request, token);
            return ResponseEntity.ok(u.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @GetMapping("/usuarios-no-admin")
    public ResponseEntity<List<Usuario>> listarUsuariosNoAdmin() {
        return ResponseEntity.ok(authService.listarUsuariosNoAdmin());
    }
}
