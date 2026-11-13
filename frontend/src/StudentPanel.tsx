import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, changeStudentStatus, createStudent, getStudent, updateStudent, type Student, type StudentInput } from './api'

type Props = {
  token: string
  studentId: string | null
  onClose: () => void
  onLogout: () => void
  onSaved: () => void
}

const emptyForm: StudentInput = { name: '', contactNumber: null, notes: null }

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Revisá los datos ingresados. El nombre es obligatorio y los campos tienen un límite de caracteres.'
    if (error.status === 403) return 'Tu usuario no tiene permiso para realizar esta acción.'
    if (error.status === 404) return 'Este alumno ya no existe. Cerrá el panel y actualizá la lista.'
  }
  return 'No se pudo completar la operación. Intentá de nuevo.'
}

export default function StudentPanel({ token, studentId, onClose, onLogout, onSaved }: Props) {
  const [student, setStudent] = useState<Student | null>(null)
  const [form, setForm] = useState<StudentInput>(emptyForm)
  const [editing, setEditing] = useState(studentId === null)
  const [loading, setLoading] = useState(studentId !== null)
  const [saving, setSaving] = useState(false)
  const [confirmStatus, setConfirmStatus] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (!studentId) return
    let active = true
    getStudent(token, studentId)
      .then(result => { if (active) setStudent(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError(errorMessage(caught))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [studentId, token, onLogout])

  function startEdit() {
    if (!student) return
    setForm({ name: student.name, contactNumber: student.contactNumber, notes: student.notes })
    setError('')
    setSuccess('')
    setEditing(true)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSaving(true)
    const input = {
      name: form.name.trim(),
      contactNumber: form.contactNumber?.trim() || null,
      notes: form.notes?.trim() || null,
    }
    try {
      const result = student
        ? await updateStudent(token, student.id, input)
        : await createStudent(token, input)
      setStudent(result)
      setEditing(false)
      setSuccess(student ? 'Cambios guardados.' : 'Alumno creado correctamente.')
      onSaved()
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(errorMessage(caught))
    } finally {
      setSaving(false)
    }
  }

  async function toggleStatus() {
    if (!student) return
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const result = await changeStudentStatus(token, student.id, student.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      setStudent(result)
      setConfirmStatus(false)
      setSuccess(result.status === 'ACTIVE' ? 'Alumno activado.' : 'Alumno inactivado.')
      onSaved()
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(errorMessage(caught))
    } finally {
      setSaving(false)
    }
  }

  const title = editing ? (student ? 'Editar alumno' : 'Nuevo alumno') : 'Detalle del alumno'
  return <section className="entity-panel" aria-labelledby="student-panel-title">
    <div className="panel-heading"><div><span className="eyebrow">Gestión de alumnos</span><h2 id="student-panel-title">{title}</h2></div>
      <button className="text-button" type="button" onClick={onClose}>Cerrar</button></div>
    {success && <p className="form-success" role="status">{success}</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {loading ? <p role="status">Cargando alumno…</p> : editing ?
      <form className="entity-form" onSubmit={save}>
        <div className="field"><label htmlFor="student-name">Nombre *</label><input id="student-name" autoFocus required maxLength={120} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></div>
        <div className="field"><label htmlFor="student-contact">Número de contacto</label><input id="student-contact" type="tel" maxLength={30} value={form.contactNumber ?? ''} onChange={event => setForm({ ...form, contactNumber: event.target.value })} /></div>
        <div className="field field-wide"><label htmlFor="student-notes">Notas</label><textarea id="student-notes" rows={4} maxLength={1000} value={form.notes ?? ''} onChange={event => setForm({ ...form, notes: event.target.value })} /></div>
        <div className="panel-actions"><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : student ? 'Guardar cambios' : 'Crear alumno'}</button>
          <button className="secondary-button" type="button" disabled={saving} onClick={student ? () => { setEditing(false); setError('') } : onClose}>Cancelar</button></div>
      </form> : student ? <div className="entity-details">
        <dl><div><dt>Nombre</dt><dd>{student.name}</dd></div><div><dt>Contacto</dt><dd>{student.contactNumber || 'Sin número'}</dd></div>
          <div><dt>Estado</dt><dd>{student.status === 'ACTIVE' ? 'Activo' : 'Inactivo'}</dd></div>
          <div><dt>Fecha de alta</dt><dd>{new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium' }).format(new Date(student.createdAt))}</dd></div>
          <div className="field-wide"><dt>Notas</dt><dd className="notes-text">{student.notes || 'Sin notas'}</dd></div></dl>
        <div className="panel-actions"><button className="primary-button" type="button" onClick={startEdit}>Editar datos</button>
          <button className="secondary-button" type="button" onClick={() => setConfirmStatus(true)}>{student.status === 'ACTIVE' ? 'Inactivar alumno' : 'Activar alumno'}</button></div>
        {confirmStatus && <div className="status-confirm" role="group" aria-label="Confirmar cambio de estado"><p>¿Querés {student.status === 'ACTIVE' ? 'inactivar' : 'activar'} a {student.name}?</p>
          <div className="panel-actions"><button className="primary-button" type="button" disabled={saving} onClick={toggleStatus}>{saving ? 'Guardando…' : 'Confirmar cambio'}</button>
            <button className="secondary-button" type="button" disabled={saving} onClick={() => setConfirmStatus(false)}>Cancelar</button></div></div>}
      </div> : null}
  </section>
}
