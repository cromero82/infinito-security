package com.infinitosoft.infinitosecurity.controller;

import com.infinitosoft.infinitosecurity.model.Rol;
import com.infinitosoft.infinitosecurity.service.RolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<Rol>> listarRoles() {
        log.info("Petición recibida para listar todos los roles");
        List<Rol> roles = rolService.listarRoles();
        log.info("Se encontraron {} roles", roles.size());
        return ResponseEntity.ok(roles);
    }
}
