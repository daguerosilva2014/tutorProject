# Plan incremental de desarrollo

## 1. Objetivo del plan

El desarrollo se organizará en entregas funcionales. Cada entrega atravesará las capas necesarias —controller, service, repository, model y pruebas— y producirá un resultado que pueda ejecutarse y revisarse.

El orden prioriza primero las capacidades necesarias para operar la academia y después las mejoras de robustez, auditoría y experiencia de uso.

### Criterio de complejidad

- **Baja:** cambios acotados, pocas reglas y bajo riesgo.
- **Media:** atraviesa varias capas o contiene reglas de negocio relevantes.
- **Alta:** requiere coordinación temporal, concurrencia, seguridad o varias entidades relacionadas.

## 2. Dependencias generales

```text
E0 Base ejecutable
 └── E1 Autenticación y usuarios
      ├── E2 Alumnos
      ├── E3 Materias y clases
      │    └── E4 Participaciones
      │         ├── E5 Paquetes y pagos
      │         │    └── E6 Asistencia y consumo
      │         └── E7 Recuperaciones
      └── E8 Auditoría e historial

E9 Endurecimiento y entrega MVP
```

La auditoría se inicia técnicamente desde las primeras operaciones que modifican datos, pero su consulta completa se entrega en E8.

## 3. Épica E0: Base ejecutable del proyecto

### Objetivo

Disponer de una aplicación Spring Boot mínima que compile, arranque y pueda conectarse a PostgreSQL mediante Docker y Flyway.

### Historia E0-H1

Como desarrollador, quiero ejecutar el proyecto localmente con una configuración reproducible para poder implementar y probar funcionalidades.

### Tareas técnicas

1. Crear el proyecto Maven con Java 21.
2. Incorporar Spring Boot, Web, JPA, Validation, Security, PostgreSQL, Flyway, Lombok y testing.
3. Configurar perfiles `dev` y `test`.
4. Crear Docker Compose para PostgreSQL.
5. Configurar la primera migración Flyway vacía o de infraestructura mínima.
6. Configurar la estructura clásica de paquetes por capa.
7. Verificar compilación y arranque.

### Criterios de aceptación

- `mvn test` finaliza correctamente.
- La aplicación inicia con el perfil de desarrollo.
- PostgreSQL se ejecuta mediante Docker Compose.
- Flyway ejecuta las migraciones sin errores.
- No hay credenciales sensibles versionadas.

### Pruebas

- Test de contexto mínimo de Spring Boot.
- Verificación manual de conexión a PostgreSQL.
- Verificación de ejecución de Flyway desde una base vacía.

### Complejidad

Baja-media.

### Conocimientos a dominar

- Maven y ciclo de vida de una aplicación Java.
- Perfiles de Spring Boot.
- Docker Compose.
- Migraciones de base de datos.
- Estructura clásica por capas del proyecto.

## 4. Épica E1: Autenticación y usuarios

### Objetivo

Permitir iniciar sesión con JWT y diferenciar administrador de profesor.

### Historia E1-H1

Como usuario registrado, quiero iniciar sesión para acceder a las operaciones permitidas por mi rol.

### Historia E1-H2

Como administrador, quiero crear y deshabilitar usuarios para controlar quién utiliza el sistema.

### Tareas técnicas

1. Modelar `Usuario`, roles y estado de habilitación.
2. Crear migración de usuarios.
3. Implementar almacenamiento seguro de contraseñas.
4. Implementar emisión y validación de JWT.
5. Configurar filtro de autenticación y autorización.
6. Crear `POST /api/v1/auth/login` y `GET /api/v1/auth/me`.
7. Crear endpoints administrativos de usuarios.
8. Definir cómo se crea el primer administrador.

### Criterios de aceptación

- Un usuario válido obtiene un JWT.
- Una contraseña incorrecta devuelve `401`.
- Una petición sin token devuelve `401`.
- Un usuario autenticado puede consultar `/auth/me`.
- Un profesor no puede acceder a endpoints reservados al administrador.
- Un usuario deshabilitado no puede iniciar sesión.
- Las contraseñas nunca se almacenan en texto plano.

### Pruebas

