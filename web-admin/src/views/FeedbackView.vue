<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { completeFeedback, fetchFeedbacks, fetchFeedbackStats } from '@/api/admin'
import DateRangePicker from '@/components/DateRangePicker.vue'
import FilterSuggestInput from '@/components/FilterSuggestInput.vue'
import type { FeedbackItem, FeedbackSortBy, FeedbackStats, FeedbackStatus } from '@/types/api'
import { useToast } from '@/composables/useToast'

const PAGE_SIZE = 10
const LAST_WEEK_DAYS = 7

const { show } = useToast()

const loading = ref(true)
const statsLoading = ref(true)
const completingId = ref<number | null>(null)
const filterOpen = ref(true)
const feedbacks = ref<FeedbackItem[]>([])
const stats = ref<FeedbackStats | null>(null)
const currentPage = ref(1)

const draft = reactive({
  keyword: '',
  dateFrom: '',
  dateTo: '',
  status: '' as '' | FeedbackStatus,
  sortBy: 'createdAt' as FeedbackSortBy,
  order: 'asc' as 'asc' | 'desc',
})

const applied = reactive({
  keyword: '',
  dateFrom: '',
  dateTo: '',
  status: '' as '' | FeedbackStatus,
  sortBy: 'createdAt' as FeedbackSortBy,
  order: 'asc' as 'asc' | 'desc',
})

function formatDateKey(date: Date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getLastWeekRange() {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - (LAST_WEEK_DAYS - 1))
  return { from: formatDateKey(from), to: formatDateKey(to) }
}

function applyDefaultDateRange() {
  const range = getLastWeekRange()
  draft.dateFrom = range.from
  draft.dateTo = range.to
  applied.dateFrom = range.from
  applied.dateTo = range.to
}

function feedbackDateKey(createdAt: string) {
  const raw = String(createdAt ?? '').trim()
  if (!raw) return ''
  const match = raw.match(/^(\d{4}-\d{2}-\d{2})/)
  return match ? match[1]! : raw.slice(0, 10)
}

function inDateRange(createdAt: string, from: string, to: string) {
  const date = feedbackDateKey(createdAt)
  if (from && date < from) return false
  if (to && date > to) return false
  return true
}

function contentSortRank(content: string) {
  const text = content.trim()
  if (!text) return 3
  const ch = text[0]!
  if (/[a-zA-Z]/.test(ch)) return 0
  if (/[0-9]/.test(ch)) return 1
  return 2
}

function compareContent(a: string, b: string) {
  const rankDiff = contentSortRank(a) - contentSortRank(b)
  if (rankDiff !== 0) return rankDiff
  return a.trim().localeCompare(b.trim(), 'zh-CN', { sensitivity: 'base' })
}

function compareCompletedAt(a: FeedbackItem, b: FeedbackItem) {
  const va = a.completedAt
  const vb = b.completedAt
  if (!va && !vb) return 0
  if (!va) return 1
  if (!vb) return -1
  return va.localeCompare(vb)
}

function applyDefaultSort() {
  draft.sortBy = 'createdAt'
  draft.order = 'asc'
  applied.sortBy = 'createdAt'
  applied.order = 'asc'
}

