<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { deleteLocation, fetchAdminLocations, fetchStats } from '@/api/admin'
import FilterSuggestInput from '@/components/FilterSuggestInput.vue'
import type { AdminLocationItem, AdminStats } from '@/types/api'
import { useToast } from '@/composables/useToast'

const PAGE_SIZE = 10

const router = useRouter()
const { show } = useToast()

const loading = ref(false)
const statsLoading = ref(true)
const filterOpen = ref(true)
const locations = ref<AdminLocationItem[]>([])
const stats = ref<AdminStats | null>(null)
const currentPage = ref(1)

const draft = reactive({
  keyword: '',
  category: '',
  sortBy: 'name' as 'name' | 'frequency',
  order: 'asc' as 'asc' | 'desc',
})

const applied = reactive({
  keyword: '',
  category: '',
  sortBy: 'name' as 'name' | 'frequency',
  order: 'asc' as 'asc' | 'desc',
})

const nameOptions = computed(() => {
  const set = new Set<string>()
  for (const item of locations.value) {
    if (item.name?.trim()) set.add(item.name.trim())
  }
  return Array.from(set).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const categoryOptions = computed(() => {
  const set = new Set<string>()
  for (const item of locations.value) {
    if (item.category?.trim()) set.add(item.category.trim())
  }
  return Array.from(set).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const filteredList = computed(() => {
  let list = [...locations.value]

  const kw = applied.keyword.trim().toLowerCase()
  if (kw) {
    list = list.filter((item) => item.name.toLowerCase().includes(kw))
  }

  const cat = applied.category.trim().toLowerCase()
  if (cat) {
    list = list.filter((item) => item.category.toLowerCase().includes(cat))
  }

  const dir = applied.order === 'asc' ? 1 : -1
  if (applied.sortBy === 'frequency') {
    list.sort((a, b) => (a.playCount - b.playCount) * dir || a.name.localeCompare(b.name, 'zh-CN'))
  } else {
    list.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN') * dir)
  }

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredList.value.length / PAGE_SIZE)))

const pagedList = computed(() => {
  const page = Math.min(currentPage.value, totalPages.value)
  const start = (page - 1) * PAGE_SIZE
  return filteredList.value.slice(start, start + PAGE_SIZE)
})

watch(filteredList, () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value
  }
})

onMounted(() => {
  loadStats()
  loadList()
})

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await fetchStats()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载统计失败', 'error')
  } finally {
    statsLoading.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    locations.value = await fetchAdminLocations()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载讲解点失败', 'error')
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadStats(), loadList()])
}

function applySearch() {
  applied.keyword = draft.keyword
  applied.category = draft.category
  applied.sortBy = draft.sortBy
  applied.order = draft.order
  currentPage.value = 1
}

function resetFilters() {
  draft.keyword = ''
  draft.category = ''
  draft.sortBy = 'name'
  draft.order = 'asc'
  applied.keyword = ''
  applied.category = ''
  applied.sortBy = 'name'
  applied.order = 'asc'
  currentPage.value = 1
}

function openEdit(item: AdminLocationItem) {
  router.push(`/locations/${item.id}/edit`)
}

async function remove(item: AdminLocationItem) {
  if (!window.confirm(`确定删除「${item.name}」吗？`)) return
  try {
    await deleteLocation(item.id)
    show('已删除')
    await refreshAll()
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  }
}

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

function rowIndex(index: number) {
  return (currentPage.value - 1) * PAGE_SIZE + index + 1
}
</script>

