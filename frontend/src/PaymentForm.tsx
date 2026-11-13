import { useState, type FormEvent } from 'react'
import { ApiError, type PaymentInput } from './api'
import { describeApiError, formatMoney, localToday } from './ui'

export default function PaymentForm({ balance, onCreate, onLogout }: {
  balance: number
  onCreate: (input: PaymentInput) => Promise<void>
  onLogout: () => void
}) {
  const [amount, setAmount] = useState('')
  const [paidAt, setPaidAt] = useState(localToday)
  const [method, setMethod] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = Number(amount)
    if (!Number.isFinite(value) || value <= 0 || value > balance) {
      setError(`Ingresá un importe mayor que cero y no superior a ${formatMoney(balance)}.`)
      return
    }
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      await onCreate({ amount: value, paidAt, method: method ? method as PaymentInput['method'] : null, notes: notes.trim() || null })
      setAmount('')
      setNotes('')
      setSuccess('Pago registrado.')
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
      setError(describeApiError(caught))
    } finally {
      setSaving(false)
    }
  }

  return <form className="entity-form payment-form" onSubmit={submit}>
    <div className="field"><label htmlFor="payment-amount">Importe (ARS) *</label><input id="payment-amount" type="number" min="0.01" max={balance} step="0.01" required value={amount} onChange={event => setAmount(event.target.value)} /></div>
    <div className="field"><label htmlFor="payment-date">Fecha de pago *</label><input id="payment-date" type="date" required value={paidAt} onChange={event => setPaidAt(event.target.value)} /></div>
    <div className="field"><label htmlFor="payment-method">Medio</label><select id="payment-method" value={method} onChange={event => setMethod(event.target.value)}><option value="">Sin especificar</option><option value="CASH">Efectivo</option><option value="TRANSFER">Transferencia</option></select></div>
    <div className="field field-wide"><label htmlFor="payment-notes">Notas</label><textarea id="payment-notes" rows={2} maxLength={1000} value={notes} onChange={event => setNotes(event.target.value)} /></div>
    {error && <p className="form-error field-wide" role="alert">{error}</p>}
    {success && <p className="form-success field-wide" role="status">{success}</p>}
    <div className="panel-actions"><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Registrando…' : 'Registrar pago'}</button></div>
  </form>
}