- Unitarias para generación y validación de tokens.
- Unitarias para codificación de contraseñas.
- API para login correcto e incorrecto.
- API para ausencia de token y rol insuficiente.
- Integración para persistencia de usuarios.

### Complejidad

Media-alta.

### Conocimientos a dominar

- Spring Security.
- JWT, expiración y claims.
- Hashing de contraseñas.
- Autorización por roles.
- Diferencia entre autenticación y autorización.

## 5. Épica E2: Gestión de alumnos

### Objetivo

Construir la base general de alumnos activos e inactivos, conservando su historial.

### Historia E2-H1

Como administrador o profesor autorizado, quiero registrar y consultar alumnos para poder asignarlos a clases.

### Historia E2-H2

Como usuario autorizado, quiero activar o inactivar un alumno sin eliminar su información histórica.

### Tareas técnicas

1. Modelar `Alumno` y su estado.
2. Crear migración de alumnos.
3. Implementar casos de uso de alta, consulta y modificación.
4. Implementar cambio de estado.
5. Crear DTOs y endpoints REST.
6. Agregar búsqueda por nombre y estado. Diferir la paginación a E9.
7. Impedir eliminación física de alumnos con historial.

### Criterios de aceptación

- Se puede crear un alumno con nombre y número de contacto.
- El número de contacto puede ser opcional según la decisión final de validación.
- Un alumno nuevo queda activo.
- Se puede consultar alumnos activos e inactivos.
- Inactivar no elimina el alumno.
- Un alumno inactivo no puede asignarse a nuevas clases.
- Los errores de validación utilizan el formato común.

### Pruebas

- Unitarias para activación e inactivación.
- API para alta, edición y filtros.
- API para impedir asignaciones posteriores de alumnos inactivos cuando exista la asignación en E4.
- Integración para conservar el registro.

### Complejidad

Baja-media.

### Conocimientos a dominar

- CRUD sin borrado físico.
- DTOs y validación.
- Filtros de consulta y ordenamiento.
- Estados de ciclo de vida.
- Diseño de endpoints `PATCH`.

## 6. Épica E3: Materias y clases

### Objetivo

Permitir registrar materias y programar clases concretas de profesores, sin asociar una materia obligatoria a la clase.

### Historia E3-H1

Como usuario autorizado, quiero administrar materias para poder indicar qué trabaja cada alumno cuando corresponda.

### Historia E3-H2

Como profesor, quiero crear y consultar mis clases con fecha y horario para organizar la actividad de la academia.

### Tareas técnicas

1. Modelar `Materia` y su estado.
2. Modelar `Clase` y sus estados `SCHEDULED`, `FINALIZED` y `CANCELLED`.
3. Crear migraciones de materias y clases.
4. Implementar alta, consulta y modificación de clases.
5. Implementar filtros por fecha, profesor y estado; diferir la paginación a E9.
6. Implementar cancelación explícita.
7. Validar rangos horarios.
8. Aplicar permisos: el profesor administra sus clases; el administrador administra todas.

### Criterios de aceptación

- Se pueden crear materias activas.
- Una clase requiere profesor, fecha y horario válido.
- Una clase no requiere materia.
- Un profesor puede consultar sus propias clases.
- El administrador puede consultar todas las clases.
- Una clase cancelada no puede procesarse como clase normal.
- Una clase finalizada no vuelve a programada.

### Pruebas

- Unitarias para `RangoHorario` y estados de clase.
- API para creación, filtros y cancelación.
- API de autorización entre profesor y administrador.
- Integración para relaciones entre clase y usuario.

### Complejidad

Media.

### Conocimientos a dominar

- Entidades con ciclo de vida.
- Fechas y zonas horarias.
- Filtros dinámicos.
- Autorización contextual.
- Restricciones de base de datos.

## 7. Épica E4: Participación de alumnos

### Objetivo

Permitir asignar alumnos manualmente a cada clase y registrar materias opcionales por alumno.

### Historia E4-H1

Como profesor, quiero asignar alumnos a una clase para definir quién participará.

### Historia E4-H2

Como profesor, quiero indicar cero, una o varias materias por alumno para registrar el contenido trabajado.

### Tareas técnicas

