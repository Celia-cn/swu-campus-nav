<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

/** 西南大学北碚校区大致坐标 */
const SWU_LAT = 29.563
const SWU_LON = 106.437

const now = ref(new Date())
const weatherLoading = ref(true)
const weatherText = ref('')
const weatherTemp = ref<number | null>(null)

const dateLine = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
    timeZone: 'Asia/Shanghai',
  }).format(now.value),
)

const timeLine = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: 'Asia/Shanghai',
  }).format(now.value),
)

let clockTimer: ReturnType<typeof setInterval> | null = null
let weatherTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 60_000)
  loadWeather()
  weatherTimer = setInterval(loadWeather, 30 * 60_000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (weatherTimer) clearInterval(weatherTimer)
})

function weatherLabel(code: number) {
  if (code === 0) return '晴'
  if (code <= 3) return '多云'
  if (code <= 48) return '雾'
  if (code <= 57) return '毛毛雨'
  if (code <= 67) return '雨'
  if (code <= 77) return '雪'
  if (code <= 82) return '阵雨'
  if (code <= 86) return '阵雪'
  if (code <= 99) return '雷雨'
  return '阴'
}

async function loadWeather() {
  weatherLoading.value = true
  try {
    const url =
      `https://api.open-meteo.com/v1/forecast?latitude=${SWU_LAT}&longitude=${SWU_LON}` +
      '&current=temperature_2m,weather_code&timezone=Asia%2FShanghai'
    const res = await fetch(url)
    if (!res.ok) throw new Error('weather fetch failed')
    const data = (await res.json()) as {
      current?: { temperature_2m?: number; weather_code?: number }
    }
    const temp = data.current?.temperature_2m
    const code = data.current?.weather_code ?? -1
    weatherTemp.value = typeof temp === 'number' ? Math.round(temp) : null
    weatherText.value = weatherLabel(code)
  } catch {
    weatherText.value = ''
    weatherTemp.value = null
  } finally {
    weatherLoading.value = false
  }
}
</script>

<template>
  <section class="header-meta" aria-label="日期与西南大学天气">
    <p class="meta-date">{{ dateLine }}</p>
    <p class="meta-time">{{ timeLine }}</p>
    <p class="meta-weather">
      <span class="meta-place">西南大学 · 北碚</span>
      <span v-if="weatherLoading" class="weather-loading">天气加载中…</span>
      <span v-else-if="weatherText && weatherTemp !== null" class="weather-value">
        {{ weatherText }} {{ weatherTemp }}°C
      </span>
      <span v-else class="weather-loading">天气暂不可用</span>
    </p>
  </section>
</template>

<style scoped>
.header-meta {
  flex-shrink: 0;
  text-align: right;
  padding: 0;
  /* 略向左下偏移，避免挡住右上角背景树叶 */
  margin: 12px 56px 0 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.meta-date {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.4;
  text-shadow: 0 1px 3px rgba(255, 254, 254, 0.95);
}

.meta-time {
  margin: 2px 0 6px;
  font-size: 13px;
  color: var(--text-sub);
  font-variant-numeric: tabular-nums;
  text-shadow: 0 1px 3px rgba(255, 254, 254, 0.95);
}

.meta-weather {
  margin: 0;
  font-size: 13px;
  color: var(--text-sub);
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  align-items: center;
}

.meta-place {
  color: var(--brand-accent-dark);
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(255, 254, 254, 0.95);
}

.weather-value {
  color: var(--text-main);
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(255, 254, 254, 0.95);
}

.weather-loading {
  color: var(--text-sub);
  text-shadow: 0 1px 3px rgba(255, 254, 254, 0.95);
}
</style>
