<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { deleteRoute, getMyRoutes } from '@/api/admin'
import type { UserRouteSummary } from '@/types/api'
import { useToast } from '@/composables/useToast'
import { useRouter } from 'vue-router'

const { show } = useToast()
const router = useRouter()

const loading = ref(false)
const routes = ref<UserRouteSummary[]>([])

onMounted(loadRoutes)

async function loadRoutes() {
  loading.value = true
  try {
    routes.value = await getMyRoutes()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载路线失败', 'error')
  } finally {
    loading.value = false
  }
}

async function handleDelete(route: UserRouteSummary) {
  if (!window.confirm(`确定删除路线「${route.name}」吗？`)) return

  try {
    await deleteRoute(route.id)
    show('路线已删除')
    await loadRoutes()
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  }
}

function formatDistance(meters: number | null) {
  if (meters == null) return '-'
  return meters >= 1000 ? `${(meters / 1000).toFixed(2)} km` : `${Math.round(meters)} m`
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<template>
  <section class="routes-page">
    <article class="card">
      <header class="panel-head">
        <h2>我的路线</h2>
        <button type="button" class="btn btn-login" @click="router.push('/routes/new')">
          新建路线
        </button>
      </header>

      <p v-if="loading" class="hint pad">加载中…</p>

      <template v-else>
        <section v-if="routes.length" class="table-wrap">
          <table class="data-table">
            <thead>
            <tr>
              <th>名称</th>
              <th>描述</th>
              <th>距离</th>
              <th>预估时间</th>
              <th>途经点</th>
              <th>公开</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="route in routes" :key="route.id">
              <td>{{ route.name }}</td>
              <td>{{ route.description || '-' }}</td>
              <td>{{ formatDistance(route.totalDistanceMeters) }}</td>
              <td>{{ route.estimatedTimeMinutes ? `${route.estimatedTimeMinutes} 分钟` : '-' }}</td>
              <td>{{ route.waypointCount }}</td>
              <td>{{ route.isPublic ? '是' : '否' }}</td>
              <td>{{ formatDate(route.createdAt) }}</td>
              <td>
                <button type="button" class="btn btn-login btn-xs" @click="router.push(`/routes/${route.id}`)">
                  查看
                </button>
                <button type="button" class="btn btn-danger btn-xs" @click="handleDelete(route)">
                  删除
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </section>
        <p v-else class="hint pad">暂无路线，点击右上角创建新路线</p>
      </template>
    </article>
  </section>
</template>

<style scoped>
.routes-page {
  width: 100%;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

.table-wrap {
  overflow-x: auto;
}

.btn-xs {
  padding: 5px 12px;
  font-size: 12px;
}

.btn-xs + .btn-xs {
  margin-left: 8px;
}

.pad {
  padding: 12px;
}

.hint {
  font-size: 13px;
  color: var(--text-sub);
}
</style>