1. Modelar `ParticipacionEnClase`.
2. Crear la migración de participaciones y su relación opcional con materias.
3. Implementar asignación manual.
4. Implementar consulta de participantes de una clase.
5. Implementar modificación de materias.
6. Crear restricción única alumno-clase.
7. Impedir asignaciones a clases canceladas o finalizadas.

### Criterios de aceptación

- Un alumno activo puede asignarse a una clase.
- El mismo alumno no puede asignarse dos veces.
- La participación puede tener cero materias.
- Puede tener una materia.
- Puede tener varias materias.
- El profesor solo puede modificar participaciones de sus clases.
- La asignación queda disponible para registrar asistencia posteriormente.

### Pruebas

- Unitarias para evitar duplicados.
- API para asignación con cero, una y varias materias.
- API para alumno inactivo.
- Integración para la restricción alumno-clase.

### Complejidad

Media.

### Conocimientos a dominar

- Modelado de relaciones.
- Relaciones opcionales y múltiples.
- Restricciones únicas.
- Diferencia entre entidad y relación con atributos propios.

## 8. Épica E5: Paquetes y pagos

### Objetivo

Registrar paquetes de clases, pagos completos o parciales y saldos pendientes.

### Historia E5-H1

Como usuario autorizado, quiero crear un paquete para registrar cuántas clases compró un alumno.

### Historia E5-H2

Como profesor o administrador, quiero registrar pagos parciales para conocer el saldo adeudado.

### Tareas técnicas

1. Modelar `PaqueteDeClases`, `Pago` y `Dinero`.
2. Crear migraciones de paquetes y pagos.
3. Implementar estados de uso y pago independientes.
4. Implementar creación y consulta de paquetes.
5. Implementar registro de pagos.
6. Calcular importe pagado y saldo pendiente.
7. Mantener ARS como divisa fija, validar importes positivos y pagos que no superen el saldo.
8. Permitir paquetes futuros en estado pendiente de uso.

### Criterios de aceptación

- Se puede crear un paquete con cantidad positiva de clases.
- El paquete comienza con cero clases utilizadas.
- Se pueden registrar pagos parciales.
- El estado cambia entre pendiente, parcial y pagado.
- El saldo monetario se calcula correctamente.
- Un pago superior al saldo es rechazado.
- Un alumno puede tener varios paquetes.
- Un paquete futuro puede estar pagado sin comenzar a utilizarse.

### Pruebas

- Unitarias para `Dinero`.
- Unitarias para estados y saldos de paquetes.
- API para creación y consulta.
- API para pagos completos, parciales y inválidos.
- Integración para transacciones de pago.

### Complejidad

Media.

### Conocimientos a dominar

- Valores monetarios y `BigDecimal`.
- Estados independientes.
- Transacciones.
- Validación de invariantes.
- Diseño de recursos anidados.

## 9. Épica E6: Asistencia, consumo y cierre automático

### Objetivo

Registrar asistencia y resolver automáticamente el consumo de clases cuando finaliza el horario de una clase.

### Historia E6-H1

Como profesor, quiero registrar presencia o ausencia para mantener el historial de asistencia.

### Historia E6-H2

Como sistema, quiero cerrar automáticamente las clases vencidas para consumir las clases correspondientes sin intervención manual.

### Tareas técnicas

1. Agregar estados de asistencia y consumo a la participación.
2. Implementar `PUT /class-participations/{id}/attendance`.
3. Implementar selección del paquete en curso más antiguo con saldo.
4. Consumir una clase una sola vez.
5. Crear la tarea programada de cierre automático.
6. Resolver participaciones pendientes al finalizar la clase.
7. Garantizar idempotencia del cierre.
8. Proteger actualizaciones concurrentes del paquete.
9. Registrar auditoría de los consumos.
10. Elegir `PACKAGE` o `PER_CLASS` en cada participación; guardar precio y pagos individuales cuando corresponda.
11. Registrar el aviso de ausencia con la hora del servidor y aplicar el umbral de 12 horas.

### Criterios de aceptación

