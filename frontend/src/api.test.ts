import { afterEach, describe, expect, it, vi } from 'vitest'
import { cancelClass, changeStudentStatus, changeSubjectStatus, createClass, createStudent, createSubject, getActiveTeachers, getClass, getClasses, getStudent, getStudents, getSubject, getSubjects, login, updateClass, updateStudent, updateSubject } from './api'

afterEach(() => vi.unstubAllGlobals())

describe('API del frontend', () => {
  it('envía credenciales al login y devuelve la sesión', async () => {
    const session = { accessToken: 'token-de-prueba', user: { id: '1', name: 'Ana', username: 'ana', role: 'TEACHER' } }
    const fetchMock = vi.fn().mockResolvedValue(Response.json(session))
    vi.stubGlobal('fetch', fetchMock)

    await expect(login('ana', 'clave-de-prueba')).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'ana', password: 'clave-de-prueba' }),
    })
  })

  it('expone el estado HTTP cuando el login falla', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })))
    await expect(login('ana', 'incorrecta')).rejects.toMatchObject({ status: 401 })
  })

  it('consulta alumnos con token, búsqueda y estado', async () => {
    const students = [{ id: '1', name: 'Juan', contactNumber: null, status: 'ACTIVE' }]
    const fetchMock = vi.fn().mockResolvedValue(Response.json(students))
    vi.stubGlobal('fetch', fetchMock)
    const signal = new AbortController().signal

    await expect(getStudents('token-de-prueba', 'Juan Pérez', 'ACTIVE', signal)).resolves.toEqual(students)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/students?search=Juan+P%C3%A9rez&status=ACTIVE', {
      headers: { Authorization: 'Bearer token-de-prueba' }, signal,
    })
  })

  it('crea y modifica alumnos con los campos opcionales y el token', async () => {
    const student = { id: '123', name: 'Juan', contactNumber: null, notes: null, status: 'ACTIVE', createdAt: '2026-11-10T12:00:00Z' }
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(Response.json(student)))
    vi.stubGlobal('fetch', fetchMock)
    const input = { name: 'Juan', contactNumber: null, notes: null }

    await expect(createStudent('token-de-prueba', input)).resolves.toEqual(student)
    await expect(updateStudent('token-de-prueba', '123', input)).resolves.toEqual(student)
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/v1/students', {
      method: 'POST', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify(input),
    })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/v1/students/123', {
      method: 'PATCH', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify(input),
    })
  })

  it('consulta el detalle y cambia el estado explícitamente', async () => {
    const student = { id: '123', name: 'Juan', contactNumber: null, notes: null, status: 'INACTIVE', createdAt: '2026-11-10T12:00:00Z' }
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(Response.json(student)))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getStudent('token-de-prueba', '123')).resolves.toEqual(student)
    await expect(changeStudentStatus('token-de-prueba', '123', 'INACTIVE')).resolves.toEqual(student)
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/v1/students/123', {
      method: 'GET', headers: { Authorization: 'Bearer token-de-prueba' },
    })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/v1/students/123/status', {
      method: 'PATCH', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify({ status: 'INACTIVE' }),
    })
  })

  it('lista materias con filtros y autorización', async () => {
    const subjects = [{ id: '123', name: 'Matemática', description: null, status: 'ACTIVE' }]
    const fetchMock = vi.fn().mockResolvedValue(Response.json(subjects))
    vi.stubGlobal('fetch', fetchMock)
    const signal = new AbortController().signal

    await expect(getSubjects('token-de-prueba', 'Matemática', 'ACTIVE', signal)).resolves.toEqual(subjects)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/subjects?search=Matem%C3%A1tica&status=ACTIVE', {
      headers: { Authorization: 'Bearer token-de-prueba' }, signal,
    })
  })

  it('crea, consulta, edita y cambia el estado de una materia', async () => {
    const subject = { id: '123', name: 'Matemática', description: null, status: 'ACTIVE' }
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(Response.json(subject)))
    vi.stubGlobal('fetch', fetchMock)
    const input = { name: 'Matemática', description: null }

    await expect(createSubject('token-de-prueba', input)).resolves.toEqual(subject)
    await expect(getSubject('token-de-prueba', '123')).resolves.toEqual(subject)
    await expect(updateSubject('token-de-prueba', '123', input)).resolves.toEqual(subject)
    await expect(changeSubjectStatus('token-de-prueba', '123', 'INACTIVE')).resolves.toEqual(subject)
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/v1/subjects', {
      method: 'POST', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify(input),
    })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/v1/subjects/123', {
      method: 'GET', headers: { Authorization: 'Bearer token-de-prueba' },
    })
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/v1/subjects/123', {
      method: 'PATCH', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify(input),
    })
    expect(fetchMock).toHaveBeenNthCalledWith(4, '/api/v1/subjects/123/status', {
      method: 'PATCH', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify({ status: 'INACTIVE' }),
    })
  })

  it('consulta profesores activos y clases con filtros', async () => {
    const fetchMock = vi.fn()
      .mockImplementationOnce(() => Promise.resolve(Response.json([{ id: 'profesor-1', name: 'Ana' }])))
      .mockImplementationOnce(() => Promise.resolve(Response.json([])))
    vi.stubGlobal('fetch', fetchMock)
    const signal = new AbortController().signal

    await getActiveTeachers('token-de-prueba')
    await getClasses('token-de-prueba', { from: '2026-11-10', to: '2026-11-20', teacherId: 'profesor-1', status: 'SCHEDULED' }, signal)
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/v1/users?role=TEACHER&status=ACTIVE', {
      method: 'GET', headers: { Authorization: 'Bearer token-de-prueba' },
    })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/v1/classes?from=2026-11-10&to=2026-11-20&teacherId=profesor-1&status=SCHEDULED', {
      headers: { Authorization: 'Bearer token-de-prueba' }, signal,
    })
  })

  it('programa, consulta, modifica y cancela una clase', async () => {
    const academyClass = { id: 'clase-1', date: '2026-11-20', startTime: '18:00:00', endTime: '20:00:00', teacher: { id: 'profesor-1', name: 'Ana' }, status: 'SCHEDULED', notes: null }
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(Response.json(academyClass)))
    vi.stubGlobal('fetch', fetchMock)
    const input = { date: '2026-11-20', startTime: '18:00:00', endTime: '20:00:00', notes: null }

    await createClass('token-de-prueba', { ...input, teacherId: 'profesor-1' })
    await getClass('token-de-prueba', 'clase-1')
    await updateClass('token-de-prueba', 'clase-1', input)
    await cancelClass('token-de-prueba', 'clase-1')
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/v1/classes', {
      method: 'POST', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify({ ...input, teacherId: 'profesor-1' }),
    })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/v1/classes/clase-1', {
      method: 'GET', headers: { Authorization: 'Bearer token-de-prueba' },
    })
    expect(fetchMock).toHaveBeenNthCalledWith(3, '/api/v1/classes/clase-1', {
      method: 'PATCH', headers: { Authorization: 'Bearer token-de-prueba', 'Content-Type': 'application/json' }, body: JSON.stringify(input),
    })
    expect(fetchMock).toHaveBeenNthCalledWith(4, '/api/v1/classes/clase-1/cancel', {
      method: 'POST', headers: { Authorization: 'Bearer token-de-prueba' },
    })
  })
})
