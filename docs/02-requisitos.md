# Requisitos del sistema

## Actores

### Administrador

Puede administrar usuarios, alumnos, materias, clases, pagos, paquetes, estados e historial.

### Profesor

Puede administrar sus clases, asignar alumnos, registrar asistencia, gestionar recuperaciones y consultar o modificar pagos relacionados con sus alumnos.

### Alumno

No interactúa directamente con el sistema durante el MVP.

## Requisitos funcionales

### RF-01: Gestionar alumnos

El sistema debe permitir registrar, consultar y editar alumnos.

### RF-02: Gestionar estados de alumnos

El sistema debe permitir marcar alumnos como activos o inactivos sin eliminar su historial.

### RF-03: Reactivar alumnos

El sistema debe permitir reactivar alumnos inactivos.

### RF-04: Gestionar usuarios

El sistema debe permitir administrar usuarios con rol administrador o profesor.

### RF-05: Gestionar materias

El sistema debe permitir registrar, consultar y activar o desactivar materias.

### RF-06: Crear clases

El sistema debe permitir registrar clases concretas con profesor, fecha y horario. La clase no requiere una materia obligatoria.

### RF-07: Asignar alumnos

El sistema debe permitir que un profesor asigne alumnos manualmente a una clase y registre, de forma opcional, una o varias materias trabajadas por cada alumno.

### RF-08: Registrar asistencia

El sistema debe permitir registrar asistencia o ausencia por alumno y clase.

### RF-08.1: Cerrar clases automáticamente

El sistema debe cerrar automáticamente las clases cuyo horario haya finalizado y resolver las participaciones pendientes según las reglas de asistencia, consumo y recuperación.

### RF-09: Gestionar recuperaciones

El sistema debe permitir registrar una ausencia recuperable cuando el alumno avisa con al menos 12 horas de anticipación.

### RF-10: Registrar uso de recuperación

El sistema debe permitir asociar una recuperación con una clase posterior y evitar su doble utilización.

### RF-11: Gestionar paquetes

El sistema debe permitir crear paquetes con cantidad de clases, precio y alumno asociado.

### RF-12: Registrar pagos

El sistema debe permitir registrar pagos totales o parciales asociados a un paquete.

### RF-13: Consultar saldos

El sistema debe mostrar clases compradas, utilizadas y restantes, además del importe pagado y adeudado.

### RF-14: Consultar historial

El sistema debe permitir consultar clases, asistencia, recuperaciones y pagos de un alumno.

### RF-15: Auditar cambios

El sistema debe registrar el usuario, fecha, dato anterior y dato nuevo de las modificaciones relevantes.

## Reglas de negocio

- Una clase sin aviso previo consume una clase del paquete aunque el alumno no asista.
- Una ausencia avisada con al menos 12 horas puede recuperarse.
- Una recuperación reemplaza el consumo de la clase original.
- Una clase puede incluir alumnos que trabajen materias diferentes.
- Una participación puede no tener ninguna materia especificada.
- La materia se registra en la participación del alumno y no es obligatoria para la clase.
- Un alumno inactivo no puede asignarse a una clase nueva y un alumno no puede repetirse en la misma clase.
- Solo se pueden agregar materias activas a una participación; si una materia se desactiva después, se conserva en su historial.
- La asignación de alumnos y el cambio de sus materias requieren una clase programada.
- Las clases se cierran automáticamente cuando pasa su horario.
- Un alumno puede asistir con varios profesores.
- Un paquete puede estar pagado, parcialmente pagado o adeudado.
- Un paquete puede estar completamente pagado sin haber utilizado clases; el saldo de clases y el saldo monetario se calculan por separado.
- El precio de un paquete es no negativo y un pago debe ser positivo, en ARS y no superar el saldo pendiente.
- Un alumno con deuda puede continuar asistiendo.
- El paquete se crea para un alumno sin asignarlo a una clase. Al asignar una participación se elige `PACKAGE` o `PER_CLASS`; en esta última modalidad se acuerda un precio individual y no se crea un paquete.
- Una participación `PACKAGE` exige que exista un paquete con clases disponibles al asignarla. El consumo efectivo se decide al cerrar la clase; si falta saldo, el cierre se revierte y se reintenta después.
- Una participación `PER_CLASS` genera deuda al cerrarse la clase si no hubo ausencia justificada. Puede pagarse después en uno o varios pagos, sin superar el precio acordado.
- El servidor registra la hora del aviso de ausencia. Con 12 horas o más antes del inicio, la ausencia queda justificada y no genera consumo ni cobro por clase.
- Los alumnos inactivos conservan todo su historial.
- Los profesores y administradores pueden modificar información, pero los cambios quedan auditados.
- Todos los importes se expresan en pesos argentinos (ARS); la divisa no se elige ni modifica desde la API.

