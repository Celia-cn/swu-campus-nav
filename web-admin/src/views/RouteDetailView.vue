<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteRoute, getRouteDetail } from '@/api/admin'
import type { UserRouteDetail } from '@/types/api'
import { useToast } from '@/composables/useToast'

const { show } = useToast()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<UserRouteDetail | null>(null)

onMounted(loadDetail)

async function loadDetail() {
  const id = Number(route.params.id)
  if (!id) {
    show('无效的路线ID', 'error')
    router.push('/routes')
    return
  }

  loading.value = true
  try {
    detail.value = await getRouteDetail(id)
  } catch (e) {
    show(e instanceof Error ? e.message : '加载失败', 'error')
    router.push('/routes')
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  if (!detail.value) return
  if (!window.confirm(`确定删除路线「${detail.value.name}」吗？`)) return

  try {
    await deleteRoute(detail.value.id)
    show('路线已删除')
    router.push('/routes')
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  }
}

function formatDistance(meters: number | null) {
  if (meters == null) return '-'
  return meters >= 1000 ? `${(meters / 1000).toFixed(2)} km` : `${Math.round(meters)} m`
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<template>
  <section class="route-detail-page">
    <article v-if="loading" class="card">
      <p class="hint">加载中…</p>
    </article>

    <article v-else-if="detail" class="card detail-card">
      <header class="detail-header">
        <div>
          <h2>{{ detail.name }}</h2>
          <p v-if="detail.description" class="description">{{ detail.description }}</p>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" @click="router.push('/routes')">
            返回列表
          </button>
          <button type="button" class="btn btn-danger" @click="handleDelete">
            删除路线
          </button>
        </div>
      </header>

      <section class="info-grid">
        <div class="info-item">
          <label>总距离</label>
          <span>{{ formatDistance(detail.totalDistanceMeters) }}</span>
        </div>
        <div class="info-item">
          <label>预估时间</label>
          <span>{{ detail.estimatedTimeMinutes ? `${detail.estimatedTimeMinutes} 分钟` : '-' }}</span>
        </div>
        <div class="info-item">
          <label>途经点数量</label>
          <span>{{ detail.waypoints.length }}</span>
        </div>
        <div class="info-item">
          <label>公开状态</label>
          <span>{{ detail.isPublic ? '公开' : '私有' }}</span>
        </div>
        <div class="info-item">
          <label>点赞数</label>
          <span>{{ detail.likeCount }}</span>
        </div>
        <div class="info-item">
          <label>创建时间</label>
          <span>{{ formatDate(detail.createdAt) }}</span>
        </div>
        <div class="info-item">
          <label>更新时间</label>
          <span>{{ formatDate(detail.updatedAt) }}</span>
        </div>
      </section>

      <section class="waypoints-section">
        <h3>途经点列表</h3>
        <div v-if="detail.waypoints.length" class="waypoints-list">
          <div v-for="wp in detail.waypoints" :key="wp.id" class="waypoint-card">
            <div class="waypoint-order">{{ wp.waypointOrder }}</div>
            <div class="waypoint-info">
              <div class="waypoint-name">
                {{ wp.locationName || `坐标点 ${wp.waypointOrder}` }}
              </div>
              <div class="waypoint-coords">
                纬度: {{ wp.latitude.toFixed(6) }}, 经度: {{ wp.longitude.toFixed(6) }}
              </div>
              <div v-if="wp.stayDurationMinutes" class="waypoint-stay">
                建议停留: {{ wp.stayDurationMinutes }} 分钟
              </div>
              <div v-if="wp.notes" class="waypoint-notes">
                备注: {{ wp.notes }}
              </div>
            </div>
          </div>
        </div>
        <p v-else class="hint">暂无途经点</p>
      </section>
    </article>

    <article v-else class="card">
      <p class="hint">路线不存在</p>
      <button type="button" class="btn btn-login" @click="router.push('/routes')">
        返回路线列表
      </button>
    </article>
  </section>
</template>

<style scoped>
.route-detail-page {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
}

.detail-card {
  padding: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--divider-color);
}

.detail-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
}

.description {
  margin: 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.info-item {
  padding: 12px;
  background: rgba(155, 196, 176, 0.08);
  border-radius: 8px;
}

.info-item label {
  display: block;
  font-size: 12px;
  color: var(--text-sub);
  margin-bottom: 4px;
}

.info-item span {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.waypoints-section {
  margin-top: 24px;
}

.waypoints-section h3 {
  margin: 0 0 16px;
  font-size: 18px;
}

.waypoints-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.waypoint-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(253, 224, 141, 0.08);
  border-radius: 8px;
  border-left: 3px solid var(--brand-accent);
}

.waypoint-order {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--brand-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  flex-shrink: 0;
}

.waypoint-info {
  flex: 1;
  min-width: 0;
}

.waypoint-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.waypoint-coords {
  font-size: 13px;
  color: var(--text-sub);
  margin-bottom: 4px;
}

.waypoint-stay {
  font-size: 13px;
  color: var(--brand-accent-dark);
  margin-bottom: 4px;
}

.waypoint-notes {
  font-size: 13px;
  color: var(--text-sub);
  font-style: italic;
}

.hint {
  font-size: 13px;
  color: var(--text-sub);
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions button {
    flex: 1;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>