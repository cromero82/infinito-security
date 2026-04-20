package com.infinitosoft.infinitosecurity.repository;

import com.infinitosoft.infinitosecurity.model.Usuario;
import com.infinitosoft.infinitosecurity.model.UsuarioPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioPerfilRepository extends JpaRepository<UsuarioPerfil, Integer> {
    Optional<UsuarioPerfil> findByUsuario(Usuario usuario);
}