## Stack tecnológico inicial

El sistema se implementará como un monolito Spring Boot con arquitectura clásica por capas (`controller`, `dto`, `service`, `repository`, `model`, `security`, `config` y `exception`):

- Java 21 o superior compatible con el proyecto;
- Spring Boot;
- Spring Web;
- Spring Security para autenticación y autorización;
- Spring Data JPA para persistencia;
- Bean Validation para validar entradas;
- PostgreSQL;
- Flyway para versionar migraciones de base de datos;
- Maven;
- Lombok solo cuando reduzca código repetitivo sin ocultar comportamiento;
- JUnit 5 y Spring Test;
- Docker y Docker Compose para desarrollo y despliegue.

Los servicios utilizarán directamente repositorios Spring Data JPA. El modelo persistido utilizará entidades JPA; esta decisión reduce conversiones y abstracciones para el alcance actual. Las entidades no se expondrán directamente como respuestas de la API: los controladores utilizarán DTOs.

## Requisitos no funcionales

- El sistema debe autenticar a los usuarios.
- El sistema debe aplicar permisos según el rol.
- Los datos deben conservarse de forma consistente.
- Las fechas deben utilizar formatos locales.
- Los importes deben expresarse en ARS.
- Las operaciones relevantes deben poder auditarse.
- La interfaz debe ser usable por usuarios no técnicos.
- Debe existir una estrategia de respaldo y recuperación de datos.

## Casos límite

- Duplicación de alumnos en una clase.
- Recuperación utilizada más de una vez.
- Pago parcial superior al saldo.
- Clase cancelada con alumnos asignados.
- Modificación de asistencia ya registrada.
- Paquete sin clases disponibles.
- Alumno inactivo asignado a una nueva clase.
- Eliminación solicitada de un alumno con historial.
- Modificación de un pago o una asistencia ya confirmados.

## MVP

El MVP incluye:

- autenticación;
- usuarios administrador y profesor;
- alta, edición, activación e inactivación de alumnos;
- registro de materias;
- registro de clases concretas;
- asignación manual de alumnos;
- registro de asistencia;
- clases recuperables;
- paquetes de clases;
- pagos totales y parciales;
- saldo de clases;
- deudas;
- historial de cambios;
- consultas y filtros básicos.

## Futuras versiones

- Portal para alumnos.
- Aplicación móvil.
- Notificaciones por WhatsApp o correo.
- Recordatorios de clases y vencimientos.
- Pagos online.
- Facturación electrónica.
- Reportes financieros y de asistencia.
- Exportación a Excel o PDF.
- Múltiples sedes.
- Capacidad máxima por clase.
- Vencimiento automático de paquetes.
- Reglas configurables de recuperación.
- Integraciones externas.

## Complejidad técnica estimada

La complejidad estimada es media. El desafío principal será mantener consistentes las relaciones entre clases, asistencia, recuperaciones, paquetes, pagos parciales, saldos, estados de alumnos e historial de cambios.

El alcance es adecuado para un proyecto de portfolio porque permite demostrar autenticación, autorización, persistencia, validaciones, reglas de negocio, auditoría y pruebas sin requerir microservicios ni infraestructura compleja.
