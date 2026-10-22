<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useToast } from '@/composables/useToast'

const { message, type } = useToast()
</script>

<template>
  <RouterView />
  <Transition name="fade">
    <div v-if="message" class="toast" :class="type === 'success' ? 'toast-success' : 'toast-error'">
      {{ message }}
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
