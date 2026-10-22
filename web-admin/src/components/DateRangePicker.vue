<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    from: string
    to: string
    placeholder?: string
  }>(),
  {
    placeholder: '请选择时间段',
  },
)

const emit = defineEmits<{
  'update:from': [value: string]
  'update:to': [value: string]
}>()

const open = ref(false)
const panelRef = ref<HTMLElement | null>(null)
const triggerRef = ref<HTMLButtonElement | null>(null)
const draftFrom = ref('')
const draftTo = ref('')

const displayText = computed(() => {
  if (props.from && props.to) {
    return `${props.from} ~ ${props.to}`
  }
  if (props.from) {
    return `${props.from} 起`
  }
  if (props.to) {
    return `至 ${props.to}`
  }
  return props.placeholder
})

const hasValue = computed(() => Boolean(props.from || props.to))

watch(open, (visible) => {
  if (visible) {
    draftFrom.value = props.from
    draftTo.value = props.to
  }
})

function togglePanel() {
  open.value = !open.value
}

function closePanel() {
  open.value = false
}

function onDocumentClick(event: MouseEvent) {
  if (!open.value) return
  const target = event.target as Node
  if (panelRef.value?.contains(target) || triggerRef.value?.contains(target)) {
    return
  }
  closePanel()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closePanel()
  }
}

function clearRange() {
  draftFrom.value = ''
  draftTo.value = ''
  emit('update:from', '')
  emit('update:to', '')
  closePanel()
}

function formatDateKey(date: Date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function pickLastWeek() {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - 6)
  draftFrom.value = formatDateKey(from)
  draftTo.value = formatDateKey(to)
}

function applyRange() {
  if (draftFrom.value && draftTo.value && draftFrom.value > draftTo.value) {
    const temp = draftFrom.value
    draftFrom.value = draftTo.value
    draftTo.value = temp
  }
  emit('update:from', draftFrom.value)
  emit('update:to', draftTo.value)
  closePanel()
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <section class="range-wrap">
    <button
      ref="triggerRef"
      type="button"
      class="range-trigger"
      :class="{ filled: hasValue, open }"
      @click="togglePanel"
    >
      <span class="range-text" :class="{ placeholder: !hasValue }">{{ displayText }}</span>
      <span class="range-icon" aria-hidden="true" />
    </button>

    <section v-if="open" ref="panelRef" class="range-panel" role="dialog" aria-label="选择时间段">
      <header class="panel-head">选择时间段</header>
      <section class="date-fields">
        <label class="date-field">
          <span>开始日期</span>
          <input v-model="draftFrom" type="date" class="date-input" />
        </label>
        <span class="date-sep">至</span>
        <label class="date-field">
          <span>结束日期</span>
          <input v-model="draftTo" type="date" class="date-input" :min="draftFrom || undefined" />
        </label>
      </section>
      <footer class="panel-actions">
        <button type="button" class="btn btn-secondary btn-sm preset-btn" @click="pickLastWeek">近一周</button>
        <button type="button" class="btn btn-secondary btn-sm" @click="clearRange">清空</button>
        <button type="button" class="btn btn-login btn-sm" @click="applyRange">确定</button>
      </footer>
    </section>
  </section>
</template>

<style scoped>
.range-wrap {
  position: relative;
  width: max-content;
  max-width: 100%;
}

.range-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: max-content;
  max-width: 100%;
  padding: 10px 12px;
  border: 1px solid #e8ecf2;
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--text-main);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.range-trigger:hover {
  border-color: var(--brand-accent);
}

.range-trigger.open,
.range-trigger:focus-visible {
  outline: none;
  border-color: var(--brand-accent);
  box-shadow: 0 0 0 3px rgba(155, 196, 176, 0.25);
}

.range-text {
  flex: 0 1 auto;
  white-space: nowrap;
  font-size: 14px;
}

.range-text.placeholder {
  color: #9aa3b8;
}

.range-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  opacity: 0.55;
  background: currentColor;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='black' stroke-width='2'%3E%3Crect x='3' y='4' width='18' height='18' rx='2'/%3E%3Cpath d='M16 2v4M8 2v4M3 10h18'/%3E%3C/svg%3E")
    center / contain no-repeat;
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='black' stroke-width='2'%3E%3Crect x='3' y='4' width='18' height='18' rx='2'/%3E%3Cpath d='M16 2v4M8 2v4M3 10h18'/%3E%3C/svg%3E")
    center / contain no-repeat;
}

.range-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 30;
  min-width: min(100%, 320px);
  padding: 14px;
  background: #fff;
  border: 1px solid var(--stroke-soft);
  border-radius: var(--radius-md);
  box-shadow: 0 12px 32px rgba(26, 31, 54, 0.12);
}

.panel-head {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.date-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.date-field span {
  font-size: 12px;
  color: var(--text-sub);
}

.date-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e8ecf2;
  border-radius: var(--radius-sm);
  font: inherit;
  color: var(--text-main);
  background: #fff;
}

.date-input:focus {
  outline: none;
  border-color: var(--brand-accent);
  box-shadow: 0 0 0 2px rgba(155, 196, 176, 0.2);
}

.date-sep {
  align-self: center;
  font-size: 13px;
  color: var(--text-sub);
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eef2f0;
}

.preset-btn {
  margin-right: auto;
}

.btn-sm {
  padding: 7px 14px;
  font-size: 13px;
}
</style>
