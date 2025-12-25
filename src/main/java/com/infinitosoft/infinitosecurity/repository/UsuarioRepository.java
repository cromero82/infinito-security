package com.infinitosoft.infinitosecurity.repository;

import com.infinitosoft.infinitosecurity.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByCorreoElectronico(String correoElectronico);
    boolean existsByCorreoElectronico(String correoElectronico);

    @Query("SELECT u FROM Usuario u WHERE NOT EXISTS (SELECT r FROM u.roles r WHERE r.sigla = 'admin')")
    List<Usuario> findAllByRolSiglaNotAdmin();
}
