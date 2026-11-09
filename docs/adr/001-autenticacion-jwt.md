# ADR-001: Autenticación stateless con JWT

## Estado

Aceptada.

## Contexto

El sistema necesita autenticar administradores y profesores, aplicar permisos por rol y permitir que clientes HTTP consuman la API sin sesiones de servidor.

## Decisión

Se utilizarán access tokens JWT Bearer, con una duración inicial de una hora y sin refresh tokens en el MVP.

- La firma utilizará una clave provista por `JWT_SECRET`.
- Las contraseñas se almacenarán mediante BCrypt.
- El rol se incluirá como claim del token y también se verificará contra el usuario persistido.
- Las sesiones HTTP permanecerán deshabilitadas.
- El login será la única ruta pública de autenticación.

## Alternativas descartadas

- Sesiones HTTP: requieren estado en el servidor y no coinciden con el contrato Bearer.
- Tokens opacos almacenados en base de datos: agregan persistencia y revocación explícita que el MVP todavía no necesita.
- Spring Authorization Server: resulta excesivo para una única aplicación y dos roles.

## Consecuencias

La API es sencilla de consumir y escalar horizontalmente. La revocación inmediata de tokens no estará disponible; deshabilitar un usuario impedirá nuevos logins y el filtro verificará el estado persistido en cada petición autenticada.