- Una presencia consume una clase al finalizar la clase.
- Una ausencia sin aviso suficiente consume una clase.
- Repetir el mismo registro no consume otra clase.
- El consumo se aplica al paquete más antiguo elegible.
- Un paquete agotado no recibe consumos.
- Las clases vencidas se cierran automáticamente.
- El cierre automático no duplica consumos.
- Una clase cancelada no consume clases.
- Un paquete se crea para el alumno sin clase asignada. Una participación `PER_CLASS` no requiere paquete y permite deuda y pagos parciales.
- Una ausencia avisada con al menos 12 horas no consume ni genera deuda individual.
- Si falta saldo al cierre de una participación `PACKAGE`, se revierte el cierre completo para poder reintentarlo.

### Pruebas

- Unitarias para todos los estados de asistencia y consumo.
- Unitarias para selección de paquetes.
- Integración para transacciones y concurrencia básica.
- Test de tarea programada con una clase vencida.
- API para presencia, ausencia y estados de error.

### Complejidad

Alta.

### Conocimientos a dominar

- Reglas de negocio con estados.
- Transacciones y bloqueo optimista o equivalente.
- Tareas programadas.
- Idempotencia.
- Consistencia entre varias entidades.

## 10. Épica E7: Recuperaciones

### Objetivo

Permitir recuperar una ausencia justificada en una clase futura: la original no consume y la recuperación descuenta una clase restante o genera un cobro individual al cerrarse.

### Historia E7-H1

Como profesor, quiero registrar una ausencia con aviso de al menos 12 horas para que quede recuperable.

### Historia E7-H2

Como profesor, quiero asignar una recuperación a una clase futura para cerrar el ciclo de la ausencia.

### Tareas técnicas

1. Validar el límite de 12 horas usando fecha y hora de la clase.
2. Identificar las participaciones `JUSTIFIED_ABSENCE` y `NOT_CONSUMED` pendientes de recuperación sin reutilizar el estado de consumo como estado de recuperación.
3. Implementar `POST /class-participations/{id}/recovery`.
4. Crear una participación de destino nueva; rechazar si el alumno ya está asignado a esa clase.
5. Impedir reutilizar una recuperación.
6. Impedir recuperaciones en clases canceladas, finalizadas o no futuras.
7. Aplicar un solo consumo o cobro al cerrar la clase de recuperación.
8. Permitir asignar otra clase si el destino anterior se canceló.
9. Registrar la auditoría transversal en E8.

### Criterios de aceptación

- Avisar exactamente 12 horas antes permite justificar.
- Avisar con menos de 12 horas no permite justificar automáticamente.
- Una ausencia justificada no consume una clase.
- Una recuperación solo puede utilizarse una vez.
- La clase de destino debe ser futura.
- La clase original justificada no consume; la recuperación consume una clase restante del paquete o genera el cobro individual acordado.
- La operación repetida no crea una segunda recuperación.
- Una recuperación con clase destino cancelada puede reprogramarse.

### Pruebas

- Unitarias para el límite exacto de 12 horas.
- Unitarias para transiciones de recuperación.
- API para recuperación exitosa y errores.
- Integración para evitar duplicados.

### Complejidad

Media-alta.

### Conocimientos a dominar

- Fechas, horas y zonas horarias.
- Idempotencia de comandos.
- Relaciones autorreferenciales o referencias entre participaciones.
- Corrección de estados.

## 11. Épica E8: Auditoría e historial

### Objetivo

Conservar quién modificó datos relevantes, cuándo y qué valores cambiaron.

### Historia E8-H1

Como administrador, quiero consultar los cambios realizados para poder revisar el historial del sistema.

### Historia E8-H2

Como usuario autorizado, quiero consultar el historial operativo de un alumno.

### Tareas técnicas

1. Modelar `RegistroDeAuditoria`.
2. Crear migración de auditoría.
3. Definir qué operaciones generan auditoría.
4. Registrar usuario, fecha, entidad, operación y valores anterior/nuevo.
5. Crear consulta de auditoría con filtros y paginación.
6. Crear consulta de historial por alumno.
7. Evitar que la auditoría pueda alterarse desde la API del MVP.

### Criterios de aceptación

- Cambios de pagos, asistencia, paquetes, clases y estados generan auditoría.
- Cada entrada identifica al usuario y la fecha.
- Las correcciones conservan valor anterior y nuevo.
- El administrador puede consultar auditoría filtrada.
- El historial de un alumno reúne clases, asistencias, recuperaciones y pagos.
- No se elimina el historial al inactivar un alumno.

