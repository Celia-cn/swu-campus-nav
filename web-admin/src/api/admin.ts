import { request } from '@/api/http'
import type {
  AdminStats,
  AuthData,
  AdminLocationItem,
  FeedbackItem,
  FeedbackStats,
  LocationItem,
  LocationMediaResult,
  LocationUpsertPayload,
  UserProfile,
  UserSummary,
  UserUpdatePayload,
  RoutePlanRequest,
  MultiStopRouteRequest,
  UserRouteSummary,
  SaveRouteRequest,
  UserRouteDetail,
  TriggerHistoryItem,
  RoutePlanResponse,
} from '@/types/api'



export function login(username: string, password: string) {
  return request<AuthData>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password, client: 'WEB' }),
  })
}

export function registerAdmin(username: string, password: string) {
  return request<AuthData>('/api/auth/admin/register', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function fetchMe() {
  return request<UserProfile>('/api/auth/me')
}

export function fetchStats() {
  return request<AdminStats>('/api/admin/stats')
}

export function fetchUsers() {
  return request<UserSummary[]>('/api/admin/users')
}

export function updateUser(id: number, payload: UserUpdatePayload) {
  return request<UserSummary>(`/api/admin/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteUser(id: number) {
  return request<void>(`/api/admin/users/${id}`, { method: 'DELETE' })
}

export function fetchLocations() {
  return request<LocationItem[]>('/api/locations')
}

export function fetchAdminLocations() {
  return request<AdminLocationItem[]>('/api/admin/locations')
}

export function createLocation(payload: LocationUpsertPayload) {
  return request<LocationItem>('/api/admin/locations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateLocation(id: number, payload: LocationUpsertPayload) {
  return request<LocationItem>(`/api/admin/locations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteLocation(id: number) {
  return request<void>(`/api/admin/locations/${id}`, { method: 'DELETE' })
}

export function uploadLocationImage(locationId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<LocationMediaResult>(`/api/admin/locations/${locationId}/images`, {
    method: 'POST',
    body: form,
  })
}

export function deleteLocationImage(locationId: number, url: string) {
  const q = new URLSearchParams({ url })
  return request<LocationMediaResult>(`/api/admin/locations/${locationId}/images?${q}`, {
    method: 'DELETE',
  })
}

export function uploadLocationAudio(locationId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<LocationMediaResult>(`/api/admin/locations/${locationId}/audio`, {
    method: 'POST',
    body: form,
  })
}

export function deleteLocationAudio(locationId: number) {
  return request<LocationMediaResult>(`/api/admin/locations/${locationId}/audio`, {
    method: 'DELETE',
  })
}

export function fetchFeedbacks() {
  return request<FeedbackItem[]>('/api/admin/feedbacks')
}

export function fetchFeedbackStats() {
  return request<FeedbackStats>('/api/admin/feedbacks/stats')
}

export function completeFeedback(id: number) {
  return request<FeedbackItem>(`/api/admin/feedbacks/${id}/complete`, { method: 'PATCH' })
}

export function deleteFeedback(id: number) {
  return request<void>(`/api/admin/feedbacks/${id}`, { method: 'DELETE' })
}

// 路线规划相关API
export function planRoute(payload: RoutePlanRequest) {
  return request<RoutePlanResponse>('/api/routes/plan', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function planMultiStopRoute(payload: MultiStopRouteRequest) {
  return request<RoutePlanResponse>('/api/routes/plan-multi-stop', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getMyRoutes() {
  return request<UserRouteSummary[]>('/api/routes/my-routes')
}

export function saveRoute(payload: SaveRouteRequest) {
  return request<UserRouteSummary>('/api/routes/save', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getRouteDetail(id: number) {
  return request<UserRouteDetail>(`/api/routes/${id}`)
}

export function deleteRoute(id: number) {
  return request<void>(`/api/routes/${id}`, { method: 'DELETE' })
}

// 触发历史API
export function getTriggerHistory(locationId: number, limit = 20) {
  return request<TriggerHistoryItem[]>(`/api/locations/${locationId}/trigger-history?limit=${limit}`)
}