<template>
  <section class="locations-page">
    <section class="data-screen card">
      <header class="screen-head">
        <h2>游览数据大屏</h2>
        <button type="button" class="screen-refresh" :disabled="statsLoading" @click="loadStats">刷新</button>
      </header>
      <section v-if="statsLoading" class="screen-grid screen-grid-3">
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
      </section>
      <section v-else class="screen-grid screen-grid-3">
        <article class="screen-card highlight">
          <span class="screen-label">今日游览总数</span>
          <strong class="screen-value">{{ stats?.todayPlayCount ?? 0 }}</strong>
          <span class="screen-unit">次</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">本周用户游览总数</span>
          <strong class="screen-value">{{ stats?.weekPlayCount ?? 0 }}</strong>
          <span class="screen-unit">次</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">地点总数</span>
          <strong class="screen-value">{{ stats?.totalLocations ?? 0 }}</strong>
          <span class="screen-unit">个</span>
        </article>
      </section>
    </section>

    <article class="card list-panel">
      <button type="button" class="filter-toggle" @click="filterOpen = !filterOpen">
        <span>筛选条件</span>
        <span class="filter-arrow" :class="{ open: filterOpen }">▼</span>
      </button>

      <section v-show="filterOpen" class="filter-panel">
        <section class="filter-row">
          <section class="field field-keyword">
            <label>地点名称</label>
            <FilterSuggestInput v-model="draft.keyword" :options="nameOptions" placeholder="关键词搜索" />
          </section>
          <section class="field field-category">
            <label>分类</label>
            <FilterSuggestInput v-model="draft.category" :options="categoryOptions" placeholder="关键词搜索" />
          </section>
          <section class="field field-sort">
            <label>排序方式</label>
            <select v-model="draft.sortBy">
              <option value="name">首字母排序</option>
              <option value="frequency">游览频次</option>
            </select>
          </section>
          <section class="field field-order">
            <label>正逆序</label>
            <section class="order-toggle">
              <button
                type="button"
                class="order-dot"
                :class="{ active: draft.order === 'asc' }"
                title="正序"
                @click="draft.order = 'asc'"
              >
                ↑
              </button>
              <button
                type="button"
                class="order-dot"
                :class="{ active: draft.order === 'desc' }"
                title="逆序"
                @click="draft.order = 'desc'"
              >
                ↓
              </button>
            </section>
          </section>
          <section class="filter-actions">
            <button type="button" class="btn btn-login btn-filter" @click="applySearch">搜索</button>
            <button type="button" class="btn btn-secondary btn-filter" @click="resetFilters">重置</button>
          </section>
        </section>
      </section>

      <header class="panel-head">
        <h2>地点列表</h2>
        <section class="panel-actions">
          <button type="button" class="btn btn-login btn-add" @click="router.push('/locations/new')">
            新增
          </button>
          <button type="button" class="btn-refresh" :disabled="loading" @click="refreshAll">刷新</button>
        </section>
      </header>

      <p v-if="loading" class="hint pad">加载中…</p>
      <template v-else>
        <section v-if="pagedList.length" class="table-wrap">
          <table class="data-table location-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th class="col-name">名称</th>
                <th class="col-category">分类</th>
                <th class="col-cooldown">围栏冷却</th>
                <th class="col-count">游览次数</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in pagedList" :key="item.id">
                <td class="col-index">{{ rowIndex(index) }}</td>
                <td class="col-name">{{ item.name }}</td>
                <td class="col-category">{{ item.category }}</td>
                <td class="col-cooldown">{{ item.cooldownSeconds ?? 120 }} 秒</td>
                <td class="col-count">{{ item.playCount }}</td>
                <td class="col-actions">
                  <button type="button" class="btn btn-login btn-xs" @click="openEdit(item)">编辑</button>
                  <button type="button" class="btn btn-danger btn-xs" @click="remove(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>
        <p v-else class="hint pad">暂无符合条件的讲解点</p>

        <footer v-if="filteredList.length > 0" class="pagination">
          <section class="page-bar">
            <button
              type="button"
              class="page-btn"
              :disabled="currentPage <= 1"
              aria-label="上一页"
              @click="goPage(currentPage - 1)"
            >
              ‹
            </button>
            <span class="page-indicator" aria-live="polite">
              <span class="page-current">{{ currentPage }}</span>
              <span class="page-sep" aria-hidden="true">|</span>
              <span class="page-total">{{ totalPages }}</span>
            </span>
            <button
              type="button"
              class="page-btn"
              :disabled="currentPage >= totalPages"
              aria-label="下一页"
              @click="goPage(currentPage + 1)"
            >
              ›
            </button>
          </section>
        </footer>
      </template>
    </article>
  </section>
</template>

<style scoped>
.locations-page {
  width: 100%;
}

.data-screen {
  margin-bottom: 20px;
  padding: 0;
  overflow: hidden;
  background: var(--data-screen-bg);
  border: 1px solid var(--stroke-soft);
}

.screen-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
  gap: 12px;
}

.screen-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.screen-refresh {
  flex-shrink: 0;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-sub);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--stroke-soft);
  border-radius: 6px;
  cursor: pointer;
}

.screen-refresh:hover:not(:disabled) {
  color: var(--text-main);
  background: #fff;
}

.screen-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.screen-grid {
  display: grid;
  gap: 16px;
  padding: 16px 20px 20px;
}

.screen-grid-3 {
  grid-template-columns: repeat(3, 1fr);
}

@media (max-width: 900px) {
  .screen-grid-3 {
    grid-template-columns: 1fr;
  }
}

