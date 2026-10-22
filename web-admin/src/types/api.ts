export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserProfile {
  id: number
  username: string
  role: string
  createdAt: string
}

export interface AuthData {
  token: string
  expiresAt: string
  user: UserProfile
}

export interface LocationItem {
  id: number
  name: string
  category: string
  address: string | null
  latitude: number
  longitude: number
  radiusMeters: number
  cooldownSeconds: number
  description: string
  audioUrl: string | null
  imageUrl: string | null
  imageUrls?: string[]
}

export interface LocationMediaResult {
  imageUrls: string[]
  audioUrl: string | null
}

export interface AdminLocationItem extends LocationItem {
  playCount: number
}

export interface LocationUpsertPayload {
  name: string
  category: string
  address?: string | null
  latitude: number
  longitude: number
  radiusMeters: number
  cooldownSeconds: number
  description: string
  audioUrl?: string | null
  imageUrl?: string | null
}

export interface HotLocation {
  locationId: number
  locationName: string
  playCount: number
}

export interface VisitHourStat {
  hour: number
  visitorCount: number
}

export interface AdminStats {
  totalUsers: number
  totalLocations: number
  totalPlayCount: number
  todayVisitors: number
  todayNewUsers: number
  todayPlayCount: number
  weekPlayCount: number
  autoPlayCount: number
  manualPlayCount: number
  hotLocations: HotLocation[]
  visitHourStats: VisitHourStat[]
}

export type FeedbackStatus = 'PENDING' | 'COMPLETED'

export type FeedbackSortBy = 'createdAt' | 'completedAt' | 'content'

export interface FeedbackItem {
  id: number
  userId: number
  username: string
  content: string
  status: FeedbackStatus
  createdAt: string
  completedAt: string | null
}

export interface FeedbackStats {
  totalCount: number
  todayCount: number
  pendingCount: number
  completedCount: number
}

export type RagCategory = 'HISTORY' | 'NEWS' | 'FOOD' | 'BUS' | 'ROUTE' | 'GENERAL'

export interface RagDocument {
  id: number
  title: string
  category: RagCategory
  content: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface RagUpsertPayload {
  title: string
  category: RagCategory
  content: string
  enabled: boolean
}

export interface UserSummary {
  id: number
  username: string
  role: string
  createdAt: string
}

export interface UserUpdatePayload {
  role: string
  password?: string | null
}

export interface MediaUploadResult {
  url: string
}

// 路线相关类型
export interface UserRouteSummary {
  id: number
  name: string
  description: string | null
  totalDistanceMeters: number | null
  estimatedTimeMinutes: number | null
  waypointCount: number
  isPublic: boolean
  likeCount: number
  createdAt: string
}

export interface UserRouteDetail extends UserRouteSummary {
  waypoints: RouteWaypointDetail[]
  updatedAt: string
}

export interface RouteWaypointDetail {
  id: number
  waypointOrder: number
  latitude: number
  longitude: number
  locationId: number | null
  locationName: string | null
  stayDurationMinutes: number
  notes: string | null
}

export interface RoutePlanRequest {
  startLatitude: number
  startLongitude: number
  endLatitude: number
  endLongitude: number
  routeMode?: string
}

export interface MultiStopRouteRequest {
  startLatitude?: number
  startLongitude?: number
  locationIds: number[]
  routeMode?: string
}

export interface SaveRouteRequest {
  name: string
  description?: string
  isPublic?: boolean
  waypoints: WaypointRequest[]
}

export interface WaypointRequest {
  latitude: number
  longitude: number
  locationId?: number
  stayDurationMinutes?: number
  notes?: string
}

export interface RoutePlanResponse {
  routePoints: GeoPointDto[]
  totalDistanceMeters: number
  estimatedTimeMinutes: number
  instructions: RouteInstruction[]
}

export interface GeoPointDto {
  latitude: number
  longitude: number
}

export interface RouteInstruction {
  step: number
  latitude: number
  longitude: number
  description: string
}

// 触发历史类型
export interface TriggerHistoryItem {
  id: number
  locationId: number
  locationName: string
  triggerType: string
  distanceMeters: number
  success: boolean
  createdAt: string
}
