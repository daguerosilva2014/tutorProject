import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, getActiveTeachers, getClasses, type AcademyClass, type ClassFilters, type Session, type Teacher } from './api'
import ClassPanel from './ClassPanel'

const emptyFilters: ClassFilters = { from: '', to: '', teacherId: '', status: 'ALL' }

export default function ClassesPage({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [classes, setClasses] = useState<AcademyClass[]>([])
  const [draftFilters, setDraftFilters] = useState<ClassFilters>(emptyFilters)
  const [filters, setFilters] = useState<ClassFilters>(emptyFilters)
  const [filterError, setFilterError] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [selection, setSelection] = useState<{ kind: 'new' } | { kind: 'class'; id: string } | null>(null)
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [teachersLoading, setTeachersLoading] = useState(session.user.role === 'ADMIN')
  const [teachersError, setTeachersError] = useState(false)
  const [teachersReloadKey, setTeachersReloadKey] = useState(0)

  useEffect(() => {
    if (session.user.role !== 'ADMIN') return
    let active = true
    getActiveTeachers(session.accessToken)
      .then(result => { if (active) setTeachers(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setTeachersError(true)
      })
      .finally(() => { if (active) setTeachersLoading(false) })
    return () => { active = false }
  }, [session.accessToken, session.user.role, teachersReloadKey, onLogout])

  useEffect(() => {
    const controller = new AbortController()
    getClasses(session.accessToken, filters, controller.signal)
      .then(setClasses)
      .catch((caught: unknown) => {
        if (controller.signal.aborted) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError('No se pudieron cargar las clases. Intentá de nuevo.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [session.accessToken, filters, reloadKey, onLogout])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (draftFilters.from && draftFilters.to && draftFilters.from > draftFilters.to) {
      setFilterError('La fecha inicial debe ser anterior o igual a la final.')
      return
    }
    setFilterError('')
    setError('')
    setLoading(true)
    setFilters({ ...draftFilters })
  }

  function clearFilters() {
    setDraftFilters(emptyFilters)
    setFilters({ ...emptyFilters })
    setFilterError('')
    setError('')
    setLoading(true)
  }

  function refresh() {
    setError('')
    setLoading(true)
    setReloadKey(key => key + 1)
  }

  function retryTeachers() {
    setTeachersLoading(true)
    setTeachersError(false)
    setTeachersReloadKey(key => key + 1)
  }

  return <main className="content">
    <div className="page-heading"><div><span className="eyebrow">Agenda</span><h1>Clases</h1><p className="muted">Programá y consultá las clases de la academia.</p></div>
      <div className="heading-actions"><button className="primary-button" type="button" onClick={() => setSelection({ kind: 'new' })}>Programar clase</button>
        <div className="count-card"><span>Total mostrado</span><strong>{loading || error ? '—' : classes.length}</strong></div></div></div>
    {selection && <ClassPanel key={selection.kind === 'new' ? 'new' : selection.id} session={session}
      classId={selection.kind === 'class' ? selection.id : null} teachers={teachers} teachersLoading={teachersLoading} teachersError={teachersError}
      onClose={() => setSelection(null)} onLogout={onLogout} onSaved={refresh} onRetryTeachers={retryTeachers} />}
    <section className="list-card" aria-labelledby="class-list-title">
      <div className="list-toolbar class-toolbar"><div><h2 id="class-list-title">Listado de clases</h2><p className="muted">Filtrá por fecha, estado o profesor.</p></div>
        <form className="class-filters" onSubmit={applyFilters} role="search">
          <div className="field"><label htmlFor="class-from">Desde</label><input id="class-from" type="date" value={draftFilters.from} onChange={event => setDraftFilters({ ...draftFilters, from: event.target.value })} /></div>
          <div className="field"><label htmlFor="class-to">Hasta</label><input id="class-to" type="date" value={draftFilters.to} onChange={event => setDraftFilters({ ...draftFilters, to: event.target.value })} /></div>
          <div className="field"><label htmlFor="class-status">Estado</label><select id="class-status" value={draftFilters.status} onChange={event => setDraftFilters({ ...draftFilters, status: event.target.value })}>
            <option value="ALL">Todos</option><option value="SCHEDULED">Programadas</option><option value="FINALIZED">Finalizadas</option><option value="CANCELLED">Canceladas</option>
          </select></div>
          {session.user.role === 'ADMIN' && <div className="field"><label htmlFor="class-teacher-filter">Profesor</label><select id="class-teacher-filter" value={draftFilters.teacherId} onChange={event => setDraftFilters({ ...draftFilters, teacherId: event.target.value })} disabled={teachersLoading || teachersError}>
            <option value="">Todos</option>{teachers.map(teacher => <option key={teacher.id} value={teacher.id}>{teacher.name}</option>)}
          </select></div>}
          <div className="filter-actions"><button className="secondary-button" type="submit">Aplicar filtros</button><button className="text-button" type="button" onClick={clearFilters}>Limpiar</button></div>
          {filterError && <p className="form-error filter-error" role="alert">{filterError}</p>}
        </form></div>
      {loading ? <div className="list-message" role="status">Cargando clases…</div> :
        error ? <div className="list-message" role="alert"><p>{error}</p><button className="secondary-button" type="button" onClick={refresh}>Reintentar</button></div> :
        classes.length === 0 ? <div className="list-message"><h3>No se encontraron clases</h3><p>Probá con otros filtros o programá una clase nueva.</p></div> :
        <div className="table-scroll"><table><thead><tr><th scope="col">Fecha</th><th scope="col">Horario</th><th scope="col">Profesor</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
          <tbody>{classes.map(academyClass => <tr key={academyClass.id}><td>{academyClass.date.split('-').reverse().join('/')}</td>
            <td>{academyClass.startTime.slice(0, 5)} a {academyClass.endTime.slice(0, 5)}</td><td>{academyClass.teacher.name}</td>
            <td><span className={`status-badge ${academyClass.status.toLowerCase()}`}>{academyClass.status === 'SCHEDULED' ? 'Programada' : academyClass.status === 'FINALIZED' ? 'Finalizada' : 'Cancelada'}</span></td>
            <td><button className="text-button" type="button" onClick={() => setSelection({ kind: 'class', id: academyClass.id })} aria-label={`Ver clase del ${academyClass.date} con ${academyClass.teacher.name}`}>Ver detalle</button></td>
          </tr>)}</tbody></table></div>}
    </section>
  </main>
}