.screen-card {
  padding: 24px 20px;
  border-radius: var(--radius-md);
  background: rgba(255, 254, 254, 0.92);
  border: 1px solid rgba(217, 235, 208, 0.8);
  box-shadow: var(--shadow);
  text-align: center;
}

.screen-card.highlight {
  background: linear-gradient(145deg, rgba(255, 254, 254, 0.95), rgba(204, 245, 248, 0.5));
  border-color: var(--brand-accent);
}

.loading-card {
  color: var(--text-sub);
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.screen-label {
  display: block;
  font-size: 14px;
  color: var(--text-sub);
  margin-bottom: 10px;
}

.screen-value {
  display: block;
  font-size: 42px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--brand-accent-dark);
  letter-spacing: -1px;
}

.screen-card.highlight .screen-value {
  color: #4a8a7a;
}

.screen-unit {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-sub);
}

.list-panel {
  width: 100%;
  padding: 18px;
}

.filter-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  background: var(--surface-soft-alt);
  border: 1px solid var(--stroke-soft);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.filter-arrow {
  font-size: 10px;
  color: var(--text-sub);
  transition: transform 0.2s;
}

.filter-arrow.open {
  transform: rotate(180deg);
}

.filter-panel {
  margin-bottom: 16px;
  padding: 14px;
  background: var(--surface-soft-alt);
  border: 1px solid var(--stroke-soft);
  border-radius: var(--radius-sm);
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr minmax(118px, 160px) auto auto;
  align-items: end;
  gap: 12px;
  width: 100%;
}

.filter-panel .field {
  margin-bottom: 0;
  min-width: 0;
}

.field-sort select {
  width: 100%;
  padding-left: 8px;
  padding-right: 8px;
}

.order-toggle {
  display: flex;
  gap: 8px;
  align-items: center;
  height: 42px;
}

.order-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #d8dde6;
  background: #fff;
  color: var(--text-sub);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}

.order-dot.active {
  border-color: var(--brand-accent-dark);
  background: linear-gradient(135deg, rgba(253, 224, 141, 0.6), rgba(155, 196, 176, 0.5));
  color: var(--brand-accent-dark);
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 1px;
  justify-self: end;
}

.btn-filter {
  padding: 9px 16px;
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .filter-row {
    grid-template-columns: 1fr 1fr;
  }

  .field-sort,
  .field-order {
    grid-column: span 1;
  }

  .filter-actions {
    grid-column: 1 / -1;
    justify-self: end;
    padding-top: 4px;
  }
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.btn-add {
  padding: 6px 16px;
  font-size: 13px;
}

.btn-refresh {
  flex-shrink: 0;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-sub);
  background: #fff;
  border: 1px solid var(--stroke-soft);
  border-radius: 6px;
  cursor: pointer;
}

.btn-refresh:hover:not(:disabled) {
  color: var(--text-main);
  background: rgba(155, 196, 176, 0.2);
}

.table-wrap {
  overflow-x: auto;
}

.location-table {
  table-layout: fixed;
  width: 100%;
}

.location-table .col-index {
  width: 80px;
  text-align: center;
}

.location-table .col-name {
  text-align: center;
}

.location-table th.col-name,
.location-table td.col-name {
  text-align: center;
}

.location-table .col-category {
  width: 120px;
  text-align: center;
}

.location-table .col-cooldown {
  width: 100px;
  text-align: center;
}

.location-table .col-count {
  width: 100px;
  text-align: center;
}

.location-table .col-actions {
  width: 140px;
  white-space: nowrap;
}

.location-table .col-actions .btn + .btn {
  margin-left: 8px;
}

.btn-xs {
  padding: 5px 12px;
  font-size: 12px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #eef2f0;
}

.page-bar {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--stroke-soft);
  box-shadow: 0 2px 10px rgba(26, 31, 54, 0.06);
}

.page-btn {
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-sub);
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.page-btn:hover:not(:disabled) {
  background: rgba(155, 196, 176, 0.22);
  color: var(--brand-accent-dark);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 64px;
  padding: 0 12px;
  font-variant-numeric: tabular-nums;
}

.page-current {
  font-size: 15px;
  font-weight: 700;
  color: var(--brand-accent-dark);
}

.page-sep {
  font-size: 13px;
  font-weight: 400;
  color: #c5cad3;
  line-height: 1;
}

.page-total {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-sub);
}

.pad {
  padding: 12px;
}

.hint {
  font-size: 13px;
  color: var(--text-sub);
}
</style>