### Pruebas

- Unitarias para creación de entradas.
- Integración para auditoría dentro de la misma transacción.
- API para filtros y permisos.
- Prueba de que una operación fallida no deja una auditoría falsa.

### Complejidad

Media-alta.

### Conocimientos a dominar

- Auditoría de cambios.
- Transacciones atómicas.
- Diseño de consultas históricas.
- Separación entre estado actual e historial.

## 12. Épica E9: Endurecimiento y entrega del MVP

### Objetivo

Integrar las funcionalidades del MVP, corregir inconsistencias y dejar una versión demostrable y reproducible.

### Tareas técnicas

1. Revisar todos los endpoints contra la tabla de trazabilidad.
2. Completar respuestas de error uniformes.
3. Revisar permisos de administrador y profesor.
4. Completar paginación, filtros y ordenamiento del MVP.
5. Ejecutar pruebas de integración con PostgreSQL.
6. Agregar datos de desarrollo reproducibles sin secretos.
7. Configurar Docker Compose de la aplicación y PostgreSQL.
8. Documentar cómo ejecutar y verificar el sistema.
9. Revisar migraciones Flyway desde una base vacía.
10. Actualizar README con el estado real del proyecto.

### Criterios de aceptación

- Todos los flujos principales del MVP funcionan de extremo a extremo.
- La aplicación puede levantarse siguiendo únicamente el README.
- Las pruebas automatizadas pasan.
- No existen endpoints documentados que no estén implementados en el alcance aprobado.
- Las operaciones sensibles requieren JWT y permisos correctos.
- No hay secretos en el repositorio.
- El cierre automático puede demostrarse con una clase vencida.

### Pruebas

- Suite unitaria completa.
- Suite de integración con PostgreSQL.
- Pruebas de API de los flujos principales.
- Prueba manual guiada del MVP.
- Revisión de migraciones desde cero.

### Complejidad

Media.

### Conocimientos a dominar

- Integración de capas.
- Diagnóstico de errores.
- Testing de extremo a extremo.
- Despliegue reproducible.
- Documentación técnica.

## 13. MVP y trabajo posterior

### Incluido en el MVP

- autenticación JWT;
- roles administrador y profesor;
- gestión de usuarios;
- alumnos activos e inactivos;
- materias;
- clases concretas;
- asignación manual de alumnos;
- materias opcionales por participación;
- asistencia y ausencias;
- cierre automático de clases;
- paquetes y saldos;
- pagos completos y parciales;
- recuperaciones;
- auditoría e historial;
- paginación, filtros y errores consistentes.

### Fuera del MVP

- portal de alumnos;
- aplicación móvil;
- notificaciones por WhatsApp o correo;
- pagos online;
- facturación electrónica;
- múltiples sedes;
- capacidad máxima por clase;
- vencimiento automático de paquetes;
- reportes avanzados;
- exportación a Excel o PDF.

Estas funcionalidades deben planificarse como nuevas épicas después de validar el MVP.

## 14. Decisiones técnicas que requieren aprobación

Estas decisiones no deberían quedar implícitas durante la implementación:

1. **Creación del primer administrador:** seed controlado en desarrollo, variable de entorno al iniciar o comando administrativo manual.
2. **JWT:** duración del access token y si el MVP tendrá refresh tokens. Recomendación inicial: access token de corta duración y sin refresh token hasta necesitarlo.
3. **Zona horaria:** usar `America/Argentina/Buenos_Aires` como zona de negocio de la academia.
4. **Frecuencia del cierre automático:** recomendación inicial cada minuto o cada cinco minutos.
5. **Corrección de pagos:** permitir edición controlada y auditada o registrar una reversión y un nuevo pago. Recomendación: corrección auditada sin borrado físico.
6. **Número de contacto:** definir si es opcional o obligatorio.
7. **Pruebas de integración:** PostgreSQL en Docker Compose inicialmente o Testcontainers desde el comienzo.

## 15. Orden recomendado de implementación

```text
E0 Base ejecutable
 → E1 JWT y usuarios
 → E2 Alumnos
 → E3 Materias y clases
 → E4 Participaciones
 → E5 Paquetes y pagos
 → E6 Asistencia, consumo y cierre automático
 → E7 Recuperaciones
 → F1 Frontend inicial (login y consulta de alumnos)
 → E8 Auditoría e historial
 → E9 Endurecimiento y entrega MVP
```

