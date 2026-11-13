import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, cancelClass, createClass, getClass, updateClass, type AcademyClass, type ClassInput, type Session, type Teacher } from './api'
import ClassParticipants from './ClassParticipants'

type Props = {
  session: Session
  classId: string | null
  teachers: Teacher[]
  teachersLoading: boolean
  teachersError: boolean
  onClose: () => void
  onLogout: () => void
  onSaved: () => void
  onRetryTeachers: () => void
}

const emptyForm: ClassInput = { date: '', startTime: '', endTime: '', notes: null }

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Revisá la fecha, el horario y las notas ingresadas.'
    if (error.status === 403) return 'Tu usuario no tiene permiso para esta clase.'
    if (error.status === 404) return 'La clase o el profesor ya no existen. Actualizá la lista.'
    if (error.status === 409 || error.status === 422) return 'La clase ya no admite este cambio o el profesor no está activo. Actualizá la lista.'
  }
  return 'No se pudo completar la operación. Intentá de nuevo.'
}

function timeForApi(value: string): string {
  return value.length === 5 ? `${value}:00` : value
}

export default function ClassPanel({ session, classId, teachers, teachersLoading, teachersError, onClose, onLogout, onSaved, onRetryTeachers }: Props) {
  const [academyClass, setAcademyClass] = useState<AcademyClass | null>(null)
  const [form, setForm] = useState<ClassInput>(emptyForm)
  const [teacherId, setTeacherId] = useState('')
  const [editing, setEditing] = useState(classId === null)
  const [loading, setLoading] = useState(classId !== null)
  const [saving, setSaving] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (!classId) return
    let active = true
    getClass(session.accessToken, classId)
      .then(result => { if (active) setAcademyClass(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError(errorMessage(caught))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [classId, session.accessToken, onLogout])

  function startEdit() {
    if (!academyClass) return
    setForm({ date: academyClass.date, startTime: academyClass.startTime.slice(0, 5), endTime: academyClass.endTime.slice(0, 5), notes: academyClass.notes })
    setError('')
    setSuccess('')
    setEditing(true)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (form.startTime >= form.endTime) {
      setError('La hora de fin debe ser posterior a la hora de inicio y estar dentro del mismo día.')
      return
    }
    if (!academyClass && session.user.role === 'ADMIN' && !teacherId) {
      setError('Seleccioná un profesor activo para programar la clase.')
      return
    }
    setSaving(true)
    const input = { date: form.date, startTime: timeForApi(form.startTime), endTime: timeForApi(form.endTime), notes: form.notes?.trim() || null }
    try {
      const result = academyClass
        ? await updateClass(session.accessToken, academyClass.id, input)
        : await createClass(session.accessToken, { ...input, teacherId: session.user.role === 'ADMIN' ? teacherId : null })
      setAcademyClass(result)
      setEditing(false)
      setSuccess(academyClass ? 'Cambios guardados.' : 'Clase programada correctamente.')
      onSaved()
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(errorMessage(caught))
    } finally {
      setSaving(false)
    }
  }

  async function confirmCancellation() {
    if (!academyClass) return
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const result = await cancelClass(session.accessToken, academyClass.id)
      setAcademyClass(result)
      setConfirmCancel(false)
      setSuccess('Clase cancelada.')
      onSaved()
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(errorMessage(caught))
    } finally {
      setSaving(false)
    }
  }

  const title = editing ? (academyClass ? 'Editar clase' : 'Programar clase') : 'Detalle de la clase'
  const noTeachers = session.user.role === 'ADMIN' && !academyClass && teachers.length === 0
  return <section className="entity-panel" aria-labelledby="class-panel-title">
    <div className="panel-heading"><div><span className="eyebrow">Agenda</span><h2 id="class-panel-title">{title}</h2></div>
      <button className="text-button" type="button" onClick={onClose}>Cerrar</button></div>
    {success && <p className="form-success" role="status">{success}</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {loading ? <p role="status">Cargando clase…</p> : editing ? <>
      {noTeachers && <div className="form-error" role="status">{teachersLoading ? 'Cargando profesores…' : teachersError ? <><span>No se pudieron cargar los profesores.</span> <button className="text-button" type="button" onClick={onRetryTeachers}>Reintentar</button></> : 'No hay profesores activos. Creá uno desde la API de usuarios antes de programar una clase.'}</div>}
      <form className="entity-form" onSubmit={save}>
        {!academyClass && session.user.role === 'ADMIN' && <div className="field field-wide"><label htmlFor="class-teacher">Profesor *</label>
          <select id="class-teacher" required value={teacherId} onChange={event => setTeacherId(event.target.value)} disabled={teachersLoading || teachersError || noTeachers}>
            <option value="">Seleccionar profesor</option>{teachers.map(teacher => <option key={teacher.id} value={teacher.id}>{teacher.name}</option>)}
          </select></div>}
        <div className="field"><label htmlFor="class-date">Fecha *</label><input id="class-date" type="date" required value={form.date} onChange={event => setForm({ ...form, date: event.target.value })} /></div>
        <div className="field"><label htmlFor="class-start">Hora de inicio *</label><input id="class-start" type="time" required value={form.startTime} onChange={event => setForm({ ...form, startTime: event.target.value })} /></div>
        <div className="field"><label htmlFor="class-end">Hora de fin *</label><input id="class-end" type="time" required value={form.endTime} onChange={event => setForm({ ...form, endTime: event.target.value })} /></div>
        <div className="field field-wide"><label htmlFor="class-notes">Notas</label><textarea id="class-notes" rows={4} maxLength={1000} value={form.notes ?? ''} onChange={event => setForm({ ...form, notes: event.target.value })} /></div>
        <div className="panel-actions"><button className="primary-button" type="submit" disabled={saving || noTeachers}>{saving ? 'Guardando…' : academyClass ? 'Guardar cambios' : 'Programar clase'}</button>
          <button className="secondary-button" type="button" disabled={saving} onClick={academyClass ? () => { setEditing(false); setError('') } : onClose}>Cancelar</button></div>
      </form></> : academyClass ? <div className="entity-details">
      <dl><div><dt>Fecha</dt><dd>{academyClass.date.split('-').reverse().join('/')}</dd></div>
        <div><dt>Horario</dt><dd>{academyClass.startTime.slice(0, 5)} a {academyClass.endTime.slice(0, 5)}</dd></div>
        <div><dt>Profesor</dt><dd>{academyClass.teacher.name}</dd></div>
        <div><dt>Estado</dt><dd>{academyClass.status === 'SCHEDULED' ? 'Programada' : academyClass.status === 'FINALIZED' ? 'Finalizada' : 'Cancelada'}</dd></div>
        <div className="field-wide"><dt>Notas</dt><dd className="notes-text">{academyClass.notes || 'Sin notas'}</dd></div></dl>
      {academyClass.status === 'SCHEDULED' && <div className="panel-actions"><button className="primary-button" type="button" onClick={startEdit}>Editar clase</button>
        <button className="secondary-button" type="button" onClick={() => setConfirmCancel(true)}>Cancelar clase</button></div>}
      {confirmCancel && <div className="status-confirm" role="group" aria-label="Confirmar cancelación"><p>¿Querés cancelar esta clase del {academyClass.date.split('-').reverse().join('/')}? Esta acción no se puede deshacer.</p>
        <div className="panel-actions"><button className="primary-button" type="button" disabled={saving} onClick={confirmCancellation}>{saving ? 'Cancelando…' : 'Confirmar cancelación'}</button>
          <button className="secondary-button" type="button" disabled={saving} onClick={() => setConfirmCancel(false)}>Volver</button></div></div>}
      <ClassParticipants token={session.accessToken} academyClass={academyClass} onLogout={onLogout} />
    </div> : null}
  </section>
}
