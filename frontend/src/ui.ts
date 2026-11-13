import { ApiError } from './api'

export function formatMoney(amount: number): string {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(amount)
}

export function formatDate(date: string): string {
  return date.split('-').reverse().join('/')
}

export function localToday(): string {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const domainErrors: Record<string, string> = {
  USERNAME_ALREADY_EXISTS: 'Ese nombre de usuario ya está en uso.',
  INVALID_PASSWORD: 'La contraseña debe tener al menos 8 caracteres.',
  DUPLICATE_PARTICIPATION: 'El alumno ya está asignado a esta clase.',
  NO_PACKAGE_BALANCE: 'El alumno no tiene clases disponibles en un paquete.',
  PAYMENT_EXCEEDS_BALANCE: 'El importe supera el saldo pendiente.',
  ATTENDANCE_CONFLICT: 'La asistencia ya fue registrada con otra marca.',
  INVALID_RECOVERY: 'La ausencia o la clase destino no cumplen las condiciones de recuperación.',
  INVALID_CLASS_STATE: 'La clase ya no admite este cambio.',
  INVALID_BILLING_MODE: 'La modalidad de cobro no coincide con el precio ingresado.',
  INVALID_TEACHER: 'Seleccioná un profesor activo.',
  INVALID_CLASS_SCHEDULE: 'Revisá el rango de fechas o el horario de la clase.',
  INACTIVE_STUDENT: 'El alumno está inactivo y no puede asignarse a la clase.',
  INACTIVE_SUBJECT: 'No se puede agregar una materia inactiva.',
  CONCURRENT_UPDATE: 'Los datos cambiaron durante la operación. Actualizá la pantalla e intentá de nuevo.',
}

export function describeApiError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code && domainErrors[error.code]) return domainErrors[error.code]
    if (error.status === 400) return 'Revisá los datos ingresados.'
    if (error.status === 403) return 'Tu usuario no tiene permiso para esta operación.'
    if (error.status === 404) return 'El recurso ya no existe. Actualizá la lista.'
    if (error.status === 409 || error.status === 422) return 'La operación no cumple una regla de negocio. Actualizá los datos e intentá de nuevo.'
  }
  return 'No se pudo completar la operación. Intentá de nuevo.'
}
