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
