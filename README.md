# Academia Backend

Backend para la gestión operativa de academias pequeñas.

## Requisitos

- Java 21
- Maven 3.9 o superior
- Docker y Docker Compose

## Ejecutar las pruebas

```bash
mvn test
```

Las pruebas utilizan una base H2 en memoria y el perfil `test`.

## Ejecutar PostgreSQL

Copiar las variables de ejemplo si se necesitan valores personalizados:

```bash
cp .env.example .env
```

Iniciar PostgreSQL:

```bash
docker compose up -d postgres
```

La base queda disponible en `localhost:5432`. Las migraciones Flyway se ejecutan automáticamente cuando arranca la aplicación.

## Ejecutar la aplicación

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La aplicación usa arquitectura clásica por capas: `controller`, `service`, `repository`, `model`, `dto`, `security`, `config` y `exception`. E1 implementa autenticación JWT y administración de usuarios; E2 agrega gestión de alumnos activos e inactivos; E3 agrega materias y programación, consulta, edición y cancelación de clases; E4 permite asignar alumnos a clases con materias opcionales por participación. El cierre automático y la asistencia todavía están pendientes. El diseño se describe en [docs/04-arquitectura.md](docs/04-arquitectura.md) y los endpoints en [docs/05-api.md](docs/05-api.md).

## Configuración

Los valores locales pueden definirse mediante variables de entorno:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `INITIAL_ADMIN_USERNAME`
- `INITIAL_ADMIN_PASSWORD`

No se deben versionar archivos `.env` ni credenciales reales.

## Autenticación inicial

La aplicación requiere `JWT_SECRET` con al menos 32 caracteres cuando se ejecuta con el perfil `dev`.

Para crear el primer administrador de forma automática, definir también:

- `INITIAL_ADMIN_USERNAME`
- `INITIAL_ADMIN_PASSWORD`

El administrador inicial solo se crea si todavía no existe ningún administrador. Luego se puede iniciar sesión en:

```http
POST /api/v1/auth/login
```

Las rutas protegidas utilizan:

```http
Authorization: Bearer <token>
```
