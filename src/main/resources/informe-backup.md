# Informe: Implementación BackupController

**Fecha:** 2026-04-16  
**Rama:** feature/development

---

## 1. Objetivo

Implementar un endpoint `GET /backup` que retorne una copia completa de todas las entidades del esquema `security` en formato JSON, para uso de auditoría o respaldo de datos.

---

## 2. Estado de entidades JPA antes de la implementación

| Tabla BD            | Entidad existente | Campos mapeados | Estado         |
|---------------------|-------------------|-----------------|----------------|
| `roles`             | `Rol`             | id, nombre, sigla | Completo      |
| `usuario`           | `Usuario`         | id, nombre, correo_electronico, contrasena, telefono, activo | Completo |
| `usuario_rol`       | (join table)      | via `@ManyToMany` en `Usuario` | Completo |
| `usuario_roles`     | (join table)      | — | No mapeada (ver nota) |
| `usuario_perfil`    | —                 | — | **Faltaba** |
| `proveedorback`     | —                 | — | **Faltaba** |
| `tipo_egresoback`   | —                 | — | **Faltaba** |
| (Token)             | `Token`           | — | Deprecada, sin campos JPA |

---

## 3. Archivos creados

### Entidades JPA nuevas

| Archivo | Tabla | Campos mapeados |
|---------|-------|-----------------|
| `model/TipoEgresoBack.java` | `tipo_egresoback` | id (int), nombre (varchar 100), descripcion (text) |
| `model/ProveedorBack.java` | `proveedorback` | id (int), documento (varchar 50), nombre (varchar 150), telefono (varchar 30), correo (varchar 100), tipoEgreso (FK → TipoEgresoBack) |
| `model/UsuarioPerfil.java` | `usuario_perfil` | id (int), usuario (FK → Usuario), personalizacion (jsonb) |

### Repositorios nuevos

| Archivo | Entidad |
|---------|---------|
| `repository/TipoEgresoBackRepository.java` | TipoEgresoBack |
| `repository/ProveedorBackRepository.java` | ProveedorBack |
| `repository/UsuarioPerfilRepository.java` | UsuarioPerfil |

### Servicio y controlador

| Archivo | Descripción |
|---------|-------------|
| `dto/BackupDto.java` | DTO que agrupa todas las entidades del backup |
| `service/BackupService.java` | Lógica de negocio: consulta todas las entidades con `findAll()` |
| `controller/BackupController.java` | Expone `GET /backup` |

### Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `security/SecurityConfig.java` | Se agregó `/backup/**` a la lista de rutas permitidas sin autenticación |
| `pom.xml` | Se agregó dependencia `hibernate-types-55:2.21.1` para soporte de columna `jsonb` |

---

## 4. Endpoint expuesto

```
GET http://localhost:8081/backup
```

**Respuesta (200 OK):**
```json
{
  "roles": [ ... ],
  "usuarios": [ ... ],
  "usuarioPerfiles": [ ... ],
  "proveedores": [ ... ],
  "tiposEgreso": [ ... ]
}
```

---

## 5. Notas y observaciones

### Tabla `usuario_roles` (duplicada)
El esquema contiene dos tablas con estructura idéntica:
- `usuario_rol` — mapeada en `Usuario` vía `@ManyToMany @JoinTable`
- `usuario_roles` — **no mapeada**, aparentemente duplicada o legada

Se recomienda verificar si `usuario_roles` está en uso y, de no estarlo, eliminarla del esquema para evitar confusión.

### Campo `personalizacion` (jsonb)
El campo `personalizacion` de `usuario_perfil` es de tipo `jsonb` en PostgreSQL. Para su correcto mapeo en Hibernate 5.x se utilizó la librería `com.vladmihalcea:hibernate-types-55:2.21.1` con la anotación `@Type(type = "jsonb")`. El valor se almacena y recupera como `String` JSON.

### Campo `contrasena` en backup
El campo `contrasena` de `Usuario` está incluido en la respuesta del backup. Se recomienda considerar si se desea ocultarlo en la respuesta mediante un DTO dedicado por seguridad.

### Dependencia `hibernate-types-55`
Compatible con Hibernate 5.5/5.6 (usado en Spring Boot 2.7.x). Si en el futuro se migra a Spring Boot 3.x (Hibernate 6), se deberá reemplazar por `hypersistence-utils-hibernate-63`.
