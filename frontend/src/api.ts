export type User = { id: string; name: string; username: string; role: 'ADMIN' | 'TEACHER' }
export type Session = { accessToken: string; user: User }
export type Student = { id: string; name: string; contactNumber: string | null; notes: string | null; status: 'ACTIVE' | 'INACTIVE'; createdAt: string }
export type StudentInput = { name: string; contactNumber: string | null; notes: string | null }
export type Subject = { id: string; name: string; description: string | null; status: 'ACTIVE' | 'INACTIVE' }
export type SubjectInput = { name: string; description: string | null }
export type Teacher = { id: string; name: string; username: string; role: 'TEACHER'; enabled: boolean }
export type AcademyClass = { id: string; date: string; startTime: string; endTime: string; teacher: { id: string; name: string }; status: 'SCHEDULED' | 'FINALIZED' | 'CANCELLED'; notes: string | null }
export type ClassInput = { date: string; startTime: string; endTime: string; notes: string | null }
export type CreateClassInput = ClassInput & { teacherId: string | null }
export type ClassFilters = { from: string; to: string; teacherId: string; status: string }

export class ApiError extends Error {
  readonly status: number

  constructor(status: number) {
    super(`API request failed: ${status}`)
    this.status = status
  }
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) throw new ApiError(response.status)
  return response.json() as Promise<T>
}

export async function login(username: string, password: string): Promise<Session> {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  return readResponse<Session>(response)
}

export async function getStudents(token: string, search: string, status: string, signal: AbortSignal): Promise<Student[]> {
  const query = new URLSearchParams()
  if (search) query.set('search', search)
  if (status !== 'ALL') query.set('status', status)
  const suffix = query.size ? `?${query}` : ''
  const response = await fetch(`/api/v1/students${suffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    signal,
  })
  return readResponse<Student[]>(response)
}

async function authorizedRequest<T>(url: string, token: string, method: 'GET' | 'POST' | 'PATCH', body?: unknown): Promise<T> {
  const response = await fetch(url, {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  return readResponse<T>(response)
}

export function getStudent(token: string, id: string): Promise<Student> {
  return authorizedRequest(`/api/v1/students/${id}`, token, 'GET')
}

export function createStudent(token: string, input: StudentInput): Promise<Student> {
  return authorizedRequest('/api/v1/students', token, 'POST', input)
}

export function updateStudent(token: string, id: string, input: StudentInput): Promise<Student> {
  return authorizedRequest(`/api/v1/students/${id}`, token, 'PATCH', input)
}

export function changeStudentStatus(token: string, id: string, status: Student['status']): Promise<Student> {
  return authorizedRequest(`/api/v1/students/${id}/status`, token, 'PATCH', { status })
}

export async function getSubjects(token: string, search: string, status: string, signal: AbortSignal): Promise<Subject[]> {
  const query = new URLSearchParams()
  if (search) query.set('search', search)
  if (status !== 'ALL') query.set('status', status)
  const suffix = query.size ? `?${query}` : ''
  const response = await fetch(`/api/v1/subjects${suffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    signal,
  })
  return readResponse<Subject[]>(response)
}

export function getSubject(token: string, id: string): Promise<Subject> {
  return authorizedRequest(`/api/v1/subjects/${id}`, token, 'GET')
}

export function createSubject(token: string, input: SubjectInput): Promise<Subject> {
  return authorizedRequest('/api/v1/subjects', token, 'POST', input)
}

export function updateSubject(token: string, id: string, input: SubjectInput): Promise<Subject> {
  return authorizedRequest(`/api/v1/subjects/${id}`, token, 'PATCH', input)
}

export function changeSubjectStatus(token: string, id: string, status: Subject['status']): Promise<Subject> {
  return authorizedRequest(`/api/v1/subjects/${id}/status`, token, 'PATCH', { status })
}

export async function getActiveTeachers(token: string): Promise<Teacher[]> {
  return authorizedRequest('/api/v1/users?role=TEACHER&status=ACTIVE', token, 'GET')
}

export async function getClasses(token: string, filters: ClassFilters, signal: AbortSignal): Promise<AcademyClass[]> {
  const query = new URLSearchParams()
  if (filters.from) query.set('from', filters.from)
  if (filters.to) query.set('to', filters.to)
  if (filters.teacherId) query.set('teacherId', filters.teacherId)
  if (filters.status !== 'ALL') query.set('status', filters.status)
  const suffix = query.size ? `?${query}` : ''
  const response = await fetch(`/api/v1/classes${suffix}`, {
    headers: { Authorization: `Bearer ${token}` }, signal,
  })
  return readResponse<AcademyClass[]>(response)
}

export function getClass(token: string, id: string): Promise<AcademyClass> {
  return authorizedRequest(`/api/v1/classes/${id}`, token, 'GET')
}

export function createClass(token: string, input: CreateClassInput): Promise<AcademyClass> {
  return authorizedRequest('/api/v1/classes', token, 'POST', input)
}

export function updateClass(token: string, id: string, input: ClassInput): Promise<AcademyClass> {
  return authorizedRequest(`/api/v1/classes/${id}`, token, 'PATCH', input)
}

export function cancelClass(token: string, id: string): Promise<AcademyClass> {
  return authorizedRequest(`/api/v1/classes/${id}/cancel`, token, 'POST')
}
