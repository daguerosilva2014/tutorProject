# Academia Backend

Backend para la gestión operativa de academias pequeñas.

## Requisitos

- Java 21
- Maven 3.9 o superior
- Docker y Docker Compose
- Node.js 22.12 o superior (versión LTS) y npm para el frontend
- OpenSSL para generar las credenciales locales de desarrollo

## Ejecutar las pruebas

```bash
mvn test
```

Las pruebas utilizan una base H2 en memoria y el perfil `test`.

## Ejecutar PostgreSQL y el backend para desarrollo

Iniciar PostgreSQL:

```bash
docker compose up -d postgres
```

Después, iniciar el backend:

```bash
./scripts/dev-backend.sh
```

En la primera ejecución, el script crea `.env` con una clave JWT y una contraseña inicial aleatorias, muestra la contraseña en la terminal y arranca Spring Boot. `.env` está ignorado por Git y el script no lo modifica si ya existe. La aplicación usa una base separada, `academia_pruebas`, que el script crea si falta; no cambia los datos de la base `academia`. El usuario inicial es `admin` y la contraseña se puede consultar localmente en `.env`. Si `academia_pruebas` ya tiene un administrador, el script no cambia su contraseña: se deben usar las credenciales existentes. PostgreSQL queda disponible en `localhost:5432`; Flyway ejecuta las migraciones al arrancar la aplicación.

La aplicación usa arquitectura clásica por capas: `controller`, `service`, `repository`, `model`, `dto`, `security`, `config` y `exception`. E1 implementa autenticación JWT y usuarios; E2, alumnos; E3, materias y clases; E4, participaciones; E5, paquetes y pagos parciales; E6, asistencia, cierre automático y cobro por clase; E7, recuperaciones. Todos los importes se interpretan en ARS y la API no acepta una divisa configurable. La auditoría y el historial completo quedan pendientes para E8. El diseño se describe en [docs/04-arquitectura.md](docs/04-arquitectura.md) y los endpoints en [docs/05-api.md](docs/05-api.md).

## Frontend

El frontend usa React, TypeScript y Vite. Permite iniciar sesión, administrar usuarios, alumnos y materias, y operar clases con participantes, asistencia, paquetes, pagos y recuperaciones. Para ejecutarlo, primero iniciá PostgreSQL y el backend con el perfil `dev`; después, en otra terminal:

```bash
cd frontend
npm ci
npm run dev
```

Abrí la dirección que muestra Vite (habitualmente `http://localhost:5173`). En desarrollo, Vite envía las solicitudes `/api` al backend en `http://localhost:8080`. El token JWT se conserva solo en memoria: al recargar la página hay que volver a iniciar sesión. Historial y auditoría siguen pendientes en el backend. Más detalles en [docs/07-frontend.md](docs/07-frontend.md).

Para verificar el frontend: `npm test`, `npm run lint` y `npm run build` desde `frontend/`.

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

La aplicación requiere `JWT_SECRET` con al menos 32 caracteres cuando se ejecuta con el perfil `dev`. `scripts/setup-dev-env.sh` genera un valor local para probarla; más adelante se pueden configurar valores propios en `.env`.

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
