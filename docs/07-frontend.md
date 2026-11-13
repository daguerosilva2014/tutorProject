# Frontend

## Alcance actual

La interfaz está destinada a administradores y profesores. Incluye inicio y cierre de sesión y navegación entre alumnos, materias y clases. Alumnos y materias tienen listado con búsqueda y filtro, alta, detalle, edición y cambio de estado con confirmación. Clases tiene listado con filtros por fechas y estado, programación, detalle, edición y cancelación con confirmación. Consume los endpoints existentes de `/api/v1/students`, `/api/v1/subjects`, `/api/v1/classes`, `/api/v1/users` para el selector de profesores y `POST /api/v1/auth/login`.

Todavía no incorpora las pantallas de participaciones, asistencia, paquetes, pagos ni recuperaciones. La autorización y validación definitiva siguen estando a cargo del backend.

## Estructura y ejecución

El proyecto React con TypeScript y Vite vive en `frontend/` y se ejecuta separado de Spring Boot durante el desarrollo. `frontend/src/api.ts` concentra las llamadas HTTP; `App.tsx` contiene el ingreso, navegación y listado de alumnos; `SubjectsPage.tsx` y `ClassesPage.tsx` contienen los otros listados; `StudentPanel.tsx`, `SubjectPanel.tsx` y `ClassPanel.tsx` contienen formularios y detalles. El servidor de Vite dirige `/api` a `http://localhost:8080`, por lo que el navegador hace solicitudes al mismo origen de desarrollo.

Con PostgreSQL y el backend iniciados mediante `./scripts/dev-backend.sh`, ejecutar `npm ci` y `npm run dev` desde `frontend/`. El script usa la base local separada `academia_pruebas` para no afectar administradores o datos anteriores en `academia`. Para comprobar compilación y tipos, ejecutar `npm run build`.
Las llamadas HTTP tienen pruebas con `npm test`; `npm run lint` revisa el código estáticamente.

## Sesión y errores

El JWT se guarda únicamente en el estado de React. Al salir, recibir `401` o recargar la página, el usuario debe iniciar sesión de nuevo. La interfaz no guarda tokens en `localStorage` ni imprime credenciales en la consola. Los formularios muestran errores de validación o permisos; los listados distinguen carga, error y resultado vacío. La lista correspondiente se actualiza tras guardar cambios. Si los filtros actuales excluyen al alumno, materia o clase recién creados o modificados, seguirán visibles en el panel de detalle pero no en esa lista hasta cambiar los filtros. Cambiar de sección cierra el panel abierto y reinicia los filtros de la sección anterior.

En clases, el administrador selecciona un profesor activo obtenido de `/api/v1/users`; un profesor programa clases para sí mismo sin selector. La base de prueba puede comenzar sin profesores: en ese caso el administrador puede consultar clases, pero necesita crear un profesor mediante la API de usuarios antes de programar una. La interfaz solo permite editar o cancelar clases `SCHEDULED`, como el backend. Los filtros de fecha se aplican con un botón y rechazan un rango invertido.

Para servir el frontend compilado en otro entorno, se debe configurar el servidor web para dirigir `/api` al backend. Esa configuración de despliegue queda para E9.
