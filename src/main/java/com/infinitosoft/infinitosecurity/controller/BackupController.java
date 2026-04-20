package com.infinitosoft.infinitosecurity.controller;

import com.infinitosoft.infinitosecurity.dto.BackupDto;
import com.infinitosoft.infinitosecurity.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backup")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    public ResponseEntity<BackupDto> obtenerBackup() {
        log.info("Petición recibida para obtener backup completo");
        BackupDto backup = backupService.obtenerBackupCompleto();
        log.info("Backup generado correctamente");
        return ResponseEntity.ok(backup);
    }

    @PostMapping("/restaurar")
    public ResponseEntity<Map<String, Integer>> restaurarBackup(@RequestBody BackupDto backup) {
        log.info("Petición recibida para restaurar backup");
        Map<String, Integer> resultado = backupService.restaurarBackup(backup);
        log.info("Backup restaurado: {}", resultado);
        return ResponseEntity.ok(resultado);
    }
}
