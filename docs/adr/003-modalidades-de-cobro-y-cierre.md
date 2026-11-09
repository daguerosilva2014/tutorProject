# ADR 003: Modalidades de cobro y cierre de clases

## Contexto

Un alumno puede comprar un paquete antes de conocer sus clases o pagar cada clase individualmente. Un paquete impago puede usarse, y el cobro por clase puede quedar pendiente. El consumo se determina al terminar la clase y depende de la asistencia.

## Decisión

Cada participación conserva una modalidad `PACKAGE` o `PER_CLASS`. El paquete pertenece al alumno, sin relación previa con clases. Para `PACKAGE`, el cierre toma el paquete más antiguo con saldo y registra un `ClassConsumption` único. Para `PER_CLASS`, la participación conserva el precio acordado y sus pagos se registran en `PerClassPayment`. Un `Clock` con zona de la academia determina la anticipación del aviso y el vencimiento de la clase. El cierre de cada clase es transaccional e idempotente; si falta saldo, se revierte y se reintenta.

## Alternativas consideradas

- Crear un paquete ficticio para cada clase individual: simplifica el descuento de clases, pero confunde la deuda individual con la compra de un paquete y genera registros que no corresponden al negocio.
- Asignar un paquete concreto al crear la participación: reserva saldo anticipadamente, pero acopla la compra a clases aún no tomadas y dificulta usar primero el paquete más antiguo disponible al cierre.

## Consecuencias

La respuesta de participación expone modalidad, asistencia, consumo y saldo. La selección del paquete ocurre al cerrar, por lo que varias clases programadas pueden competir por el mismo saldo; la versión JPA del paquete evita un consumo concurrente indebido. Si se agota antes del cierre, la clase queda programada hasta registrar saldo. La recuperación de ausencias justificadas se diseñará en E7 sobre estos estados.
