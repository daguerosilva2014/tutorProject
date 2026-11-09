# ADR-002: Arquitectura clásica por capas

## Estado

Aceptada.

## Contexto

La arquitectura inicial organizaba el código por funcionalidad y aplicaba separación hexagonal con puertos, adaptadores y modelos distintos para dominio y JPA. Para el alcance actual, esa estructura añadía conversiones y abstracciones difíciles de seguir sin un beneficio práctico proporcional.

El proyecto también tiene un objetivo educativo: las responsabilidades y el recorrido de una petición deben ser fáciles de identificar para un estudiante que trabaja con Spring.

## Decisión

Organizar el monolito por capas técnicas: `controller`, `dto`, `service`, `repository`, `model`, `security`, `config` y `exception`.

- Los controladores convierten HTTP a llamadas de servicio y construyen respuestas DTO.
- Los servicios coordinan operaciones y transacciones.
- Los servicios utilizan interfaces de Spring Data JPA directamente.
- Las entidades JPA también son los modelos persistidos; no se mantienen adaptadores ni mappers duplicados.
- Spring Security y JWT permanecen en la capa de seguridad/configuración.

## Alternativas consideradas

- **Módulos por funcionalidad y capas internas:** mantiene agrupados los cambios de cada módulo, pero conserva la estructura que se decidió simplificar.
- **Separación hexagonal completa:** desacopla dominio y persistencia, aunque requiere interfaces, adaptadores y conversiones adicionales.
- **Capas globales con modelo separado de JPA:** reduce el agrupamiento por funcionalidad, pero conserva una duplicación que no se necesita hoy.

## Consecuencias

El código es más directo para el tamaño actual y el recorrido de una operación es visible como controller → service → repository → model. Los servicios dependen de Spring Data y las entidades dependen de JPA. Si aparecen reglas de dominio complejas o más de una fuente de persistencia, se deberá reevaluar el aislamiento mediante otro ADR.

Los contratos HTTP permanecen en DTOs, por lo que los cambios en columnas o entidades no se exponen automáticamente en la API.
