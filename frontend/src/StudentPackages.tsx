import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, createPackage, createPackagePayment, getPackage, getPackagePayments, getPackages, type ClassPackage, type Payment } from './api'
import PaymentForm from './PaymentForm'
import { describeApiError, formatDate, formatMoney, localToday } from './ui'

export default function StudentPackages({ token, studentId, onLogout }: { token: string; studentId: string; onLogout: () => void }) {
  const [packages, setPackages] = useState<ClassPackage[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [selected, setSelected] = useState<ClassPackage | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [totalClasses, setTotalClasses] = useState('')
  const [price, setPrice] = useState('')
  const [purchasedAt, setPurchasedAt] = useState(localToday)
  const [showCreate, setShowCreate] = useState(false)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [error, setError] = useState('')
  const [detailsError, setDetailsError] = useState('')
  const [formError, setFormError] = useState('')
  const [notice, setNotice] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    getPackages(token, studentId)
      .then(result => { if (active) setPackages(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError(describeApiError(caught))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [token, studentId, reloadKey, onLogout])

  useEffect(() => {
    if (!selectedId) return
    let active = true
    Promise.all([getPackage(token, selectedId), getPackagePayments(token, selectedId)])
      .then(([item, history]) => { if (active) { setSelected(item); setPayments(history) } })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setDetailsError(describeApiError(caught))
      })
      .finally(() => { if (active) setDetailsLoading(false) })
    return () => { active = false }
  }, [token, selectedId, reloadKey, onLogout])

  function selectPackage(id: string) {
    if (id === selectedId) return
    setSelectedId(id)
    setSelected(null)
    setPayments([])
    setDetailsError('')
    setDetailsLoading(true)
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const count = Number(totalClasses)
    const value = Number(price)
    if (!Number.isInteger(count) || count < 1 || !Number.isFinite(value) || value < 0) {
      setFormError('Ingresá una cantidad positiva y un precio válido.')
      return
    }
    setSaving(true)
    setFormError('')
    setNotice('')
    try {
      const result = await createPackage(token, studentId, { totalClasses: count, price: value, purchasedAt })
      setShowCreate(false)
      setTotalClasses('')
      setPrice('')
      setNotice('Paquete creado.')
      setLoading(true)
      setReloadKey(key => key + 1)
      selectPackage(result.id)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setFormError(describeApiError(caught))
    } finally {
      setSaving(false)
    }
  }

  async function addPayment(input: Parameters<typeof createPackagePayment>[2]) {
    if (!selectedId) return
    await createPackagePayment(token, selectedId, input)
    setLoading(true)
    setDetailsLoading(true)
    setReloadKey(key => key + 1)
  }

  return <section className="related-section" aria-labelledby="packages-title">
    <div className="related-heading"><div><h3 id="packages-title">Paquetes y pagos</h3><p className="muted">Clases disponibles y saldos del alumno.</p></div>
      <button className="secondary-button" type="button" onClick={() => { setShowCreate(!showCreate); setFormError('') }}>{showCreate ? 'Cerrar formulario' : 'Nuevo paquete'}</button></div>
    {notice && <p className="form-success" role="status">{notice}</p>}
    {showCreate && <form className="entity-form inner-form" onSubmit={create}>
      <div className="field"><label htmlFor="package-classes">Cantidad de clases *</label><input id="package-classes" type="number" min="1" step="1" required value={totalClasses} onChange={event => setTotalClasses(event.target.value)} /></div>
      <div className="field"><label htmlFor="package-price">Precio (ARS) *</label><input id="package-price" type="number" min="0" step="0.01" required value={price} onChange={event => setPrice(event.target.value)} /></div>
      <div className="field"><label htmlFor="package-date">Fecha de compra *</label><input id="package-date" type="date" required value={purchasedAt} onChange={event => setPurchasedAt(event.target.value)} /></div>
      {formError && <p className="form-error field-wide" role="alert">{formError}</p>}
      <div className="panel-actions"><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Guardando…' : 'Crear paquete'}</button></div>
    </form>}
    {loading ? <p role="status">Cargando paquetes…</p> : error ? <p className="form-error" role="alert">{error} <button className="text-button" type="button" onClick={() => { setError(''); setLoading(true); setReloadKey(key => key + 1) }}>Reintentar</button></p> : packages.length === 0 ? <p className="muted">Este alumno todavía no tiene paquetes.</p> :
      <div className="table-scroll"><table><thead><tr><th>Compra</th><th>Clases restantes</th><th>Precio</th><th>Saldo pendiente</th><th>Acciones</th></tr></thead><tbody>{packages.map(item => <tr key={item.id}><td>{formatDate(item.purchasedAt)}</td><td>{item.remainingClasses} de {item.totalClasses}</td><td>{formatMoney(item.price)}</td><td>{formatMoney(item.outstandingAmount)}</td><td><button className="text-button" type="button" onClick={() => selectPackage(item.id)}>Ver pagos</button></td></tr>)}</tbody></table></div>}
    {selectedId && <div className="related-detail"><div className="related-heading"><h4>Detalle del paquete</h4><button className="text-button" type="button" onClick={() => { setSelectedId(null); setSelected(null) }}>Cerrar</button></div>
      {detailsLoading ? <p role="status">Cargando pagos…</p> : detailsError ? <p className="form-error" role="alert">{detailsError} <button className="text-button" type="button" onClick={() => { setDetailsError(''); setDetailsLoading(true); setReloadKey(key => key + 1) }}>Reintentar</button></p> : selected && <>
        <p>Clases usadas: {selected.usedClasses} · Restantes: {selected.remainingClasses} · Abonado: {formatMoney(selected.paidAmount)} · Saldo: {formatMoney(selected.outstandingAmount)}</p>
        <h5>Pagos registrados</h5>{payments.length === 0 ? <p className="muted">Todavía no hay pagos.</p> : <ul className="record-list">{payments.map(payment => <li key={payment.id}>{formatDate(payment.paidAt)} · {formatMoney(payment.amount)} · {payment.method === 'CASH' ? 'Efectivo' : payment.method === 'TRANSFER' ? 'Transferencia' : 'Sin medio'}{payment.notes ? ` · ${payment.notes}` : ''}</li>)}</ul>}
        {selected.outstandingAmount > 0 && <><h5>Registrar pago</h5><PaymentForm key={selected.id} balance={selected.outstandingAmount} onCreate={addPayment} onLogout={onLogout} /></>}
      </>}
    </div>}
  </section>
}
