package com.infinitosoft.infinitosecurity.repository;

import com.infinitosoft.infinitosecurity.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findBySigla(String sigla);
}
