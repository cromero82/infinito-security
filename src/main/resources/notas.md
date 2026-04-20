
# Scripts cURL para Postman

## Restaurar contraseña (Auth Service)

Importa este request en Postman para invocar la restauración de contraseña. Reemplaza el correo por el del usuario objetivo.

```
curl --location --request POST 'http://localhost:8081/auth/restaurar-contrasena?correoElectronico=usuario@example.com'
```

Respuesta esperada:
- 204 No Content cuando el correo se envía correctamente y la contraseña queda actualizada.
- 404 si el usuario no existe.
- 502 si falló el servicio de correo.

Notas:
- Este endpoint es público (no requiere token) según la configuración de seguridad actual.

## Actualizar usuario (Auth Service)

Permite actualizar nombre, correo, teléfono o contraseña de un usuario existente. El usuario se identifica mediante el token JWT enviado en el encabezado `Authorization`.

```
curl --location --request PUT 'http://localhost:8081/auth/actualizar-usuario' \
--header 'Authorization: Bearer <TU_TOKEN_JWT>' \
--header 'Content-Type: application/json' \
--data-raw '{
    "nombre": "Nuevo Nombre",
    "telefono": "1234567890",
    "contrasena": "NuevaPass123*",
    "correoElectronico": "nuevo.correo@example.com"
}'
```

Respuesta esperada:
- 200 OK con el ID del usuario actualizado.
- 400 Bad Request si no se envía ningún campo para actualizar o el token es inválido.
- 404 Not Found si el usuario asociado al token no existe.
- 409 Conflict si el nuevo correo electrónico ya está en uso por otro usuario.

## Actualizar roles de usuario (Auth Service)

Permite actualizar los roles de un usuario identificado por su correo electrónico.

```
curl --location --request PUT 'http://localhost:8081/auth/actualizar-roles-usuarios' \
--header 'Content-Type: application/json' \
--data-raw '{
    "correoElectronico": "usuario1@example.com",
    "roles": ["admin", "vendedor"]
}'
```

Respuesta esperada:
- 204 No Content si la actualización fue exitosa.
- 400 Bad Request si el usuario o algún rol no existe.
- 500 Internal Server Error en caso de error inesperado.

## Logout (Auth Service)

Finaliza la sesión del usuario. Actualmente, al ser un sistema basado en JWT sin estado y sin lista negra, este endpoint es informativo o para compatibilidad futura.

```
curl --location --request POST 'http://localhost:8081/auth/logout' \
--header 'Content-Type: application/json' \
--data-raw '{
    "token": "TU_TOKEN_JWT"
}'
```

Respuesta esperada:
- 204 No Content.

## Listar usuarios (Auth Service)

Obtiene la lista completa de usuarios registrados en el sistema.

```
curl --location --request GET 'http://localhost:8081/auth/usuarios'
```

Respuesta esperada:
- 200 OK con un arreglo JSON conteniendo los usuarios.

Notas:
- Este endpoint es público según la configuración actual (`/auth/**` permitido).

## Listar usuarios no administradores (Auth Service)

Obtiene la lista de usuarios que no tienen el rol de administrador.

```
curl --location --request GET 'http://localhost:8081/auth/usuarios-no-admin'
```

Respuesta esperada:
- 200 OK con un arreglo JSON conteniendo los usuarios filtrados.

## Listar roles (Rol Service)

Obtiene la lista completa de roles disponibles en el sistema.

```
curl --location --request GET 'http://localhost:8081/roles'
```

Respuesta esperada:
- 200 OK con un arreglo JSON conteniendo los roles (id, nombre, sigla).

Notas:
- Este endpoint es público según la configuración actual (`/roles/**` permitido).
