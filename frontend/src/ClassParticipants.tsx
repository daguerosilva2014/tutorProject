import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, addParticipant, assignRecovery, createParticipationPayment, getClasses, getPackages, getParticipants, getParticipationPayments, getStudents, getSubjects, recordAttendance, replaceParticipationSubjects, type AcademyClass, type Participation, type Payment, type PaymentInput, type Student, type Subject } from './api'
import PaymentForm from './PaymentForm'
import { describeApiError, formatDate, formatMoney } from './ui'

function SubjectChoices({ idPrefix, choices, selected, onChange }: {
  idPrefix: string
  choices: { id: string; name: string }[]
  selected: string[]
  onChange: (ids: string[]) => void
}) {
  return <fieldset className="choice-fieldset"><legend>Materias opcionales</legend>
    {choices.length === 0 ? <p className="muted">No hay materias activas.</p> : <div className="choice-grid">{choices.map(subject => <label key={subject.id} htmlFor={`${idPrefix}-${subject.id}`} className="checkbox-label">
      <input id={`${idPrefix}-${subject.id}`} type="checkbox" checked={selected.includes(subject.id)} onChange={event => onChange(event.target.checked ? [...selected, subject.id] : selected.filter(id => id !== subject.id))} />{subject.name}
    </label>)}</div>}
  </fieldset>
}

