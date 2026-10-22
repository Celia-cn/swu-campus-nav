import type { ApiResponse } from '@/types/api'

const TOKEN_KEY = 'swu_admin_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number
  code: number

  constructor(status: number, code: number, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function parseJson<T>(response: Response): Promise<ApiResponse<T>> {
  const text = await response.text()
  if (!text) {
    throw new ApiError(response.status, response.status, '服务器无响应')
  }
  return JSON.parse(text) as ApiResponse<T>
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, { ...options, headers })
  const body = await parseJson<T>(response)

  if (!response.ok || body.code !== 0) {
    const message = body.message || `请求失败 HTTP ${response.status}`
    if (response.status === 401) {
      clearToken()
    }
    throw new ApiError(response.status, body.code, message)
  }

  return body.data
}

export async function uploadFile(file: File, type: 'image' | 'audio'): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  form.append('type', type)
  const result = await request<{ url: string }>('/api/admin/media/upload', {
    method: 'POST',
    body: form,
  })
  return result.url
}

export function mediaUrl(path: string | null | undefined): string {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return path.startsWith('/') ? path : `/${path}`
}
