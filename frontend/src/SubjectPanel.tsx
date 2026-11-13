import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, changeSubjectStatus, createSubject, getSubject, updateSubject, type Subject, type SubjectInput } from './api'

type Props = {
  token: string
  subjectId: string | null
  onClose: () => void
  onLogout: () => void
  onSaved: () => void
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) return 'Revisá los datos ingresados. El nombre es obligatorio y los campos tienen un límite de caracteres.'
    if (error.status === 403) return 'Tu usuario no tiene permiso para realizar esta acción.'
    if (error.status === 404) return 'Esta materia ya no existe. Cerrá el panel y actualizá la lista.'
  }
  return 'No se pudo completar la operación. Intentá de nuevo.'
}

export default function SubjectPanel({ token, subjectId, onClose, onLogout, onSaved }: Props) {
  const [subject, setSubject] = useState<Subject | null>(null)
  const [form, setForm] = useState<SubjectInput>({ name: '', description: null })
  const [editing, setEditing] = useState(subjectId === null)
  const [loading, setLoading] = useState(subjectId !== null)
  const [saving, setSaving] = useState(false)
  const [confirmStatus, setConfirmStatus] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (!subjectId) return
    let active = true
    getSubject(token, subjectId)
      .then(result => { if (active) setSubject(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError(errorMessage(caught))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [subjectId, token, onLogout])

  function startEdit() {
    if (!subject) return
    setForm({ name: subject.name, description: subject.description })
    setError('')
    setSuccess('')
    setEditing(true)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSaving(true)
    const input = { name: form.name.trim(), description: form.description?.trim() || null }
    try {
      const result = subject
        ? await updateSubject(token, subject.id, input)
        : await createSubject(token, input)
      setSubject(result)
      setEditing(false)
      setSuccess(subject ? 'Cambios guardados.' : 'Materia creada correctamente.')
      onSaved()
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(errorMessage(caught))
    } finally {
      setSaving(false)
    }
  }

  async function toggleStatus() {
    if (!subject) return
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const result = await changeSubjectStatus(token, subject.id, subject.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      setSubject(result)
      setConfirmStatus(false)
      setSuccess(result.status === 'ACTIVE' ? 'Materia activada.' : 'Materia inactivada.')
      onSaved()
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(errorMessage(caught))
    } finally {
      setSaving(false)
    }
  }

  const title = editing ? (subject ? 'Editar materia' : 'Nueva materia') : 'Detalle de la materia'
  return <section className="entity-panel" aria-labelledby="subject-panel-title">
    <div className="panel-heading"><div><span className="eyebrow">Gestión de materias</span><h2 id="subject-panel-title">{title}</h2></div>
      <button className="text-button" type="button" onClick={onClose}>Cerrar</button></div>
    {success && <p className="form-success" role="status">{success}</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {loading ? <p role="status">Cargando materia…</p> : editing ?
      <form className="entity-form" onSubmit={save}>
        <div className="field field-wide"><label htmlFor="subject-name">Nombre *</label><input id="subject-name" autoFocus required maxLength={120} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></div>
        <div className="field field-wide"><label htmlFor="subject-description">Descripción</label><textarea id="subject-description" rows={4} maxLength={1000} value={form.description ?? ''} onChange={event => setForm({ ...form, description: event.target.value })} /></div>
        <div className="panel-actions"><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : subject ? 'Guardar cambios' : 'Crear materia'}</button>
          <button className="secondary-button" type="button" disabled={saving} onClick={subject ? () => { setEditing(false); setError('') } : onClose}>Cancelar</button></div>
      </form> : subject ? <div className="entity-details">
        <dl><div><dt>Nombre</dt><dd>{subject.name}</dd></div><div><dt>Estado</dt><dd>{subject.status === 'ACTIVE' ? 'Activa' : 'Inactiva'}</dd></div>
          <div className="field-wide"><dt>Descripción</dt><dd className="notes-text">{subject.description || 'Sin descripción'}</dd></div></dl>
        <div className="panel-actions"><button className="primary-button" type="button" onClick={startEdit}>Editar datos</button>
          <button className="secondary-button" type="button" onClick={() => setConfirmStatus(true)}>{subject.status === 'ACTIVE' ? 'Inactivar materia' : 'Activar materia'}</button></div>
        {confirmStatus && <div className="status-confirm" role="group" aria-label="Confirmar cambio de estado"><p>¿Querés {subject.status === 'ACTIVE' ? 'inactivar' : 'activar'} la materia {subject.name}?</p>
          <div className="panel-actions"><button className="primary-button" type="button" disabled={saving} onClick={toggleStatus}>{saving ? 'Guardando…' : 'Confirmar cambio'}</button>
            <button className="secondary-button" type="button" disabled={saving} onClick={() => setConfirmStatus(false)}>Cancelar</button></div></div>}
      </div> : null}
  </section>
}