Después de cada épica se debe ejecutar una revisión del código, actualizar la documentación afectada y verificar los criterios de aceptación antes de comenzar la siguiente.

F1 se incorporó después de E7 por decisión del proyecto. Las siguientes pantallas del frontend se desarrollarán en entregas pequeñas a medida que se definan; E8 y E9 continúan pendientes.

## 16. Estado actual

### E0: Base ejecutable del proyecto

Estado: completada.

Se creó el proyecto Maven con Java 21, la aplicación Spring Boot mínima, los perfiles `dev` y `test`, PostgreSQL mediante Docker Compose, la migración inicial de Flyway, el test de contexto y la documentación de ejecución.

Verificaciones realizadas:

- `mvn test`: correcto, 1 test ejecutado.
- `docker compose config --quiet`: correcto.
- PostgreSQL 16 mediante Docker Compose: contenedor saludable.
- Arranque con perfil `dev`: correcto; conexión a PostgreSQL y migración Flyway verificadas.

Las épicas E8 y E9 permanecen pendientes.

### E1: Autenticación y usuarios

Estado: completada.

Se implementaron usuarios persistidos, roles `ADMIN` y `TEACHER`, contraseñas con BCrypt, login JWT Bearer, consulta del usuario autenticado, endpoints administrativos de usuarios, bootstrap opcional del primer administrador y pruebas de autorización.

Verificaciones realizadas:

- `mvn test`: correcto, 11 tests ejecutados.
- Migraciones Flyway 1 y 2 validadas con H2 y PostgreSQL.
- Arranque con perfil `dev`: correcto.
- Bootstrap del administrador inicial verificado en PostgreSQL.
- Casos `401`, `403`, credenciales inválidas y usuario deshabilitado verificados.

### Refactorización arquitectónica posterior a E1

Estado: realizada en el código y la documentación actuales.

Se reemplazó la organización por funcionalidad y la separación hexagonal por paquetes globales por capa. `User` pasó a ser entidad JPA y `UserRepository` pasó a ser la interfaz Spring Data utilizada directamente por el servicio. Se actualizaron el diseño arquitectónico y los requisitos tecnológicos. La decisión está registrada en [ADR-002](adr/002-arquitectura-clasica-por-capas.md).

Verificación: `mvn -Dmaven.repo.local=/tmp/tutorProject-maven-repository test` finalizó correctamente; 11 tests ejecutados, sin fallos ni errores.

### E2: Gestión de alumnos

Estado: implementada para altas, consultas, edición, activación, inactivación y filtros sin paginación.

E4 verifica que un alumno inactivo no pueda asignarse a una clase. La paginación de alumnos queda pendiente para E9.

Verificación: 15 tests correctos con el perfil `test`, incluyendo la migración Flyway V4 en H2, validación de entradas, filtros, autenticación y cambios de estado.

### E3: Materias y clases

Estado: implementada para materias y programación, consulta, edición, filtrado y cancelación de clases.

Las materias admiten estados `ACTIVE` e `INACTIVE`. Cada clase tiene un profesor activo, fecha y horario válido; comienza `SCHEDULED`. Un profesor solo administra sus clases y el administrador puede administrar las de cualquier profesor activo. La edición y la cancelación solo se permiten mientras la clase esté programada. E4 incorpora la asignación de alumnos y materias por participación; el cierre automático y la transición a `FINALIZED` llegarán en E6. La paginación queda pendiente para E9.

Verificación: 24 tests correctos con el perfil `test`, incluidas las migraciones Flyway V5 y V6 en H2, contratos HTTP, filtros, permisos y transiciones de estado. PostgreSQL todavía no se verificó para estas migraciones.

### E4: Participación de alumnos

Estado: implementada para asignación manual, consulta de participantes y reemplazo de materias opcionales.

Cada participación vincula un alumno y una clase. Solo se admiten alumnos activos y clases programadas; una restricción única impide repetir el alumno en una clase. Las materias activas pueden asociarse en cantidad cero o mayor; desactivar una materia no borra su asociación previa. El profesor solo opera en sus clases y el administrador puede hacerlo en todas. La asistencia, el consumo y las recuperaciones quedan para E6 y E7.

