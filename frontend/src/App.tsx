import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ApiError, getStudents, login, type Session, type Student } from './api'
import StudentPanel from './StudentPanel'
import SubjectsPage from './SubjectsPage'
import ClassesPage from './ClassesPage'
import './App.css'

function App() {
  const [session, setSession] = useState<Session | null>(null)
  const handleLogout = useCallback(() => setSession(null), [])
  return session
    ? <Workspace session={session} onLogout={handleLogout} />
    : <LoginPage onLogin={setSession} />
}

function Workspace({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [page, setPage] = useState<'students' | 'subjects' | 'classes'>('students')
  return <div className="app-shell">
    <header className="app-header"><div className="header-inner"><Brand />
      <nav aria-label="Navegación principal">
        <button type="button" aria-current={page === 'students' ? 'page' : undefined} onClick={() => setPage('students')}>Alumnos</button>
        <button type="button" aria-current={page === 'subjects' ? 'page' : undefined} onClick={() => setPage('subjects')}>Materias</button>
        <button type="button" aria-current={page === 'classes' ? 'page' : undefined} onClick={() => setPage('classes')}>Clases</button>
      </nav>
      <div className="account"><div><strong>{session.user.name}</strong><small>{session.user.role === 'ADMIN' ? 'Administrador' : 'Profesor'}</small></div>
        <button className="text-button" type="button" onClick={onLogout}>Salir</button></div>
    </div></header>
    {page === 'students' ? <StudentsPage session={session} onLogout={onLogout} /> :
      page === 'subjects' ? <SubjectsPage session={session} onLogout={onLogout} /> :
        <ClassesPage session={session} onLogout={onLogout} />}
  </div>
}

function LoginPage({ onLogin }: { onLogin: (session: Session) => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      onLogin(await login(username.trim(), password))
    } catch (caught) {
      setError(caught instanceof ApiError && caught.status === 401
        ? 'Usuario o contraseña incorrectos.'
        : 'No se pudo iniciar sesión. Revisá la conexión e intentá de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  return <main className="login-page"><div className="login-layout">
    <section className="login-intro" aria-labelledby="welcome-title">
      <Brand />
      <div><span className="eyebrow">Gestión de academia</span><h1 id="welcome-title">Un lugar para organizar cada clase.</h1>
        <p>Consultá tus alumnos y mantené la información de la academia a mano.</p></div>
      <small>Simple para el día a día.</small>
    </section>
    <section className="login-panel" aria-labelledby="login-title"><div className="login-card">
      <span className="eyebrow">Bienvenido de nuevo</span><h2 id="login-title">Iniciar sesión</h2>
      <p className="muted">Ingresá con tu usuario de administrador o profesor.</p>
      <form onSubmit={handleSubmit} className="form-stack">
        <div className="field"><label htmlFor="username">Usuario</label><input id="username" autoComplete="username" required value={username} onChange={e => setUsername(e.target.value)} /></div>
        <div className="field"><label htmlFor="password">Contraseña</label><input id="password" type="password" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} /></div>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button" type="submit" disabled={loading}>{loading ? 'Ingresando…' : 'Ingresar'}</button>
      </form>
    </div></section>
  </div></main>
}

function StudentsPage({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [students, setStudents] = useState<Student[]>([])
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [selection, setSelection] = useState<{ kind: 'new' } | { kind: 'student'; id: string } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getStudents(session.accessToken, search, status, controller.signal)
      .then(setStudents)
      .catch((caught: unknown) => {
        if (controller.signal.aborted) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError('No se pudieron cargar los alumnos. Intentá de nuevo.')
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

  function retry() {
    setLoading(true)
    setError('')
    setReloadKey(key => key + 1)
  }

  function refreshAfterSave() {
    setLoading(true)
    setError('')
    setReloadKey(key => key + 1)
  }

  return <main className="content">
      <div className="page-heading"><div><span className="eyebrow">Directorio</span><h1>Alumnos</h1><p className="muted">Consultá y administrá la información de cada alumno.</p></div>
        <div className="heading-actions"><button className="primary-button" type="button" onClick={() => setSelection({ kind: 'new' })}>Nuevo alumno</button>
          <div className="count-card"><span>Total mostrado</span><strong>{loading || error ? '—' : students.length}</strong></div></div></div>
      {selection && <StudentPanel key={selection.kind === 'new' ? 'new' : selection.id} token={session.accessToken}
        studentId={selection.kind === 'student' ? selection.id : null} onClose={() => setSelection(null)}
        onLogout={onLogout} onSaved={refreshAfterSave} />}
      <section className="list-card" aria-labelledby="list-title">
        <div className="list-toolbar"><div><h2 id="list-title">Listado de alumnos</h2><p className="muted">Buscá por nombre o filtrá por estado.</p></div>
          <form className="filters" onSubmit={handleSearch} role="search">
            <label className="sr-only" htmlFor="student-search">Buscar alumno</label>
            <input id="student-search" type="search" placeholder="Buscar por nombre" value={searchInput} onChange={e => setSearchInput(e.target.value)} />
            <button className="secondary-button" type="submit">Buscar</button>
            <label className="sr-only" htmlFor="student-status">Filtrar por estado</label>
            <select id="student-status" value={status} onChange={e => handleStatusChange(e.target.value)}>
              <option value="ALL">Todos los estados</option><option value="ACTIVE">Activos</option><option value="INACTIVE">Inactivos</option>
            </select>
          </form></div>
        {loading ? <div className="list-message" role="status">Cargando alumnos…</div> :
          error ? <div className="list-message" role="alert"><p>{error}</p><button className="secondary-button" type="button" onClick={retry}>Reintentar</button></div> :
          students.length === 0 ? <div className="list-message"><h3>No se encontraron alumnos</h3><p>Probá con otro nombre o estado, o creá uno nuevo.</p></div> :
          <div className="table-scroll"><table><thead><tr><th scope="col">Alumno</th><th scope="col">Contacto</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
            <tbody>{students.map(student => <tr key={student.id}><td><strong>{student.name}</strong></td>
              <td>{student.contactNumber || <span className="muted">Sin número</span>}</td>
              <td><span className={`status-badge ${student.status === 'ACTIVE' ? 'active' : 'inactive'}`}>{student.status === 'ACTIVE' ? 'Activo' : 'Inactivo'}</span></td>
              <td><button className="text-button" type="button" onClick={() => setSelection({ kind: 'student', id: student.id })} aria-label={`Ver detalle de ${student.name}`}>Ver detalle</button></td>
            </tr>)}</tbody></table></div>}
      </section>
  </main>
}

function Brand() {
  return <div className="brand"><span className="brand-mark" aria-hidden="true">A</span><span>Academia<span className="brand-dot">.</span></span></div>
}

export default App
