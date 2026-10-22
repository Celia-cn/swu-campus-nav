<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { deleteUser, fetchStats, fetchUsers, updateUser } from '@/api/admin'
import FilterSuggestInput from '@/components/FilterSuggestInput.vue'
import type { AdminStats, UserSummary } from '@/types/api'
import { useToast } from '@/composables/useToast'

const PAGE_SIZE = 10

const { show } = useToast()

const loading = ref(true)
const statsLoading = ref(true)
const saving = ref(false)
const filterOpen = ref(true)
const users = ref<UserSummary[]>([])
const stats = ref<AdminStats | null>(null)
const currentPage = ref(1)

const showEdit = ref(false)
const editing = ref<UserSummary | null>(null)
const editForm = reactive({
  role: 'USER',
  password: '',
})

const draft = reactive({
  keyword: '',
  role: '',
  sortBy: 'createdAt' as 'createdAt' | 'username' | 'id',
  order: 'desc' as 'asc' | 'desc',
})

const applied = reactive({
  keyword: '',
  role: '',
  sortBy: 'createdAt' as 'createdAt' | 'username' | 'id',
  order: 'desc' as 'asc' | 'desc',
})

const usernameOptions = computed(() => {
  const set = new Set<string>()
  for (const user of users.value) {
    if (user.username?.trim()) set.add(user.username.trim())
  }
  return Array.from(set).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const filteredList = computed(() => {
  let list = [...users.value]

  const kw = applied.keyword.trim().toLowerCase()
  if (kw) {
    list = list.filter((user) => user.username.toLowerCase().includes(kw))
  }

  if (applied.role) {
    list = list.filter((user) => user.role === applied.role)
  }

  const dir = applied.order === 'asc' ? 1 : -1
  if (applied.sortBy === 'username') {
    list.sort((a, b) => a.username.localeCompare(b.username, 'zh-CN') * dir)
  } else if (applied.sortBy === 'id') {
    list.sort((a, b) => (a.id - b.id) * dir)
  } else {
    list.sort((a, b) => a.createdAt.localeCompare(b.createdAt) * dir)
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
  loadUsers()
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

async function loadUsers() {
  loading.value = true
  try {
    users.value = await fetchUsers()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载用户失败', 'error')
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadStats(), loadUsers()])
}

function applySearch() {
  applied.keyword = draft.keyword
  applied.role = draft.role
  applied.sortBy = draft.sortBy
  applied.order = draft.order
  currentPage.value = 1
}

function resetFilters() {
  draft.keyword = ''
  draft.role = ''
  draft.sortBy = 'createdAt'
  draft.order = 'desc'
  applied.keyword = ''
  applied.role = ''
  applied.sortBy = 'createdAt'
  applied.order = 'desc'
  currentPage.value = 1
}

function formatRole(role: string) {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

function formatTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

function openEdit(user: UserSummary) {
  editing.value = user
  editForm.role = user.role
  editForm.password = ''
  showEdit.value = true
}

function closeEdit() {
  showEdit.value = false
  editing.value = null
}

async function submitEdit() {
  if (!editing.value) return
  saving.value = true
  try {
    const payload: { role: string; password?: string } = { role: editForm.role }
    if (editForm.password.trim()) {
      payload.password = editForm.password.trim()
    }
    await updateUser(editing.value.id, payload)
    show('用户更新成功')
    closeEdit()
    await refreshAll()
  } catch (e) {
    show(e instanceof Error ? e.message : '更新失败', 'error')
  } finally {
    saving.value = false
  }
}

async function remove(user: UserSummary) {
  if (!window.confirm(`确定删除用户「${user.username}」吗？其游览记录将一并清除。`)) return
  try {
    await deleteUser(user.id)
    show('用户已删除')
    await refreshAll()
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  }
}
</script>

<template>
  <section class="users-page">
    <section class="data-screen card">
      <header class="screen-head">
        <h2>用户数据大屏</h2>
        <span class="screen-time">实时统计</span>
      </header>
      <section v-if="statsLoading" class="screen-grid screen-grid-3">
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
        <article class="screen-card loading-card">加载中…</article>
      </section>
      <section v-else class="screen-grid screen-grid-3">
        <article class="screen-card highlight">
          <span class="screen-label">今日新增用户总数</span>
          <strong class="screen-value">{{ stats?.todayNewUsers ?? 0 }}</strong>
          <span class="screen-unit">人</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">用户总量</span>
          <strong class="screen-value">{{ stats?.totalUsers ?? users.length }}</strong>
          <span class="screen-unit">人</span>
        </article>
        <article class="screen-card">
          <span class="screen-label">当日用户访问数量</span>
          <strong class="screen-value">{{ stats?.todayVisitors ?? 0 }}</strong>
          <span class="screen-unit">人</span>
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
            <label>用户名</label>
            <FilterSuggestInput v-model="draft.keyword" :options="usernameOptions" placeholder="关键词搜索" />
          </section>
          <section class="field field-role">
            <label>角色</label>
            <select v-model="draft.role">
              <option value="">全部</option>
              <option value="ADMIN">管理员</option>
              <option value="USER">普通用户</option>
            </select>
          </section>
          <section class="field field-sort">
            <label>排序方式</label>
            <select v-model="draft.sortBy">
              <option value="createdAt">注册时间</option>
              <option value="username">用户名</option>
              <option value="id">ID</option>
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
        <h2>用户列表</h2>
        <button type="button" class="btn-refresh" :disabled="loading" @click="refreshAll">刷新</button>
      </header>

      <p v-if="loading" class="hint pad">加载中…</p>
      <template v-else>
        <section v-if="pagedList.length" class="table-wrap">
          <table class="data-table user-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>角色</th>
                <th>注册时间</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in pagedList" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>
                  <span class="badge" :class="user.role === 'ADMIN' ? 'badge-admin' : 'badge-user'">
                    {{ formatRole(user.role) }}
                  </span>
                </td>
                <td>{{ formatTime(user.createdAt) }}</td>
                <td class="col-actions">
                  <button type="button" class="btn btn-login btn-xs" @click="openEdit(user)">编辑</button>
                  <button type="button" class="btn btn-danger btn-xs" @click="remove(user)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>
        <p v-else class="hint pad">暂无符合条件的用户</p>

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

    <Teleport to="body">
      <section v-if="showEdit && editing" class="modal-mask" @click.self="closeEdit">
        <article class="modal card">
          <h3>编辑用户 · {{ editing.username }}</h3>
          <form @submit.prevent="submitEdit">
            <section class="field">
              <label>角色</label>
              <select v-model="editForm.role">
                <option value="USER">普通用户</option>
                <option value="ADMIN">管理员</option>
              </select>
            </section>
            <section class="field">
              <label>新密码（留空则不修改）</label>
              <input v-model="editForm.password" type="password" placeholder="至少 6 位" autocomplete="new-password" />
            </section>
            <section class="modal-actions">
              <button type="button" class="btn btn-secondary" @click="closeEdit">取消</button>
              <button type="submit" class="btn btn-login" :disabled="saving">
                {{ saving ? '保存中…' : '保存' }}
              </button>
            </section>
          </form>
        </article>
      </section>
    </Teleport>
  </section>
</template>

<style scoped>
.users-page {
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
}

.screen-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.screen-time {
  font-size: 12px;
  color: var(--text-sub);
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
  position: relative;
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
  grid-template-columns: 1fr 120px minmax(118px, 160px) auto auto;
  align-items: end;
  gap: 12px;
  width: 100%;
}

.filter-panel .field {
  margin-bottom: 0;
  min-width: 0;
}

.field-sort select,
.field-role select {
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

.col-actions {
  white-space: nowrap;
  width: 1%;
}

.col-actions .btn + .btn {
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
  padding: 12px 20px;
}

.hint {
  font-size: 13px;
  color: var(--text-sub);
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(26, 31, 54, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.modal {
  width: min(400px, 100%);
  padding: 24px;
}

.modal h3 {
  margin: 0 0 20px;
  font-size: 18px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
</style>
