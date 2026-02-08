package com.infinitosoft.infinitosecurity.service;

import com.infinitosoft.infinitosecurity.dto.UpdateUserRequest;
import com.infinitosoft.infinitosecurity.dto.UserInfo;
import com.infinitosoft.infinitosecurity.dto.UsuarioRolDto;
import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.repository.RolRepository;
import com.infinitosoft.infinitosecurity.repository.UsuarioRepository;
import com.infinitosoft.infinitosecurity.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.UUID;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${mail.service.url}")
    private String mailServiceUrl;

    @Transactional
    public Usuario crearUsuario(String nombre, String correoElectronico, String contrasena, String telefono) {
        logger.info("Iniciando servicio crearUsuario para correo: {}", correoElectronico);
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
        logger.info("Iniciando servicio iniciarSesion para correo: {}", correoElectronico);
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
        logger.info("Iniciando servicio validarToken");
        return jwtService.isTokenValid(token);
    }

    public boolean esTokenExpirado(String token) {
        logger.info("Iniciando servicio esTokenExpirado");
        return jwtService.isTokenExpired(token);
    }

    public void finalizarSesion(String token) {
        logger.info("Iniciando servicio finalizarSesion");
        // Sin almacenamiento de tokens, finalizar sesión es un no-op.
    }

    public UserInfo getClaims(String token) {
        logger.info("Iniciando servicio getClaims");
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

    public UUID obtenerIdPorCorreo(String correoElectronico) {
        logger.info("Iniciando servicio obtenerIdPorCorreo para correo: {}", correoElectronico);
        return usuarioRepository.findByCorreoElectronico(correoElectronico)
                .map(Usuario::getId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public UserInfo obtenerUsuarioPorId(UUID id) {
        logger.info("Iniciando servicio obtenerUsuarioPorId para id: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return UserInfo.builder()
                .nombre(usuario.getNombre())
                .correoElectronico(usuario.getCorreoElectronico())
                .telefono(usuario.getTelefono())
                .roles(new java.util.ArrayList<>(usuario.getRoles()))
                .build();
    }

    /**
     * Restaura la contraseña del usuario enviado por correo electrónico:
     * - Verifica existencia del usuario
     * - Genera una contraseña temporal (4 números + 3-4 letras + símbolo especial * o %)
     * - Envía la contraseña temporal por correo mediante el servicio externo configurado
     * - Si la respuesta es 2xx, actualiza en BD la contraseña (codificada)
     */
    @Transactional
    public void restaurarContrasena(String correoElectronico) {
        logger.info("Iniciando servicio restaurarContrasena para correo: {}", correoElectronico);
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String temporal = generarContrasenaTemporal();
        String html = construirHtmlContrasena(temporal);

        // Construir payload
        Map<String, String> payload = Map.of(
                "to", correoElectronico,
                "subject", "Generacion de contraseña temporal - Sistema GestorMarket",
                "message", html
        );

        // Enviar correo
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> req = new HttpEntity<>(payload, headers);
        ResponseEntity<String> resp = rt.postForEntity(mailServiceUrl, req, String.class);

        if (resp.getStatusCode().is2xxSuccessful()) {
            // Actualizar contraseña en BD con encoding
            usuario.setContrasena(passwordEncoder.encode(temporal));
            usuarioRepository.save(usuario);
        } else {
            throw new IllegalStateException("Fallo al enviar el correo de restauración: " + resp.getStatusCode());
        }
    }

    @Transactional
    public Usuario actualizarUsuario(UpdateUserRequest request, String token) {
        logger.info("Iniciando servicio actualizarUsuario");
        
        // Limpiar token si viene con Bearer
        String jwt = token;
        if (token != null && token.startsWith("Bearer ")) {
            jwt = token.substring(7);
        }

        // Extraer correo del token
        UserInfo userInfo = jwtService.getUserInfo(jwt);
        String correoActual = userInfo.getCorreoElectronico();

        if (correoActual == null || correoActual.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario desde el token");
        }

        boolean hasUpdates = false;
        if (request.getNombre() != null && !request.getNombre().isBlank()) hasUpdates = true;
        if (request.getCorreoElectronico() != null && !request.getCorreoElectronico().isBlank()) hasUpdates = true;
        if (request.getTelefono() != null && !request.getTelefono().isBlank()) hasUpdates = true;
        if (request.getContrasena() != null && !request.getContrasena().isBlank()) hasUpdates = true;

        if (!hasUpdates) {
            throw new IllegalArgumentException("Debe proporcionar al menos un campo para actualizar");
        }

        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoActual)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombre(request.getNombre());
        }

        if (request.getCorreoElectronico() != null && !request.getCorreoElectronico().isBlank()) {
            // Verificar si el nuevo correo ya existe en otro usuario
            usuarioRepository.findByCorreoElectronico(request.getCorreoElectronico())
                    .ifPresent(u -> {
                        if (!u.getId().equals(usuario.getId())) {
                            throw new DataIntegrityViolationException("El nuevo correo electrónico ya está en uso");
                        }
                    });
            usuario.setCorreoElectronico(request.getCorreoElectronico());
        }

        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            usuario.setTelefono(request.getTelefono());
        }

        if (request.getContrasena() != null && !request.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void actualizarRolesUsuarios(UsuarioRolDto dto) {
        logger.info("Iniciando servicio actualizarRolesUsuarios para usuario: {}", dto.getCorreoElectronico());
        
        if (dto.getCorreoElectronico() == null || dto.getCorreoElectronico().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        }

        Usuario usuario = usuarioRepository.findByCorreoElectronico(dto.getCorreoElectronico())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + dto.getCorreoElectronico()));

        if (dto.getRoles() != null) {
            Set<Rol> nuevosRoles = new HashSet<>();
            for (String sigla : dto.getRoles()) {
                Rol rol = rolRepository.findBySigla(sigla)
                        .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con sigla: " + sigla));
                nuevosRoles.add(rol);
            }
            usuario.setRoles(nuevosRoles);
            usuarioRepository.save(usuario);
        }
    }

    public List<Usuario> listarUsuarios() {
        logger.info("Iniciando servicio listarUsuarios");
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarUsuariosNoAdmin() {
        logger.info("Iniciando servicio listarUsuariosNoAdmin");
        return usuarioRepository.findAllByRolSiglaNotAdmin();
    }

    private String generarContrasenaTemporal() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        // 4 números
        for (int i = 0; i < 4; i++) sb.append(random.nextInt(10));
        // 3 o 4 letras (minúsculas aleatorias)
        int letras = 3 + random.nextInt(2); // 3 o 4
        for (int i = 0; i < letras; i++) {
            char c = (char) ('a' + random.nextInt(26));
            sb.append(c);
        }
        // símbolo especial * o %
        sb.append(random.nextBoolean() ? '*' : '%');
        return sb.toString();
    }

    private String construirHtmlContrasena(String password) {
        return "<div style='font-family: Arial, sans-serif; background-color:#f9f9f9; padding:20px; border-radius:8px; max-width:600px; margin:auto;'>" +
                "<h2 style='color:#4CAF50; text-align:center;'>Recuperación de contraseña</h2>" +
                "<p style='font-size:16px; color:#333;'>Hola,</p>" +
                "<p style='font-size:16px; color:#333;'>Se ha generado una contraseña temporal para tu acceso:</p>" +
                "<div style='background-color:#eee; padding:10px; border-radius:5px; text-align:center; font-size:18px; font-weight:bold; color:#000;'>" +
                password +
                "</div>" +
                "<p style='font-size:14px; color:#555; margin-top:15px;'>Por seguridad, te recomendamos cambiar esta contraseña lo antes posible por una que sea fácil de recordar y cumpla con tus estándares de seguridad.</p>" +
                "<hr style='margin:20px 0; border:none; border-top:1px solid #ddd;'/>" +
                "<p style='font-size:12px; color:#999; text-align:center;'>Este mensaje fue generado automáticamente. Si no solicitaste la recuperación, por favor ignóralo.</p>" +
                "</div>";
    }
}
