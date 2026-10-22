<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    options: string[]
    placeholder?: string
    inputId?: string
  }>(),
  {
    placeholder: '关键词搜索',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const open = ref(false)

const suggestions = computed(() => {
  const q = props.modelValue.trim().toLowerCase()
  if (!q) return []
  return props.options.filter((item) => item.toLowerCase().includes(q)).slice(0, 5)
})

const showList = computed(() => open.value && suggestions.value.length > 0)

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
  open.value = true
}

function onFocus() {
  if (props.modelValue.trim() && suggestions.value.length) {
    open.value = true
  }
}

function onBlur() {
  window.setTimeout(() => {
    open.value = false
  }, 150)
}

function pick(option: string) {
  emit('update:modelValue', option)
  open.value = false
}
</script>

<template>
  <section class="suggest-wrap">
    <input
      :id="inputId"
      type="text"
      class="suggest-input"
      :value="modelValue"
      :placeholder="placeholder"
      autocomplete="off"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
    />
    <ul v-if="showList" class="suggest-list" role="listbox">
      <li
        v-for="item in suggestions"
        :key="item"
        role="option"
        @mousedown.prevent="pick(item)"
      >
        {{ item }}
      </li>
    </ul>
  </section>
</template>

<style scoped>
.suggest-wrap {
  position: relative;
  width: 100%;
}

.suggest-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e8ecf2;
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--text-main);
}

.suggest-input:focus {
  outline: none;
  border-color: var(--brand-accent);
  box-shadow: 0 0 0 3px rgba(155, 196, 176, 0.25);
}

.suggest-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 20;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border: 1px solid var(--stroke-soft);
  border-radius: var(--radius-sm);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.1);
  max-height: 220px;
  overflow-y: auto;
}

.suggest-list li {
  padding: 9px 12px;
  font-size: 13px;
  color: var(--text-main);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.suggest-list li:hover {
  background: rgba(155, 196, 176, 0.2);
}
</style>