const filteredList = computed(() => {
  let list = [...feedbacks.value]

  const kw = applied.keyword.trim().toLowerCase()
  if (kw) {
    list = list.filter((item) => item.content.toLowerCase().includes(kw))
  }

  if (applied.dateFrom || applied.dateTo) {
    list = list.filter((item) => inDateRange(item.createdAt, applied.dateFrom, applied.dateTo))
  }

  if (applied.status) {
    list = list.filter((item) => item.status === applied.status)
  }

  const dir = applied.order === 'asc' ? 1 : -1
  list.sort((a, b) => {
    let cmp = 0
    if (applied.sortBy === 'completedAt') {
      cmp = compareCompletedAt(a, b)
    } else if (applied.sortBy === 'content') {
      cmp = compareContent(a.content, b.content)
    } else {
      cmp = a.createdAt.localeCompare(b.createdAt)
    }
    return cmp * dir
  })

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

function rowIndex(index: number) {
  return (currentPage.value - 1) * PAGE_SIZE + index + 1
}

onMounted(() => {
  applyDefaultDateRange()
  applyDefaultSort()
  refreshAll()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onActivated(() => {
  refreshAll()
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    refreshAll()
  }
}

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await fetchFeedbackStats()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载统计失败', 'error')
  } finally {
    statsLoading.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    feedbacks.value = await fetchFeedbacks()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载反馈失败', 'error')
  } finally {
    loading.value = false
  }
}

function extendDateRangeToToday() {
  const today = formatDateKey(new Date())
  if (applied.dateTo && applied.dateTo < today) {
    draft.dateTo = today
    applied.dateTo = today
  }
}

async function refreshAll() {
  extendDateRangeToToday()
  await Promise.all([loadStats(), loadList()])
}

function applySearch() {
  applied.keyword = draft.keyword
  applied.dateFrom = draft.dateFrom
  applied.dateTo = draft.dateTo
  applied.status = draft.status
  applied.sortBy = draft.sortBy
  applied.order = draft.order
  currentPage.value = 1
}

function resetFilters() {
  draft.keyword = ''
  draft.status = ''
  applied.keyword = ''
  applied.status = ''
  applyDefaultDateRange()
  applyDefaultSort()
  currentPage.value = 1
}

function formatTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function formatStatus(status: FeedbackStatus) {
  return status === 'COMPLETED' ? '已完成' : '待处理'
}

function completeButtonLabel(item: FeedbackItem) {
  if (item.status === 'COMPLETED') return '已处理'
  if (completingId.value === item.id) return '处理中…'
  return '已处理'
}

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

async function markCompleted(item: FeedbackItem) {
  if (item.status === 'COMPLETED') return
  completingId.value = item.id
  try {
    const updated = await completeFeedback(item.id)
    show('已标记为已完成')
    const index = feedbacks.value.findIndex((row) => row.id === item.id)
    if (index >= 0) {
      feedbacks.value[index] = updated
    }
    await loadStats()
  } catch (e) {
    show(e instanceof Error ? e.message : '操作失败', 'error')
  } finally {
    completingId.value = null
  }
}

</script>

<template>
  <section class="feedback-page">
    <section class="data-screen card">
      <header class="screen-head">
        <h2>反馈数据大屏</h2>
        <button type="button" class="screen-refresh" :disabled="statsLoading" @click="loadStats">刷新</button>
      </header>
      <section v-if="statsLoading" class="screen-grid screen-grid-4">
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
      </section>
      <section v-else class="screen-grid screen-grid-4">
        <article class="screen-card highlight">
          <span class="screen-label">待处理反馈</span>
          <strong class="screen-value">{{ stats?.pendingCount ?? 0 }}</strong>
          <span class="screen-unit">条</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">今日反馈总数</span>
          <strong class="screen-value">{{ stats?.todayCount ?? 0 }}</strong>
          <span class="screen-unit">条</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">已完成反馈</span>
          <strong class="screen-value">{{ stats?.completedCount ?? 0 }}</strong>
          <span class="screen-unit">条</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">反馈总量</span>
          <strong class="screen-value">{{ stats?.totalCount ?? 0 }}</strong>
          <span class="screen-unit">条</span>
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
            <label>反馈内容</label>
            <FilterSuggestInput v-model="draft.keyword" :options="[]" placeholder="关键词搜索" />
          </section>
          <section class="field field-range">
            <label>时间段</label>
            <DateRangePicker v-model:from="draft.dateFrom" v-model:to="draft.dateTo" />
          </section>
          <section class="field field-status">
            <label>反馈状态</label>
            <select v-model="draft.status">
              <option value="">全部</option>
              <option value="PENDING">待处理</option>
              <option value="COMPLETED">已完成</option>
            </select>
          </section>
          <section class="field field-sort">
            <label>排序方式</label>
            <select v-model="draft.sortBy">
              <option value="createdAt">反馈时间</option>
              <option value="completedAt">处理时间</option>
              <option value="content">内容首字母</option>
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
        <h2>用户反馈列表</h2>
        <button type="button" class="btn-refresh" :disabled="loading" @click="refreshAll">刷新</button>
      </header>

      <p v-if="loading" class="hint pad">加载中…</p>
      <template v-else>
        <section class="table-wrap">
          <table class="data-table feedback-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th class="col-time">反馈时间</th>
                <th class="col-content">反馈内容</th>
                <th class="col-status">反馈状态</th>
                <th class="col-time">处理时间</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in pagedList" :key="item.id">
                <td class="col-index">{{ rowIndex(index) }}</td>
                <td class="col-time">{{ formatTime(item.createdAt) }}</td>
                <td class="col-content">
                  <span class="content-text" :title="item.content">{{ item.content }}</span>
                </td>
                <td class="col-status">
                  <span class="badge" :class="item.status === 'COMPLETED' ? 'badge-completed' : 'badge-pending'">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td class="col-time">{{ item.completedAt ? formatTime(item.completedAt) : '-' }}</td>
                <td class="col-actions">
                  <button
                    type="button"
                    class="btn btn-login btn-xs btn-complete"
                    :disabled="item.status === 'COMPLETED' || completingId === item.id"
                    @click="markCompleted(item)"
                  >
                    {{ completeButtonLabel(item) }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <footer class="pagination">
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
.feedback-page {
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

.screen-grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

@media (max-width: 1100px) {
  .screen-grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .screen-grid-4 {
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
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 360px;
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
  grid-template-columns: 1fr max-content 110px minmax(128px, 148px) auto auto;
  align-items: end;
  gap: 12px;
  width: 100%;
}

.field-keyword {
  min-width: 0;
}

.field-range {
  width: max-content;
  max-width: 100%;
}

.filter-panel .field {
  margin-bottom: 0;
  min-width: 0;
}

.field-status select,
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

  .field-range,
  .field-status,
  .field-sort,
  .field-order {
    grid-column: span 1;
  }

  .filter-actions {
    grid-column: 1 / -1;
    justify-self: end;
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

.col-index {
  width: 1%;
  min-width: 4.5em;
  padding-left: 10px;
  padding-right: 10px;
  white-space: nowrap;
  text-align: center;
  color: var(--text-sub);
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

.table-wrap {
  overflow-x: auto;
}

.feedback-table .col-content {
  width: auto;
  text-align: center;
}

.col-time {
  white-space: nowrap;
  width: 1%;
}

.col-status {
  white-space: nowrap;
  width: 1%;
}

.content-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  text-align: center;
  cursor: default;
}

.col-actions {
  white-space: nowrap;
  width: 1%;
  text-align: center;
}

.btn-complete:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  filter: grayscale(0.35);
}

.btn-xs {
  padding: 5px 10px;
  font-size: 12px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: auto;
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

</style>
