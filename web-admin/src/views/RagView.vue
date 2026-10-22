<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { createRagDocument, deleteRagDocument, fetchRagDocuments, updateRagDocument } from '@/api/rag'
import type { RagCategory, RagDocument, RagUpsertPayload } from '@/types/api'
import { useToast } from '@/composables/useToast'

const { show } = useToast()

const loading = ref(true)
const saving = ref(false)
const list = ref<RagDocument[]>([])
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const categoryOptions: { value: RagCategory; label: string }[] = [
  { value: 'HISTORY', label: '校史' },
  { value: 'NEWS', label: '资讯' },
  { value: 'FOOD', label: '周边美食' },
  { value: 'BUS', label: '校车交通' },
  { value: 'ROUTE', label: '前往方式' },
  { value: 'GENERAL', label: '综合' },
]

const form = reactive<RagUpsertPayload>({
  title: '',
  category: 'GENERAL',
  content: '',
  enabled: true,
})

function categoryLabel(cat: RagCategory) {
  return categoryOptions.find((o) => o.value === cat)?.label ?? cat
}

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

async function loadList() {
  loading.value = true
  try {
    list.value = await fetchRagDocuments()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.title = ''
  form.category = 'GENERAL'
  form.content = ''
  form.enabled = true
  modalOpen.value = true
}

function openEdit(item: RagDocument) {
  editingId.value = item.id
  form.title = item.title
  form.category = item.category
  form.content = item.content
  form.enabled = item.enabled
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) {
    show('请填写标题和内容', 'error')
    return
  }
  saving.value = true
  try {
    const payload: RagUpsertPayload = {
      title: form.title.trim(),
      category: form.category,
      content: form.content.trim(),
      enabled: form.enabled,
    }
    if (editingId.value) {
      await updateRagDocument(editingId.value, payload)
      show('已更新')
    } else {
      await createRagDocument(payload)
      show('已创建')
    }
    closeModal()
    await loadList()
  } catch (e) {
    show(e instanceof Error ? e.message : '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function remove(item: RagDocument) {
  if (!window.confirm(`确定删除「${item.title}」吗？`)) return
  try {
    await deleteRagDocument(item.id)
    show('已删除')
    await loadList()
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  }
}

onMounted(loadList)
</script>

<template>
  <article class="card rag-page">
    <header class="panel-head">
      <section>
        <h2>AI 知识库（RAG）</h2>
        <p class="hint">管理 DeepSeek 对话检索的知识条目，供手机端 AI 助手回答校史、资讯、美食、校车与路线等问题。</p>
      </section>
      <section class="head-actions">
        <button type="button" class="btn-refresh" :disabled="loading" @click="loadList">刷新</button>
        <button type="button" class="btn btn-login" @click="openCreate">新增条目</button>
      </section>
    </header>

    <p v-if="loading" class="hint pad">加载中…</p>
    <section v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>分类</th>
            <th>状态</th>
            <th>更新时间</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.title }}</td>
            <td>{{ categoryLabel(item.category) }}</td>
            <td>
              <span class="badge" :class="item.enabled ? 'badge-completed' : 'badge-pending'">
                {{ item.enabled ? '启用' : '停用' }}
              </span>
            </td>
            <td>{{ formatTime(item.updatedAt) }}</td>
            <td class="col-actions">
              <button type="button" class="btn btn-login btn-xs" @click="openEdit(item)">编辑</button>
              <button type="button" class="btn btn-secondary btn-xs" @click="remove(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!list.length" class="empty-hint">暂无知识条目，可点击「新增条目」或使用后端种子数据。</p>
    </section>

    <Teleport to="body">
      <section v-if="modalOpen" class="modal-backdrop" @click.self="closeModal">
        <article class="modal card">
          <header class="modal-head">
            <h3>{{ editingId ? '编辑知识条目' : '新增知识条目' }}</h3>
            <button type="button" class="modal-close" aria-label="关闭" @click="closeModal">×</button>
          </header>
          <form class="modal-form" @submit.prevent="save">
            <div class="field">
              <label>标题</label>
              <input v-model="form.title" maxlength="200" required />
            </div>
            <div class="field">
              <label>分类</label>
              <select v-model="form.category">
                <option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div class="field">
              <label>
                <input v-model="form.enabled" type="checkbox" />
                启用（参与 AI 检索）
              </label>
            </div>
            <div class="field">
              <label>内容</label>
              <textarea v-model="form.content" rows="12" required />
            </div>
            <footer class="modal-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
              <button type="submit" class="btn btn-login" :disabled="saving">
                {{ saving ? '保存中…' : '保存' }}
              </button>
            </footer>
          </form>
        </article>
      </section>
    </Teleport>
  </article>
</template>

<style scoped>
.rag-page {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-refresh {
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

.col-actions {
  white-space: nowrap;
  width: 1%;
}

.col-actions .btn + .btn {
  margin-left: 6px;
}

.btn-xs {
  padding: 5px 10px;
  font-size: 12px;
}

.empty-hint {
  margin: 16px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(26, 31, 54, 0.45);
}

.modal {
  width: min(640px, 100%);
  max-height: min(90vh, 720px);
  overflow: auto;
  padding: 20px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.modal-head h3 {
  margin: 0;
  font-size: 16px;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--surface-soft-alt);
  font-size: 22px;
  cursor: pointer;
}

.modal-form textarea {
  width: 100%;
  min-height: 200px;
  resize: vertical;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>