export default function ClassParticipants({ token, academyClass, onLogout }: { token: string; academyClass: AcademyClass; onLogout: () => void }) {
  const [participants, setParticipants] = useState<Participation[]>([])
  const [students, setStudents] = useState<Student[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [loading, setLoading] = useState(true)
  const [choicesLoading, setChoicesLoading] = useState(academyClass.status === 'SCHEDULED')
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [studentId, setStudentId] = useState('')
  const [billingMode, setBillingMode] = useState<'PACKAGE' | 'PER_CLASS'>('PACKAGE')
  const [unitPrice, setUnitPrice] = useState('')
  const [subjectIds, setSubjectIds] = useState<string[]>([])
  const [packageBalance, setPackageBalance] = useState<number | null>(null)
  const [packageError, setPackageError] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [editSubjects, setEditSubjects] = useState(false)
  const [editSubjectIds, setEditSubjectIds] = useState<string[]>([])
  const [confirmMark, setConfirmMark] = useState<'PRESENT' | 'ABSENT' | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [paymentsLoading, setPaymentsLoading] = useState(false)
  const [recoveryClasses, setRecoveryClasses] = useState<AcademyClass[]>([])
  const [recoveryLoading, setRecoveryLoading] = useState(false)
  const [showRecovery, setShowRecovery] = useState(false)
  const [targetClassId, setTargetClassId] = useState('')

  useEffect(() => {
    let active = true
    getParticipants(token, academyClass.id)
      .then(result => { if (active) setParticipants(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setError(describeApiError(caught))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [token, academyClass.id, reloadKey, onLogout])

  useEffect(() => {
    if (academyClass.status !== 'SCHEDULED') return
    const controller = new AbortController()
    Promise.all([getStudents(token, '', 'ACTIVE', controller.signal), getSubjects(token, '', 'ACTIVE', controller.signal)])
      .then(([studentList, subjectList]) => { if (!controller.signal.aborted) { setStudents(studentList); setSubjects(subjectList) } })
      .catch(caught => {
        if (controller.signal.aborted) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setActionError('No se pudieron cargar alumnos o materias. Cerrá el detalle e intentá de nuevo.')
      })
      .finally(() => { if (!controller.signal.aborted) setChoicesLoading(false) })
    return () => controller.abort()
  }, [token, academyClass.status, onLogout])

  useEffect(() => {
    if (!studentId || billingMode !== 'PACKAGE') return
    let active = true
    getPackages(token, studentId)
      .then(items => { if (active) setPackageBalance(items.reduce((total, item) => total + item.remainingClasses, 0)) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setPackageError(describeApiError(caught))
      })
    return () => { active = false }
  }, [token, studentId, billingMode, onLogout])

  useEffect(() => {
    const selected = participants.find(item => item.id === selectedId)
    if (!selected || selected.billingMode !== 'PER_CLASS' || academyClass.status !== 'FINALIZED') return
    let active = true
    getParticipationPayments(token, selected.id)
      .then(result => { if (active) setPayments(result) })
      .catch(caught => {
        if (!active) return
        if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
        setActionError(describeApiError(caught))
      })
      .finally(() => { if (active) setPaymentsLoading(false) })
    return () => { active = false }
  }, [token, selectedId, participants, academyClass.status, reloadKey, onLogout])

  function refresh() {
    setLoading(true)
    setError('')
    setReloadKey(key => key + 1)
  }

  function chooseStudent(id: string) {
    setStudentId(id)
    setPackageBalance(null)
    setPackageError('')
  }

  function chooseBillingMode(mode: 'PACKAGE' | 'PER_CLASS') {
    setBillingMode(mode)
    setPackageBalance(null)
    setPackageError('')
  }

  function handleActionError(caught: unknown) {
    if (caught instanceof ApiError && caught.status === 401) { onLogout(); return }
    setActionError(describeApiError(caught))
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!studentId) { setActionError('Seleccioná un alumno.'); return }
    if (billingMode === 'PACKAGE' && packageBalance !== null && packageBalance < 1) { setActionError('El alumno necesita un paquete con clases disponibles.'); return }
    const price = Number(unitPrice)
    if (billingMode === 'PER_CLASS' && (!Number.isFinite(price) || price < 0 || unitPrice === '')) { setActionError('Ingresá un precio individual válido.'); return }
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      await addParticipant(token, academyClass.id, { studentId, billingMode, ...(billingMode === 'PER_CLASS' ? { unitPrice: price } : {}), subjectIds })
      setShowAdd(false)
      chooseStudent('')
      setSubjectIds([])
      setUnitPrice('')
      setNotice('Alumno asignado a la clase.')
      refresh()
    } catch (caught) { handleActionError(caught) }
    finally { setBusy(false) }
  }

  async function saveSubjects() {
    if (!selectedId) return
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      await replaceParticipationSubjects(token, selectedId, editSubjectIds)
      setEditSubjects(false)
      setNotice('Materias actualizadas.')
      refresh()
    } catch (caught) { handleActionError(caught) }
    finally { setBusy(false) }
  }

  async function markAttendance() {
    if (!selectedId || !confirmMark) return
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      const result = await recordAttendance(token, selectedId, confirmMark)
      setConfirmMark(null)
      setNotice(result.attendanceStatus === 'JUSTIFIED_ABSENCE' ? 'Ausencia justificada registrada.' : 'Asistencia registrada.')
      refresh()
    } catch (caught) { handleActionError(caught) }
    finally { setBusy(false) }
  }

  async function openRecovery() {
    setRecoveryLoading(true)
    setActionError('')
    setRecoveryClasses([])
    setTargetClassId('')
    setShowRecovery(true)
    try {
      const candidates = await getClasses(token, { from: academyClass.date, to: '', teacherId: '', status: 'SCHEDULED' }, new AbortController().signal)
      setRecoveryClasses(candidates.filter(item => item.id !== academyClass.id))
    } catch (caught) { handleActionError(caught) }
    finally { setRecoveryLoading(false) }
  }

  async function saveRecovery(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedId || !targetClassId) return
    setBusy(true)
    setActionError('')
    setNotice('')
    try {
      await assignRecovery(token, selectedId, targetClassId)
      setShowRecovery(false)
      setNotice('Recuperación asignada a la clase elegida.')
      refresh()
    } catch (caught) { handleActionError(caught) }
    finally { setBusy(false) }
  }

  async function addPayment(input: PaymentInput) {
    if (!selectedId) return
    await createParticipationPayment(token, selectedId, input)
    setPaymentsLoading(true)
    refresh()
  }

  const selected = participants.find(item => item.id === selectedId)
  const subjectOptions = selected ? [...subjects, ...selected.subjects.filter(item => !subjects.some(active => active.id === item.id))] : subjects
  const canRecover = selected && academyClass.status === 'FINALIZED' && selected.attendanceStatus === 'JUSTIFIED_ABSENCE' && selected.consumptionStatus === 'NOT_CONSUMED' && !selected.recoveryTargetId

  return <section className="related-section" aria-labelledby="participants-title">
    <div className="related-heading"><div><h3 id="participants-title">Participantes</h3><p className="muted">Alumnos, materias, asistencia y cobros de esta clase.</p></div>
      {academyClass.status === 'SCHEDULED' && <button className="secondary-button" type="button" onClick={() => { setShowAdd(!showAdd); setActionError('') }}>{showAdd ? 'Cerrar formulario' : 'Asignar alumno'}</button>}</div>
    {notice && <p className="form-success" role="status">{notice}</p>}
    {actionError && <p className="form-error" role="alert">{actionError}</p>}
    {showAdd && academyClass.status === 'SCHEDULED' && <form className="entity-form inner-form" onSubmit={create}>
      {choicesLoading ? <p role="status">Cargando alumnos y materias…</p> : <>
        <div className="field"><label htmlFor="participation-student">Alumno *</label><select id="participation-student" required value={studentId} onChange={event => chooseStudent(event.target.value)}><option value="">Seleccionar alumno</option>{students.filter(student => !participants.some(item => item.student.id === student.id)).map(student => <option key={student.id} value={student.id}>{student.name}</option>)}</select></div>
        <div className="field"><label htmlFor="participation-mode">Modalidad de cobro *</label><select id="participation-mode" value={billingMode} onChange={event => chooseBillingMode(event.target.value as 'PACKAGE' | 'PER_CLASS')}><option value="PACKAGE">Paquete</option><option value="PER_CLASS">Clase individual</option></select></div>
        {billingMode === 'PACKAGE' && studentId && <p className="field-wide muted" role="status">{packageError || (packageBalance === null ? 'Consultando paquetes…' : packageBalance > 0 ? `Clases disponibles en paquetes: ${packageBalance}` : 'El alumno no tiene clases disponibles. Creá un paquete desde su detalle antes de asignarlo.')}</p>}
        {billingMode === 'PER_CLASS' && <div className="field"><label htmlFor="participation-price">Precio de la clase (ARS) *</label><input id="participation-price" type="number" min="0" step="0.01" required value={unitPrice} onChange={event => setUnitPrice(event.target.value)} /></div>}
        <SubjectChoices idPrefix="add-subject" choices={subjects} selected={subjectIds} onChange={setSubjectIds} />
        <div className="panel-actions"><button className="primary-button" type="submit" disabled={busy || (billingMode === 'PACKAGE' && studentId !== '' && (packageBalance === null || packageBalance < 1))}>{busy ? 'Asignando…' : 'Asignar alumno'}</button></div>
      </>}
    </form>}
    {loading ? <p role="status">Cargando participantes…</p> : error ? <p className="form-error" role="alert">{error} <button className="text-button" type="button" onClick={refresh}>Reintentar</button></p> : participants.length === 0 ? <p className="muted">Todavía no hay alumnos asignados.</p> :
      <div className="table-scroll"><table><thead><tr><th>Alumno</th><th>Cobro</th><th>Asistencia</th><th>Saldo</th><th>Acciones</th></tr></thead><tbody>{participants.map(item => <tr key={item.id}><td><strong>{item.student.name}</strong></td><td>{item.billingMode === 'PACKAGE' ? 'Paquete' : `Individual · ${formatMoney(item.unitPrice ?? 0)}`}</td><td>{item.attendanceStatus === 'PENDING' ? 'Pendiente' : item.attendanceStatus === 'PRESENT' ? 'Presente' : item.attendanceStatus === 'JUSTIFIED_ABSENCE' ? 'Ausencia justificada' : 'Ausente'}</td><td>{item.billingMode === 'PER_CLASS' ? formatMoney(item.outstandingAmount) : '—'}</td><td><button className="text-button" type="button" onClick={() => { if (selectedId === item.id) return; setSelectedId(item.id); setEditSubjects(false); setConfirmMark(null); setShowRecovery(false); setActionError(''); setPayments([]); setPaymentsLoading(item.billingMode === 'PER_CLASS' && academyClass.status === 'FINALIZED') }}>Ver detalle</button></td></tr>)}</tbody></table></div>}
    {selected && <div className="related-detail"><div className="related-heading"><h4>{selected.student.name}</h4><button className="text-button" type="button" onClick={() => setSelectedId(null)}>Cerrar</button></div>
      <p>Materias: {selected.subjects.length ? selected.subjects.map(item => item.name).join(', ') : 'Ninguna'} · Cobro: {selected.billingMode === 'PACKAGE' ? 'Paquete' : `Individual (${formatMoney(selected.unitPrice ?? 0)})`}</p>
      <p>Asistencia: {selected.attendanceStatus === 'JUSTIFIED_ABSENCE' ? 'Ausencia justificada' : selected.attendanceStatus === 'PENDING' ? 'Pendiente' : selected.attendanceStatus === 'PRESENT' ? 'Presente' : 'Ausente'} · Consumo: {selected.consumptionStatus}</p>
      {selected.recoveryTargetId && <p className="form-success">Recuperación asignada.</p>}
      {academyClass.status === 'SCHEDULED' && <><div className="panel-actions"><button className="secondary-button" type="button" onClick={() => { setEditSubjects(!editSubjects); setEditSubjectIds(selected.subjects.map(item => item.id)) }}>Editar materias</button>
        {selected.attendanceStatus === 'PENDING' && <><button className="secondary-button" type="button" onClick={() => setConfirmMark('PRESENT')}>Marcar presente</button><button className="secondary-button" type="button" onClick={() => setConfirmMark('ABSENT')}>Avisar ausencia</button></>}</div>
        {editSubjects && <div className="inner-form"><SubjectChoices idPrefix="edit-subject" choices={subjectOptions} selected={editSubjectIds} onChange={setEditSubjectIds} /><div className="panel-actions"><button className="primary-button" type="button" disabled={busy} onClick={saveSubjects}>Guardar materias</button><button className="secondary-button" type="button" onClick={() => setEditSubjects(false)}>Cancelar</button></div></div>}
        {confirmMark && <div className="status-confirm"><p>¿Confirmás {confirmMark === 'PRESENT' ? 'la presencia' : 'el aviso de ausencia'} de {selected.student.name}? Esta marca no se puede cambiar luego.</p><div className="panel-actions"><button className="primary-button" type="button" disabled={busy} onClick={markAttendance}>{busy ? 'Guardando…' : 'Confirmar'}</button><button className="secondary-button" type="button" onClick={() => setConfirmMark(null)}>Volver</button></div></div>}
      </>}
      {academyClass.status === 'FINALIZED' && selected.billingMode === 'PER_CLASS' && <div className="related-detail"><h5>Pagos de la clase</h5>{paymentsLoading ? <p role="status">Cargando pagos…</p> : payments.length === 0 ? <p className="muted">Todavía no hay pagos.</p> : <ul className="record-list">{payments.map(payment => <li key={payment.id}>{formatDate(payment.paidAt)} · {formatMoney(payment.amount)}{payment.method ? ` · ${payment.method === 'CASH' ? 'Efectivo' : 'Transferencia'}` : ''}{payment.notes ? ` · ${payment.notes}` : ''}</li>)}</ul>}
        <p>Abonado: {formatMoney(selected.paidAmount)} · Saldo: {formatMoney(selected.outstandingAmount)}</p>
        {selected.outstandingAmount > 0 && <PaymentForm key={selected.id} balance={selected.outstandingAmount} onCreate={addPayment} onLogout={onLogout} />}
      </div>}
      {canRecover && <><button className="secondary-button" type="button" onClick={openRecovery}>Asignar recuperación</button>
        {showRecovery && <form className="entity-form inner-form" onSubmit={saveRecovery}><div className="field field-wide"><label htmlFor="recovery-target">Clase destino *</label><select id="recovery-target" required value={targetClassId} onChange={event => setTargetClassId(event.target.value)} disabled={recoveryLoading}><option value="">Seleccionar clase</option>{recoveryClasses.map(item => <option key={item.id} value={item.id}>{formatDate(item.date)} · {item.startTime.slice(0, 5)} · {item.teacher.name}</option>)}</select></div>
          {recoveryLoading ? <p role="status">Buscando clases programadas…</p> : recoveryClasses.length === 0 ? <p className="muted">No hay clases destino disponibles.</p> : null}
          <div className="panel-actions"><button className="primary-button" type="submit" disabled={busy || recoveryLoading || recoveryClasses.length === 0}>{busy ? 'Asignando…' : 'Confirmar recuperación'}</button><button className="secondary-button" type="button" onClick={() => setShowRecovery(false)}>Cancelar</button></div></form>}
      </>}
    </div>}
  </section>
}
