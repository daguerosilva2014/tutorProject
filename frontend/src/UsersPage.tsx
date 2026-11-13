import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, createUser, getUsers, updateUser, type ManagedUser, type Session } from './api'
import { describeApiError } from './ui'

type UserForm = { name: string; username: string; password: string; role: ManagedUser['role']; enabled: boolean }
const emptyForm: UserForm = { name: '', username: '', password: '', role: 'TEACHER', enabled: true }

export default function UsersPage({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [users, setUsers] = useState<ManagedUser[]>([])
  const [selected, setSelected] = useState<ManagedUser | 'new' | null>(null)
  const [form, setForm] = useState<UserForm>(emptyForm)
  const [roleFilter, setRoleFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')
  const [notice, setNotice] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    getUsers(session.accessToken)
      .then(result => { if (active) setUsers(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError(describeApiError(caught))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [session.accessToken, reloadKey, onLogout])

  function openNew() {
    setSelected('new')
    setForm(emptyForm)
    setFormError('')
    setNotice('')
  }

  function openEdit(user: ManagedUser) {
    setSelected(user)
    setForm({ name: user.name, username: user.username, password: '', role: user.role, enabled: user.enabled })
    setFormError('')
    setNotice('')
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selected) return
    setSaving(true)
    setFormError('')
    try {
      if (selected === 'new') {
        await createUser(session.accessToken, { name: form.name.trim(), username: form.username.trim(), password: form.password, role: form.role })
        setNotice('Usuario creado. La contraseña inicial ya no puede consultarse desde esta pantalla.')
      } else {
        await updateUser(session.accessToken, selected.id, { name: form.name.trim(), role: form.role, enabled: form.enabled })
        setNotice('Usuario actualizado.')
      }
      setSelected(null)
      setForm(emptyForm)
      setError('')
      setLoading(true)
      setReloadKey(key => key + 1)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setFormError(describeApiError(caught))
    } finally {
      setSaving(false)
    }
  }

  const visible = users.filter(user => (roleFilter === 'ALL' || user.role === roleFilter) &&
    (statusFilter === 'ALL' || user.enabled === (statusFilter === 'ACTIVE')))
  const editingSelf = selected !== null && selected !== 'new' && selected.id === session.user.id

  return <main className="content">
    <div className="page-heading"><div><span className="eyebrow">Administración</span><h1>Usuarios</h1><p className="muted">Creá profesores y administrá quién puede ingresar.</p></div>
      <div className="heading-actions"><button className="primary-button" type="button" onClick={openNew}>Nuevo usuario</button>
        <div className="count-card"><span>Total mostrado</span><strong>{loading || error ? '—' : visible.length}</strong></div></div></div>
    {notice && <p className="form-success page-notice" role="status">{notice}</p>}
    {selected && <section className="entity-panel" aria-labelledby="user-panel-title">
      <div className="panel-heading"><div><span className="eyebrow">Administración de usuarios</span><h2 id="user-panel-title">{selected === 'new' ? 'Nuevo usuario' : 'Editar usuario'}</h2></div>
        <button className="text-button" type="button" onClick={() => setSelected(null)}>Cerrar</button></div>
      {formError && <p className="form-error" role="alert">{formError}</p>}
      <form className="entity-form" onSubmit={save}>
        <div className="field"><label htmlFor="user-name">Nombre *</label><input id="user-name" required maxLength={120} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></div>
        <div className="field"><label htmlFor="user-username">Usuario *</label><input id="user-username" required maxLength={80} autoComplete="username" disabled={selected !== 'new'} value={form.username} onChange={event => setForm({ ...form, username: event.target.value })} /></div>
        {selected === 'new' && <div className="field"><label htmlFor="user-password">Contraseña inicial *</label><input id="user-password" type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={form.password} onChange={event => setForm({ ...form, password: event.target.value })} /><small>Al menos 8 caracteres. Guardala antes de crear el usuario.</small></div>}
        <div className="field"><label htmlFor="user-role">Rol *</label><select id="user-role" value={form.role} disabled={editingSelf} onChange={event => setForm({ ...form, role: event.target.value as ManagedUser['role'] })}><option value="TEACHER">Profesor</option><option value="ADMIN">Administrador</option></select></div>
        {selected !== 'new' && <div className="field field-wide"><label className="checkbox-label" htmlFor="user-enabled"><input id="user-enabled" type="checkbox" checked={form.enabled} disabled={editingSelf} onChange={event => setForm({ ...form, enabled: event.target.checked })} />Usuario habilitado</label></div>}
        {editingSelf && <p className="muted field-wide">Podés cambiar tu nombre, pero no deshabilitar tu propia cuenta ni cambiar tu rol desde esta pantalla.</p>}
        <div className="panel-actions"><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : selected === 'new' ? 'Crear usuario' : 'Guardar cambios'}</button>
          <button className="secondary-button" type="button" disabled={saving} onClick={() => setSelected(null)}>Cancelar</button></div>
      </form>
    </section>}
    <section className="list-card" aria-labelledby="users-list-title"><div className="list-toolbar"><div><h2 id="users-list-title">Listado de usuarios</h2><p className="muted">La contraseña no se muestra después de crear la cuenta.</p></div>
      <div className="filters"><label className="sr-only" htmlFor="users-role-filter">Filtrar por rol</label><select id="users-role-filter" value={roleFilter} onChange={event => setRoleFilter(event.target.value)}><option value="ALL">Todos los roles</option><option value="ADMIN">Administradores</option><option value="TEACHER">Profesores</option></select>
        <label className="sr-only" htmlFor="users-status-filter">Filtrar por estado</label><select id="users-status-filter" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="ALL">Todos los estados</option><option value="ACTIVE">Habilitados</option><option value="INACTIVE">Deshabilitados</option></select></div></div>
      {loading ? <div className="list-message" role="status">Cargando usuarios…</div> : error ? <div className="list-message" role="alert"><p>{error}</p><button className="secondary-button" type="button" onClick={() => { setError(''); setLoading(true); setReloadKey(key => key + 1) }}>Reintentar</button></div> : visible.length === 0 ? <div className="list-message">No se encontraron usuarios.</div> :
        <div className="table-scroll"><table><thead><tr><th>Nombre</th><th>Usuario</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{visible.map(user => <tr key={user.id}><td><strong>{user.name}</strong></td><td>{user.username}</td><td>{user.role === 'ADMIN' ? 'Administrador' : 'Profesor'}</td><td><span className={`status-badge ${user.enabled ? 'active' : 'inactive'}`}>{user.enabled ? 'Habilitado' : 'Deshabilitado'}</span></td><td><button className="text-button" type="button" onClick={() => openEdit(user)} aria-label={`Editar a ${user.name}`}>Editar</button></td></tr>)}</tbody></table></div>}
    </section>
  </main>
}
