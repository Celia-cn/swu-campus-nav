import { ref } from 'vue'

const message = ref('')
const type = ref<'success' | 'error'>('success')
let timer: ReturnType<typeof setTimeout> | null = null

export function useToast() {
  function show(msg: string, kind: 'success' | 'error' = 'success') {
    message.value = msg
    type.value = kind
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      message.value = ''
    }, 2800)
  }

  return { message, type, show }
}
