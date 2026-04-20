# Documentación de Endpoints de Seguridad

Este archivo contiene la descripción de los endpoints disponibles en el servicio de seguridad.

## Autenticación y Usuarios

### Obtener usuario por ID
Recupera la información completa de un usuario utilizando su identificador único (UUID).

**Endpoint:** `GET /auth/usuario/{id}`

**Parámetros:**
- `id` (path): El UUID del usuario.

**Ejemplo de solicitud:**
```bash
curl --location --request GET 'http://localhost:8081/auth/usuario/550e8400-e29b-41d4-a716-446655440000'
```

**Respuesta esperada:**
- `200 OK`: Devuelve el objeto `UserInfo` en formato JSON.
- `404 Not Found`: Si el usuario con el ID proporcionado no existe.

---

### Registro de usuario
Registra un nuevo usuario en el sistema.

**Endpoint:** `POST /auth/registro`

### Inicio de sesión
Autentica a un usuario y devuelve un token JWT.

**Endpoint:** `POST /auth/login`

### Validar Token
Verifica si un token JWT es válido.

**Endpoint:** `POST /auth/validate`

### Verificar Expiración de Token
Verifica si un token JWT ha expirado.

**Endpoint:** `POST /auth/is-expired`

### Cerrar Sesión
Invalida la sesión actual.

**Endpoint:** `POST /auth/logout`

### Obtener Claims (Información del Usuario)
Extrae la información del usuario contenida en el token JWT.

**Endpoint:** `POST /auth/claims`

### Obtener ID por Correo
Recupera el UUID de un usuario a partir de su correo electrónico.

**Endpoint:** `GET /auth/usuario-id?correoElectronico={correo}`

---

## Backup

### Obtener Backup Completo
Recupera un respaldo completo de los datos de seguridad, incluyendo roles, usuarios y perfiles de usuario.

**Endpoint:** `GET /backup`

**Ejemplo de solicitud:**
```bash
curl --location --request GET 'http://localhost:8081/backup'
```

**Respuesta esperada:**
- `200 OK`: Devuelve un objeto `BackupDto` en formato JSON que contiene:
    - `roles`: Lista de todos los roles registrados.
    - `usuarios`: Lista de todos los usuarios registrados.
    - `usuarioPerfiles`: Lista de todos los perfiles de usuario asociados.

---

### Restaurar Backup
Recibe un objeto `BackupDto` (con el mismo formato que retorna `GET /backup`) y repuebla la base de datos aplicando lógica **upsert**:

- **Roles:** se identifica por `sigla` (campo único). Si no existe se crea; si existe, actualiza `nombre` solo si es diferente.
- **Usuarios:** se identifica por `correoElectronico`. Si no existe se crea (conservando la contraseña del backup); si existe, actualiza `nombre`, `telefono`, `activo` y `roles` solo si difieren.
- **Perfiles de usuario:** se identifica por el `correoElectronico` del usuario asociado. Si no existe se crea; si existe, actualiza `personalizacion` solo si es diferente.

**Endpoint:** `POST /backup/restaurar`

**Ejemplo de solicitud:**
```bash
curl --location --request POST 'http://localhost:8081/backup/restaurar' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "roles": [
      { "id": 1, "nombre": "Administrador", "sigla": "admin" },
      { "id": 2, "nombre": "Usuario", "sigla": "user" }
    ],
    "usuarios": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "nombre": "Carlos Romero",
        "correoElectronico": "carlos@ejemplo.com",
        "contrasena": "$2a$10$hashedpassword...",
        "telefono": "123456789",
        "activo": true,
        "roles": [{ "id": 1, "nombre": "Administrador", "sigla": "admin" }]
      }
    ],
    "usuarioPerfiles": [
      {
        "id": 1,
        "usuario": { "correoElectronico": "carlos@ejemplo.com" },
        "personalizacion": "{\"tema\":\"oscuro\"}"
      }
    ]
  }'
```

**Respuesta esperada:**
- `200 OK`: Devuelve un resumen con los contadores de registros creados y actualizados por entidad:

```json
{
  "rolesCreados": 1,
  "rolesActualizados": 0,
  "usuariosCreados": 1,
  "usuariosActualizados": 0,
  "perfilesCreados": 1,
  "perfilesActualizados": 0
}
```
