# Contratos de la API REST

## 1. Convenciones generales

La API será REST y utilizará la siguiente base URL:

```text
/api/v1
```

El versionado se justifica porque los contratos podrían ser consumidos posteriormente por una aplicación móvil o un portal de alumnos.

Headers comunes:

```http
Authorization: Bearer <jwt>
Content-Type: application/json
Accept: application/json
```

Las fechas utilizarán ISO-8601, por ejemplo `2026-11-06` y `2026-11-06T18:30:00-03:00`. Los identificadores se expondrán como UUID. La API no expondrá directamente entidades JPA.

## 2. Recursos principales

| Recurso | Concepto de dominio |
|---|---|
| `/auth` | Autenticación y JWT |
| `/users` | Usuario administrador o profesor |
| `/students` | Alumno |
| `/subjects` | Materia |
| `/classes` | Clase concreta |
| `/class-participations` | Participación de un alumno en una clase |
| `/packages` | Paquete de clases |
| `/payments` | Pago asociado a un paquete |
| `/audit-entries` | Registro de auditoría |

## 3. Autenticación y autorización

Se utilizará Spring Security con JWT Bearer. Todas las operaciones requieren autenticación, excepto el login.

### Iniciar sesión

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "username": "profesor1",
  "password": "password-seguro"
}
```

Response `200 OK`:

```json
{
  "accessToken": "jwt-firmado",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "7b3a9f1e-6c1f-4a1b-9e23-1d7c7d9b1c10",
    "name": "Ana Pérez",
    "role": "TEACHER"
  }
}
```

Errores: `400 Bad Request` para campos faltantes y `401 Unauthorized` para credenciales inválidas.

### Usuario autenticado

```http
GET /api/v1/auth/me
```

El administrador puede gestionar todos los recursos. Los profesores pueden administrar sus propias clases, asignar alumnos activos, registrar asistencia y recuperaciones en esas clases, y consultar o modificar pagos de cualquier alumno. La administración de usuarios queda reservada al administrador.

## 4. Usuarios

Acceso recomendado: administrador.

```http
GET    /api/v1/users?role=TEACHER&status=ACTIVE
POST   /api/v1/users
GET    /api/v1/users/{userId}
PATCH  /api/v1/users/{userId}
```

Los filtros `role` y `status` son opcionales. `status` acepta `ACTIVE` o `INACTIVE`. La respuesta es un array JSON de usuarios, ordenado por nombre. Por ahora, el endpoint devuelve todos los usuarios que coinciden con los filtros; no recibe parámetros de paginación.

Ejemplo de respuesta:

```json
[
  {
    "id": "7f211f5a-461a-4f3d-9c23-c2ccac2ad922",
    "name": "Ana Pérez",
    "username": "ana.perez",
    "role": "TEACHER",
    "enabled": true,
    "createdAt": "2026-11-08T14:00:00Z"
  }
]
```

Request de creación:

```json
{
  "name": "Ana Pérez",
  "username": "ana.perez",
  "password": "password-seguro",
  "role": "TEACHER"
}
```

## 5. Alumnos

```http
GET    /api/v1/students
POST   /api/v1/students
GET    /api/v1/students/{studentId}
PATCH  /api/v1/students/{studentId}
PATCH  /api/v1/students/{studentId}/status
```

Filtros implementados: `status` (`ACTIVE` o `INACTIVE`) y `search` (coincidencia parcial del nombre). La lista se ordena por nombre y no está paginada por ahora.

Request:

```json
{
  "name": "Juan Gómez",
  "contactNumber": "1122334455",
  "notes": "Alumno nuevo"
}
```

Response `201 Created`:

```json
{
  "id": "b7f63c7e-9e31-4cb9-88b9-8de9d2eb3a21",
  "name": "Juan Gómez",
  "contactNumber": "1122334455",
  "notes": "Alumno nuevo",
  "status": "ACTIVE",
  "createdAt": "2026-11-06T15:00:00-03:00"
}
```

Cambio de estado:

```json
{
  "status": "INACTIVE"
}
```

No se utilizará `DELETE` para alumnos con historial.

`PATCH /students/{studentId}` requiere `name`. Los campos `contactNumber` y `notes` son opcionales; omitirlos o enviarlos como `null` borra su valor anterior. `PATCH /status` conserva el identificador y los demás datos.

## 6. Materias

```http
GET    /api/v1/subjects?status=ACTIVE&search=mat
POST   /api/v1/subjects
GET    /api/v1/subjects/{subjectId}
PATCH  /api/v1/subjects/{subjectId}
PATCH  /api/v1/subjects/{subjectId}/status
```

Request:

```json
{
  "name": "Matemática",
  "description": "Apoyo escolar"
}
```

Response `201 Created`:

```json
{
  "id": "1d9e52c4-3f6a-4ed2-bab5-9d4e1e7b8e12",
  "name": "Matemática",
  "description": "Apoyo escolar",
  "status": "ACTIVE"
}
```

`GET /subjects` devuelve un array ordenado por nombre. `status` (`ACTIVE` o `INACTIVE`) y `search` (coincidencia parcial del nombre) son filtros opcionales. No hay paginación en E3. `PATCH /subjects/{subjectId}` requiere `name`; omitir `description` o enviarla como `null` borra su valor anterior. El cambio de estado recibe `{"status":"INACTIVE"}` o `{"status":"ACTIVE"}`. Una materia es opcional en una participación y nunca obligatoria en una clase; la asignación a participaciones llegará en E4.

## 7. Clases

```http
GET    /api/v1/classes
POST   /api/v1/classes
GET    /api/v1/classes/{classId}
PATCH  /api/v1/classes/{classId}
POST   /api/v1/classes/{classId}/cancel
```

Filtros opcionales: `from` y `to` (fechas inclusivas, formato `YYYY-MM-DD`), `teacherId` y `status` (`SCHEDULED`, `FINALIZED` o `CANCELLED`). La lista se ordena por fecha, hora de inicio e identificador. No hay paginación en E3. Un profesor solo ve sus clases; el administrador puede ver todas o filtrar por profesor. Un profesor que solicite explícitamente otro `teacherId` recibe `403`.

Request de creación:

```json
{
  "date": "2026-11-10",
  "startTime": "18:00:00",
  "endTime": "20:00:00",
  "teacherId": "7b3a9f1e-6c1f-4a1b-9e23-1d7c7d9b1c10",
  "notes": "Clase de apoyo"
}
```

Response `201 Created`:

```json
{
  "id": "e15c73e7-4555-4f4a-a31c-efc9923f9eb5",
  "date": "2026-11-10",
  "startTime": "18:00:00",
  "endTime": "20:00:00",
  "teacher": {
    "id": "7b3a9f1e-6c1f-4a1b-9e23-1d7c7d9b1c10",
    "name": "Ana Pérez"
  },
  "status": "SCHEDULED",
  "notes": "Clase de apoyo"
}
```

Un administrador debe enviar `teacherId` de un usuario `TEACHER` activo. El profesor puede omitir `teacherId` (se usa su propio usuario) o enviar su identificador; no puede elegir a otro profesor. El horario usa hora local de la academia y debe cumplir `startTime < endTime` dentro de la misma fecha. No se permite un horario que cruce medianoche en E3.

`PATCH /classes/{classId}` requiere `date`, `startTime` y `endTime`; `notes` se puede omitir o enviar como `null` para borrarlo. El profesor de una clase no se cambia mediante ese endpoint. Solo las clases `SCHEDULED` pueden modificarse o cancelarse. `POST /classes/{classId}/cancel` devuelve la clase con estado `CANCELLED`; repetirlo devuelve `409`. Una clase de otro profesor devuelve `403` al consultar, editar o cancelar.

La respuesta de clase no incluye `participantsCount`; el listado de participantes está disponible en `GET /classes/{classId}/participants`. El estado `FINALIZED` ya forma parte del modelo y del filtro, pero ninguna operación actual finaliza clases: el cierre automático llegará en E6.

### Cierre automático

No existirá un endpoint público para finalizar clases. Una tarea programada del backend buscará periódicamente clases `SCHEDULED` cuyo horario haya terminado y las pasará a `FINALIZED`.

La misma transacción resolverá participaciones, consumos y recuperaciones. El proceso será idempotente y no producirá consumos duplicados si se ejecuta nuevamente.

Cancelar una clase continúa siendo explícito:

```http
POST /api/v1/classes/{classId}/cancel
```

## 8. Participaciones y asistencia

### Listar participantes

```http
GET /api/v1/classes/{classId}/participants
```

### Asignar alumno

```http
POST /api/v1/classes/{classId}/participants
```

Request:

```json
{
  "studentId": "b7f63c7e-9e31-4cb9-88b9-8de9d2eb3a21",
  "subjectIds": [
    "1d9e52c4-3f6a-4ed2-bab5-9d4e1e7b8e12"
  ]
}
```

`subjectIds` puede omitirse o ser una lista vacía.

Response `201 Created`:

```json
{
  "id": "2fd16d2a-a24b-41e6-8d5d-0bf123e1770f",
  "classId": "e15c73e7-4555-4f4a-a31c-efc9923f9eb5",
  "student": {
    "id": "b7f63c7e-9e31-4cb9-88b9-8de9d2eb3a21",
    "name": "Juan Gómez"
  },
  "subjects": [
    {
      "id": "1d9e52c4-3f6a-4ed2-bab5-9d4e1e7b8e12",
      "name": "Matemática"
    }
  ]
}
```

`GET /classes/{classId}/participants` devuelve un array ordenado por nombre del alumno. Los profesores solo pueden consultar y modificar participaciones de sus clases; el administrador puede hacerlo en todas. La consulta sigue disponible si la clase se cancela. La asignación requiere una clase `SCHEDULED` y un alumno `ACTIVE`.

Errores: `400` si se repite una materia en la solicitud, `403` si la clase pertenece a otro profesor, `404` si falta la clase, el alumno o una materia, `409` si el alumno ya está asignado o la clase no está programada, y `422` si el alumno está inactivo o se intenta agregar una materia inactiva.

### Modificar materias de una participación

```http
PATCH /api/v1/class-participations/{participationId}
```

Request:

```json
{
  "subjectIds": [
    "1d9e52c4-3f6a-4ed2-bab5-9d4e1e7b8e12",
    "3cbf1d67-4dcb-4f2a-86e5-75e5dfb0d290"
  ]
}
```

En este `PATCH`, `subjectIds` es obligatorio: reemplaza la lista completa. Una lista vacía quita todas las materias. Se puede conservar una materia ya asociada aunque luego se desactive, pero no agregar una materia inactiva por primera vez. La clase debe seguir `SCHEDULED`. Si no existe la participación se devuelve `404`.

La respuesta de E4 no incluye todavía asistencia ni consumo; esos campos llegarán en E6.

### Registrar asistencia o ausencia

```http
PUT /api/v1/class-participations/{participationId}/attendance
```

Presencia:

```json
{
  "status": "PRESENT"
}
```

Ausencia:

```json
{
  "status": "ABSENT",
  "absenceNotifiedAt": "2026-11-10T03:00:00-03:00"
}
```

Response de ausencia justificada:

```json
{
  "id": "2fd16d2a-a24b-41e6-8d5d-0bf123e1770f",
  "attendanceStatus": "JUSTIFIED_ABSENCE",
  "consumptionStatus": "PENDING_RECOVERY"
}
```

La operación es idempotente para el mismo estado. Repetirla no debe descontar otra clase.

### Asignar recuperación

```http
POST /api/v1/class-participations/{participationId}/recovery
```

Request:

```json
{
  "targetClassId": "a0127b3d-2fc3-4627-a2bf-66069d46b91f"
}
```

Response `201 Created`:

```json
{
  "originalParticipationId": "2fd16d2a-a24b-41e6-8d5d-0bf123e1770f",
  "targetParticipationId": "8f9d2ae0-a26f-4ff3-a375-d0bdb6bc5e4a",
  "status": "RECOVERED"
}
```

Errores relevantes: `409` si la ausencia no es justificable, la recuperación ya fue utilizada, la clase destino no es futura o el alumno ya está asignado.

## 9. Paquetes

```http
GET  /api/v1/students/{studentId}/packages
POST /api/v1/students/{studentId}/packages
GET  /api/v1/packages/{packageId}
```

Filtros: `usageStatus`, `paymentStatus`, `page`, `size` y `sort`.

Request:

```json
{
  "totalClasses": 8,
  "price": {
    "amount": 32000.00,
    "currency": "ARS"
  },
  "purchasedAt": "2026-11-06"
}
```

Response:

```json
{
  "id": "f2f9f58d-9b59-4ec9-80df-c22d37c1f8e4",
  "studentId": "b7f63c7e-9e31-4cb9-88b9-8de9d2eb3a21",
  "totalClasses": 8,
  "usedClasses": 4,
  "remainingClasses": 4,
  "price": {
    "amount": 32000.00,
    "currency": "ARS"
  },
  "paidAmount": 20000.00,
  "outstandingAmount": 12000.00,
  "usageStatus": "IN_USE",
  "paymentStatus": "PARTIAL"
}
```

## 10. Pagos

```http
GET  /api/v1/packages/{packageId}/payments
POST /api/v1/packages/{packageId}/payments
```

Request:

```json
{
  "amount": {
    "amount": 20000.00,
    "currency": "ARS"
  },
  "paidAt": "2026-11-06",
  "method": "CASH",
  "notes": "Pago inicial"
}
```

Response `201 Created`:

```json
{
  "id": "77d82d38-77b9-4ea9-9016-5b2c7c1e8d11",
  "packageId": "f2f9f58d-9b59-4ec9-80df-c22d37c1f8e4",
  "amount": {
    "amount": 20000.00,
    "currency": "ARS"
  },
  "paidAt": "2026-11-06",
  "method": "CASH",
  "paymentStatus": "PARTIAL",
  "outstandingAmount": 12000.00
}
```

Errores: `400` por importe inválido, `404` por paquete inexistente y `409` si el importe supera el saldo pendiente.

## 11. Historial y auditoría

Historial operativo:

```http
GET /api/v1/students/{studentId}/history
```

Filtros: `from`, `to`, `type`, `page`, `size` y `sort`.

Auditoría administrativa, recomendada para administrador:

```http
GET /api/v1/audit-entries
```

Filtros: `entityType`, `entityId`, `userId`, `from`, `to`, `page`, `size`.

## 12. Formato de errores

Todas las respuestas de error utilizarán el mismo formato:

```json
{
  "timestamp": "2026-11-06T16:30:00-03:00",
  "status": 409,
  "code": "PACKAGE_WITHOUT_BALANCE",
  "message": "El paquete no tiene clases disponibles."
}
```

Error de validación:

```json
{
  "timestamp": "2026-11-06T16:30:00-03:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "name: must not be blank"
}
```

| Situación | Código HTTP |
|---|---:|
| Datos inválidos | 400 |
| JWT ausente o inválido | 401 |
| Rol insuficiente | 403 |
| Recurso inexistente | 404 |
| Regla de negocio incumplida | 409 o 422 |
| Error inesperado | 500 |

## 13. Validaciones

- Alumno: nombre obligatorio; número de contacto opcional.
- Materia: nombre obligatorio.
- Clase: profesor, fecha, inicio y finalización obligatorios.
- Clase: la finalización debe ser posterior al inicio.
- Participación: alumno obligatorio y único por clase.
- Participación: materias opcionales; deben existir y estar activas.
- Paquete: cantidad mayor que cero; moneda `ARS`; precio no negativo.
- Pago: importe mayor que cero y no superior al saldo pendiente.
- Recuperación: ausencia justificada, clase destino futura y alumno no duplicado.

## 14. Paginación, filtrado y ordenamiento

La paginación se agregará a las colecciones cuando el volumen lo requiera. El formato previsto será:

```text
?page=0&size=20&sort=name,asc
```

Las listas implementadas de usuarios, alumnos, materias y clases actualmente no están paginadas y devuelven todos los resultados filtrados. Su paginación queda pendiente para E9. Cuando se incorpore, el backend deberá limitar el tamaño máximo de página, por ejemplo a `100`.

Filtros principales:

- alumnos: estado y nombre;
- materias: estado y nombre;
- clases: rango de fechas, profesor y estado;
- paquetes: estado de uso y pago;
- auditoría: entidad, usuario y rango temporal.

## 15. Idempotencia y concurrencia

Son idempotentes las operaciones `PUT /class-participations/{id}/attendance`, `PATCH /students/{id}/status` y `PATCH /subjects/{id}/status`. Repetir el mismo estado no debe generar un consumo adicional.

Las operaciones de creación, especialmente pagos y recuperaciones, pueden aceptar:

```http
Idempotency-Key: 7dcb0bbf-ccf5-44e1-a7c2-6de53cf06e3a
```

Las transacciones y restricciones de PostgreSQL deben verificar que el paquete tenga saldo, la participación no esté resuelta, la recuperación no haya sido utilizada, el pago no supere el saldo pendiente y el alumno no esté duplicado.

## 16. Trazabilidad de requisitos

| Requisito | Endpoints o proceso |
|---|---|
| RF-01 Gestionar alumnos | `GET/POST /students`, `GET/PATCH /students/{id}` |
| RF-02 Estados de alumnos | `PATCH /students/{id}/status` |
| RF-03 Reactivar alumnos | `PATCH /students/{id}/status` |
| RF-04 Gestionar usuarios | `GET/POST /users`, `PATCH /users/{id}` |
| RF-05 Gestionar materias | `GET/POST /subjects`, `PATCH /subjects/{id}` |
| RF-06 Crear clases | `GET/POST /classes`, `GET/PATCH /classes/{id}` |
| RF-07 Asignar alumnos | `GET/POST /classes/{id}/participants`, `PATCH /class-participations/{id}` |
| RF-08 Registrar asistencia | `PUT /class-participations/{id}/attendance` |
| RF-08.1 Cierre automático | tarea programada interna, sin endpoint público |
| RF-09 Gestionar recuperaciones | `PUT /class-participations/{id}/attendance` |
| RF-10 Usar recuperación | `POST /class-participations/{id}/recovery` |
| RF-11 Gestionar paquetes | `GET/POST /students/{id}/packages`, `GET /packages/{id}` |
| RF-12 Registrar pagos | `GET/POST /packages/{id}/payments` |
| RF-13 Consultar saldos | `GET /packages/{id}`, `GET /students/{id}/packages` |
| RF-14 Consultar historial | `GET /students/{id}/history` |
| RF-15 Auditar cambios | `GET /audit-entries` |