Verificación: 30 tests correctos con el perfil `test`, incluida Flyway V7 en H2, asignaciones con cero, una y varias materias, cambio de materias, permisos, alumnos y materias inactivos y restricción única en la base. PostgreSQL todavía no se verificó para V7.

### E5: Paquetes y pagos

Estado: implementada para creación y consulta de paquetes, filtros por estado de uso y pago, registro de pagos parciales o completos y consulta de pagos.

`Money` valida importes con dos decimales; ARS es la divisa fija del sistema y no forma parte de los requests ni responses. El precio del paquete puede ser cero; los pagos deben ser positivos y no superar el saldo pendiente. El paquete mantiene saldos monetarios y de clases independientes; el uso de clases comenzará en E6. Cada pago conserva el usuario que lo registró. Los pagos se agregan junto con la actualización del total abonado en una transacción; el campo `version` impide confirmar dos actualizaciones concurrentes basadas en el mismo saldo. No hay paginación todavía. La corrección auditada de pagos queda pendiente para E8 o una decisión posterior.

Verificación: 39 tests correctos con el perfil `test`, incluidas Flyway V8 y V9 en H2, reglas monetarias, estados, API, pago parcial y completo, rechazo de pagos superiores al saldo y conflicto de versión. PostgreSQL todavía no se verificó para V8 y V9.

### Estado de E6

E6 incorpora asistencia, aviso de ausencia con hora del servidor, cierre automático por clase, consumo del paquete más antiguo con saldo y registro único de cada consumo. También incorpora las dos modalidades de cobro por participación: `PACKAGE` y `PER_CLASS`, con deuda y pagos parciales para la segunda. Un cierre sin saldo se revierte y se reintenta. Las ausencias justificadas no consumen ni generan cobro. Las recuperaciones quedan para E7; por ello el estado `NOT_CONSUMED` todavía no representa una recuperación utilizada. Verificación: 46 tests correctos con el perfil `test`, incluyendo Flyway V10 y V11 en H2, API, umbral de 12 horas, pagos parciales, cierre idempotente y reintento por falta de saldo. PostgreSQL todavía requiere comprobación de V10 y V11. Las participaciones existentes anteriores a E6 se migran con modalidad `PACKAGE` por defecto; si no tienen paquete disponible, deberá registrarse uno antes de que puedan cerrarse.

### Estado de E7

E7 vincula una participación `JUSTIFIED_ABSENCE` y `NOT_CONSUMED` con una nueva participación futura del mismo alumno. La ausencia original no consume ni cobra; la recuperación descuenta una clase restante o genera el cobro individual al cerrar la clase destino. La operación repetida devuelve el mismo vínculo, y una clase destino cancelada permite reprogramar la recuperación. La vinculación y la creación del destino son transaccionales; la restricción única y la versión JPA protegen contra duplicaciones. Verificación: 52 tests correctos con el perfil `test`, incluida la migración V12 en H2, permisos, fechas, duplicados, cancelación, consumo y cobro. PostgreSQL todavía requiere comprobación de V12. La auditoría transversal corresponde a E8.

### Estado de F1: Frontend inicial

Se incorporó `frontend/` con React, TypeScript y Vite. La primera entrega ofrece login JWT y consulta de alumnos con búsqueda y filtro de estado. La decisión se registra en [ADR-004](adr/004-frontend-react.md), y la ejecución y limitaciones en [07-frontend.md](07-frontend.md). Las demás pantallas operativas siguen pendientes.

La siguiente entrega del frontend completa la gestión de alumnos con alta, detalle, edición y cambio de estado. Utiliza los endpoints ya existentes y mantiene pendientes las demás pantallas operativas.

La entrega posterior agrega la gestión de materias con el mismo alcance y una navegación entre ambas secciones. Las pantallas de clases y los demás procesos siguen pendientes.

La siguiente entrega incorpora la pantalla de clases: listado con filtros, programación, detalle, edición y cancelación. El administrador debe seleccionar un profesor activo existente; la creación de usuarios desde el frontend aún está pendiente. Participaciones y procesos posteriores siguen pendientes.
