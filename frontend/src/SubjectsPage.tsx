import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, getSubjects, type Session, type Subject } from './api'
import SubjectPanel from './SubjectPanel'

export default function SubjectsPage({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [selection, setSelection] = useState<{ kind: 'new' } | { kind: 'subject'; id: string } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getSubjects(session.accessToken, search, status, controller.signal)
      .then(setSubjects)
      .catch((caught: unknown) => {
        if (controller.signal.aborted) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError('No se pudieron cargar las materias. Intentá de nuevo.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [session.accessToken, search, status, reloadKey, onLogout])

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    const term = searchInput.trim()
    if (term === search) setReloadKey(key => key + 1)
    else setSearch(term)
  }

  function handleStatusChange(value: string) {
    setLoading(true)
    setError('')
    setStatus(value)
  }

  function refresh() {
    setLoading(true)
    setError('')
    setReloadKey(key => key + 1)
  }

  return <main className="content">
    <div className="page-heading"><div><span className="eyebrow">Catálogo</span><h1>Materias</h1><p className="muted">Organizá las materias disponibles para las clases.</p></div>
      <div className="heading-actions"><button className="primary-button" type="button" onClick={() => setSelection({ kind: 'new' })}>Nueva materia</button>
        <div className="count-card"><span>Total mostrado</span><strong>{loading || error ? '—' : subjects.length}</strong></div></div></div>
    {selection && <SubjectPanel key={selection.kind === 'new' ? 'new' : selection.id} token={session.accessToken}
      subjectId={selection.kind === 'subject' ? selection.id : null} onClose={() => setSelection(null)}
      onLogout={onLogout} onSaved={refresh} />}
    <section className="list-card" aria-labelledby="subject-list-title">
      <div className="list-toolbar"><div><h2 id="subject-list-title">Listado de materias</h2><p className="muted">Buscá por nombre o filtrá por estado.</p></div>
        <form className="filters" onSubmit={handleSearch} role="search">
          <label className="sr-only" htmlFor="subject-search">Buscar materia</label>
          <input id="subject-search" type="search" placeholder="Buscar por nombre" value={searchInput} onChange={event => setSearchInput(event.target.value)} />
          <button className="secondary-button" type="submit">Buscar</button>
          <label className="sr-only" htmlFor="subject-status">Filtrar por estado</label>
          <select id="subject-status" value={status} onChange={event => handleStatusChange(event.target.value)}>
            <option value="ALL">Todos los estados</option><option value="ACTIVE">Activas</option><option value="INACTIVE">Inactivas</option>
          </select>
        </form></div>
      {loading ? <div className="list-message" role="status">Cargando materias…</div> :
        error ? <div className="list-message" role="alert"><p>{error}</p><button className="secondary-button" type="button" onClick={refresh}>Reintentar</button></div> :
        subjects.length === 0 ? <div className="list-message"><h3>No se encontraron materias</h3><p>Probá con otro nombre o estado, o creá una nueva.</p></div> :
        <div className="table-scroll"><table><thead><tr><th scope="col">Materia</th><th scope="col">Descripción</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
          <tbody>{subjects.map(subject => <tr key={subject.id}><td><strong>{subject.name}</strong></td>
            <td>{subject.description || <span className="muted">Sin descripción</span>}</td>
            <td><span className={`status-badge ${subject.status === 'ACTIVE' ? 'active' : 'inactive'}`}>{subject.status === 'ACTIVE' ? 'Activa' : 'Inactiva'}</span></td>
            <td><button className="text-button" type="button" onClick={() => setSelection({ kind: 'subject', id: subject.id })} aria-label={`Ver detalle de ${subject.name}`}>Ver detalle</button></td>
          </tr>)}</tbody></table></div>}
    </section>
  </main>
}
