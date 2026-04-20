package com.infinitosoft.infinitosecurity.service;

import com.infinitosoft.infinitosecurity.dto.BackupDto;
import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.model.UsuarioPerfil;
import com.infinitosoft.infinitosecurity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;

    @Transactional(readOnly = true)
    public BackupDto obtenerBackupCompleto() {
        return BackupDto.builder()
                .roles(rolRepository.findAll())
                .usuarios(usuarioRepository.findAll())
                .usuarioPerfiles(usuarioPerfilRepository.findAll())
                .build();
    }

    @Transactional
    public Map<String, Integer> restaurarBackup(BackupDto backup) {
        int rolesCreados = 0, rolesActualizados = 0;
        int usuariosCreados = 0, usuariosActualizados = 0;
        int perfilesCreados = 0, perfilesActualizados = 0;

        // Mapa: id original del backup -> Rol guardado en BD (para luego mapear roles de usuarios)
        Map<Integer, Rol> rolIdMap = new HashMap<>();

        if (backup.getRoles() != null) {
            for (Rol rolBackup : backup.getRoles()) {
                Optional<Rol> existente = rolRepository.findBySigla(rolBackup.getSigla());
                Rol rol;
                if (existente.isEmpty()) {
                    rol = new Rol();
                    rol.setSigla(rolBackup.getSigla());
                    rol.setNombre(rolBackup.getNombre());
                    rol = rolRepository.save(rol);
                    rolesCreados++;
                } else {
                    rol = existente.get();
                    if (!Objects.equals(rol.getNombre(), rolBackup.getNombre())) {
                        rol.setNombre(rolBackup.getNombre());
                        rol = rolRepository.save(rol);
                        rolesActualizados++;
                    }
                }
                rolIdMap.put(rolBackup.getId(), rol);
            }
        }

        if (backup.getUsuarios() != null) {
            for (Usuario usuarioBackup : backup.getUsuarios()) {
                Optional<Usuario> existente = usuarioRepository.findByCorreoElectronico(usuarioBackup.getCorreoElectronico());
                Usuario usuario;
                boolean esNuevo = existente.isEmpty();
                if (esNuevo) {
                    usuario = new Usuario();
                    usuario.setCorreoElectronico(usuarioBackup.getCorreoElectronico());
                    // Conserva la contraseña original del backup solo al crear
                    usuario.setContrasena(usuarioBackup.getContrasena());
                } else {
                    usuario = existente.get();
                }

                boolean modificado = esNuevo;
                if (!Objects.equals(usuario.getNombre(), usuarioBackup.getNombre())) {
                    usuario.setNombre(usuarioBackup.getNombre());
                    modificado = true;
                }
                if (!Objects.equals(usuario.getTelefono(), usuarioBackup.getTelefono())) {
                    usuario.setTelefono(usuarioBackup.getTelefono());
                    modificado = true;
                }
                if (!Objects.equals(usuario.getActivo(), usuarioBackup.getActivo())) {
                    usuario.setActivo(usuarioBackup.getActivo());
                    modificado = true;
                }

                // Mapear roles por sigla usando el mapa construido anteriormente
                Set<Rol> roles = new HashSet<>();
                if (usuarioBackup.getRoles() != null) {
                    for (Rol rBackup : usuarioBackup.getRoles()) {
                        Rol rolMapeado = rolIdMap.get(rBackup.getId());
                        if (rolMapeado == null) {
                            rolMapeado = rolRepository.findBySigla(rBackup.getSigla()).orElse(null);
                        }
                        if (rolMapeado != null) roles.add(rolMapeado);
                    }
                }
                if (!roles.equals(usuario.getRoles())) {
                    usuario.setRoles(roles);
                    modificado = true;
                }

                if (modificado) {
                    usuarioRepository.save(usuario);
                    if (esNuevo) usuariosCreados++;
                    else usuariosActualizados++;
                }
            }
        }

        if (backup.getUsuarioPerfiles() != null) {
            for (UsuarioPerfil perfilBackup : backup.getUsuarioPerfiles()) {
                if (perfilBackup.getUsuario() == null) continue;
                String correo = perfilBackup.getUsuario().getCorreoElectronico();
                Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoElectronico(correo);
                if (usuarioOpt.isEmpty()) continue;

                Usuario usuario = usuarioOpt.get();
                Optional<UsuarioPerfil> existente = usuarioPerfilRepository.findByUsuario(usuario);
                UsuarioPerfil perfil;
                boolean esNuevo = existente.isEmpty();
                if (esNuevo) {
                    perfil = new UsuarioPerfil();
                    perfil.setUsuario(usuario);
                    perfil.setPersonalizacion(perfilBackup.getPersonalizacion());
                    usuarioPerfilRepository.save(perfil);
                    perfilesCreados++;
                } else {
                    perfil = existente.get();
                    if (!Objects.equals(perfil.getPersonalizacion(), perfilBackup.getPersonalizacion())) {
                        perfil.setPersonalizacion(perfilBackup.getPersonalizacion());
                        usuarioPerfilRepository.save(perfil);
                        perfilesActualizados++;
                    }
                }
            }
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("rolesCreados", rolesCreados);
        resultado.put("rolesActualizados", rolesActualizados);
        resultado.put("usuariosCreados", usuariosCreados);
        resultado.put("usuariosActualizados", usuariosActualizados);
        resultado.put("perfilesCreados", perfilesCreados);
        resultado.put("perfilesActualizados", perfilesActualizados);
        return resultado;
    }
}
