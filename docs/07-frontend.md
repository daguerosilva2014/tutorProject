# Frontend

## Alcance actual

La interfaz React permite iniciar y cerrar sesión, administrar alumnos y materias, programar y consultar clases y, para el rol administrador, crear y editar usuarios. En el detalle de un alumno se pueden crear paquetes, consultar saldos y registrar pagos. En el detalle de una clase se pueden asignar alumnos con modalidad `PACKAGE` o `PER_CLASS`, elegir materias, registrar asistencia o ausencia, consultar pagos individuales y asignar recuperaciones cuando la ausencia cumple los requisitos.

La interfaz consume los endpoints existentes del backend. Las reglas de permisos, cobro, cierre de clases, consumo de paquetes y recuperación siguen siendo responsabilidad del backend. No hay pantallas de historial ni auditoría porque `/api/v1/students/{studentId}/history` y `/api/v1/audit-entries` aún no están implementados.

## Organización

El proyecto vive en `frontend/`. `src/api.ts` reúne los contratos y las llamadas HTTP; `src/App.tsx` contiene el ingreso y la navegación; las pantallas y paneles de cada recurso están en archivos separados. `PaymentForm.tsx` se usa tanto para pagos de paquetes como para pagos por clase. No se agregó un router: cambiar de sección desmonta la pantalla anterior y reinicia sus filtros.

En desarrollo, Vite dirige `/api` a `http://localhost:8080`. Para iniciar el sistema:

```bash
docker compose up -d postgres
./scripts/dev-backend.sh
```

En otra terminal:

```bash
cd frontend
npm ci
npm run dev
```

Abrir la URL de Vite, habitualmente `http://localhost:5173`. El backend usa la base separada `academia_pruebas`. El usuario administrador inicial y su contraseña local se encuentran en `.env`, ignorado por Git.

## Reglas visibles en la interfaz

- Solo el administrador ve **Usuarios**. La contraseña inicial se ingresa al crear la cuenta y no se puede consultar después. El frontend impide que el administrador cambie su propio rol o deshabilite su cuenta desde esa pantalla.
- El administrador debe elegir un profesor activo al programar una clase. Un profesor programa sus propias clases. Solo las clases `SCHEDULED` pueden editarse, cancelarse o recibir alumnos y marcas de asistencia.
- La modalidad `PACKAGE` requiere que el alumno tenga clases disponibles en algún paquete. `PER_CLASS` requiere un precio individual. Las materias son opcionales.
- Un aviso de ausencia se envía sin fecha ni hora aportadas por el navegador. El backend decide si llega con la anticipación suficiente para justificarla. La marca de asistencia ya registrada no se cambia.
- Los pagos por clase aparecen cuando la clase está `FINALIZED` y la participación es `PER_CLASS`. Una recuperación se ofrece solo para una participación con ausencia justificada, sin consumo y con clase original finalizada; el backend verifica también la clase destino.
- Los formularios muestran carga, errores y confirmación. El frontend limita el importe de un pago al saldo mostrado, y el backend vuelve a validarlo antes de guardar.

## Sesión y verificación

El JWT se guarda solo en memoria. Al recargar, salir o recibir `401`, hay que iniciar sesión de nuevo. No se guardan credenciales en `localStorage` ni se imprimen en consola.

Desde `frontend/`, ejecutar `npm test`, `npm run lint` y `npm run build`. Para una revisión manual completa: crear un alumno, agregarle un paquete, registrar un pago, programar una clase, asignar al alumno, elegir materias y registrar asistencia. Para comprobar pagos individuales y recuperaciones hay que esperar a que una clase se cierre; el frontend no fuerza su finalización.

Para servir el frontend compilado en otro entorno, el servidor web debe dirigir `/api` al backend. Esa configuración de despliegue queda para E9.
