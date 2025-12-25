package com.infinitosoft.infinitosecurity.service;

import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    private static final Logger logger = LogManager.getLogger(RolService.class);
    private final RolRepository rolRepository;

    public List<Rol> listarRoles() {
        logger.info("Iniciando servicio listarRoles");
        return rolRepository.findAll();
    }
}
