
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
