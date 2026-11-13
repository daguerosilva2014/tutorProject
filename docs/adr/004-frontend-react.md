# ADR-004: Frontend React separado del backend

## Estado

Aceptada.

## Contexto

El backend ya expone una API REST con autenticación JWT. Se necesita una interfaz para administradores y profesores antes de continuar con las siguientes épicas. La primera entrega debe ser pequeña y permitir verificar la integración real con la API.

## Decisión

Crear un frontend en `frontend/` con React, TypeScript y Vite. Durante el desarrollo, Vite dirigirá `/api` al backend Spring Boot. La primera entrega incluye login y consulta de alumnos. El token JWT permanecerá solo en memoria y se descartará al recargar o cerrar sesión.

## Alternativa considerada

Thymeleaf dentro de Spring Boot permitiría una sola aplicación, pero incorporaría vistas de servidor a un backend diseñado como API REST. React mantiene la interfaz separada y permite sumar pantallas operativas sin cambiar los controladores existentes.

## Consecuencias

El desarrollo requiere Node.js además de Java. La sesión no persiste entre recargas, coherente con el JWT actual sin refresh token. Para un despliegue se deberá servir el frontend y dirigir `/api` al backend. Las reglas de autorización permanecen en Spring Security.
