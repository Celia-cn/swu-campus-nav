<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchStats } from '@/api/admin'
import type { AdminStats } from '@/types/api'
import { useToast } from '@/composables/useToast'

const { show } = useToast()
const loading = ref(true)
const stats = ref<AdminStats | null>(null)

const hotTopFive = computed(() => {
  if (!stats.value?.hotLocations?.length) return []
  return [...stats.value.hotLocations]
    .sort((a, b) => b.playCount - a.playCount)
    .slice(0, 5)
})

const hourStats = computed(() => {
  const list = stats.value?.visitHourStats
  if (list?.length) return list
  return Array.from({ length: 24 }, (_, hour) => ({ hour, visitorCount: 0 }))
})

const chartMax = computed(() => {
  const max = Math.max(0, ...hourStats.value.map((item) => item.visitorCount))
  return max > 0 ? max : 1
})

const yAxisTicks = computed(() => {
  const max = chartMax.value
  const step = max <= 5 ? 1 : Math.ceil(max / 4)
  const ticks: number[] = []
  for (let v = 0; v <= max; v += step) {
    ticks.push(v)
  }
  if (ticks[ticks.length - 1] !== max) {
    ticks.push(max)
  }
  return [...new Set(ticks)].sort((a, b) => a - b)
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    stats.value = await fetchStats()
  } catch (e) {
    show(e instanceof Error ? e.message : '加载统计失败', 'error')
  } finally {
    loading.value = false
  }
}

const CHART_WIDTH = 560
const CHART_HEIGHT = 220
const CHART_PAD = { top: 14, right: 10, bottom: 26, left: 6 }

const linePoints = computed(() => {
  const plotW = CHART_WIDTH - CHART_PAD.left - CHART_PAD.right
  const plotH = CHART_HEIGHT - CHART_PAD.top - CHART_PAD.bottom
  const max = chartMax.value

  return hourStats.value.map((item, index) => {
    const x = CHART_PAD.left + (index / 23) * plotW
    const y = CHART_PAD.top + (1 - item.visitorCount / max) * plotH
    return { x, y, hour: item.hour, count: item.visitorCount }
  })
})

const smoothLinePath = computed(() => {
  const points = linePoints.value
  if (points.length === 0) return ''
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`

  let path = `M ${points[0].x} ${points[0].y}`
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i]
    const p1 = points[i + 1]
    const cx = (p0.x + p1.x) / 2
    path += ` C ${cx} ${p0.y}, ${cx} ${p1.y}, ${p1.x} ${p1.y}`
  }
  return path
})

const gridLines = computed(() => {
  const plotH = CHART_HEIGHT - CHART_PAD.top - CHART_PAD.bottom
  const max = chartMax.value
  return yAxisTicks.value.map((tick) => {
    const y = CHART_PAD.top + (1 - tick / max) * plotH
    return { y, tick }
  })
})

function formatHour(hour: number) {
  return `${String(hour).padStart(2, '0')}:00`
}

function formatXLabel(hour: number) {
  return String(hour)
}
</script>

<template>
  <section class="dashboard-page">
    <p v-if="loading" class="hint pad">加载中…</p>

    <template v-else-if="stats">
      <section class="data-screen card">
        <header class="screen-head">
          <h2>数据概览</h2>
        </header>
        <section class="screen-grid screen-grid-5">
          <article class="screen-card highlight">
            <span class="screen-label">今日访问人数</span>
            <strong class="screen-value">{{ stats?.todayVisitors }}</strong>
            <span class="screen-unit">人</span>
          </article>
          <article class="screen-card">
            <span class="screen-label">今日讲解次数</span>
            <strong class="screen-value">{{ stats?.todayPlayCount }}</strong>
            <span class="screen-unit">次</span>
          </article>
          <article class="screen-card">
            <span class="screen-label">讲解总次数</span>
            <strong class="screen-value">{{ stats?.totalPlayCount }}</strong>
            <span class="screen-unit">次</span>
          </article>
          <article class="screen-card">
            <span class="screen-label">讲解点总数</span>
            <strong class="screen-value">{{ stats?.totalLocations }}</strong>
            <span class="screen-unit">个</span>
          </article>
          <article class="screen-card">
            <span class="screen-label">自动 / 手动讲解</span>
            <strong class="screen-value screen-value-sub">{{ stats?.autoPlayCount }} / {{ stats?.manualPlayCount }}</strong>
          </article>
        </section>
      </section>

      <section class="dashboard-split">
        <article class="card rank-card">
          <h2 class="section-title">热门景点排行榜</h2>
          <ol v-if="hotTopFive.length" class="hot-rank-list">
            <li v-for="(item, index) in hotTopFive" :key="item.locationId">
              <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="name" :title="item.locationName">{{ item.locationName }}</span>
              <span class="count">{{ item.playCount }} 次</span>
            </li>
          </ol>
          <p v-else class="hint empty-hint">暂无播放记录</p>
        </article>

        <article class="card chart-card">
          <h2 class="section-title">数据可视化 · 访问时段</h2>
          <p class="chart-desc">横轴：时间（时）　纵轴：访问人数（人）</p>
          <section v-if="hourStats.length" class="chart-wrap">
            <section class="chart-y-axis" aria-hidden="true">
              <span v-for="tick in [...yAxisTicks].reverse()" :key="tick" class="y-tick">
                {{ tick === 0 ? '' : tick }}
              </span>
            </section>
            <section class="chart-main">
              <svg
                class="line-chart"
                :viewBox="`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`"
                preserveAspectRatio="none"
                role="img"
                aria-label="各时段访问人数折线图（含同一用户多次访问）"
              >
                <line
                  v-for="line in gridLines"
                  :key="line.tick"
                  :x1="CHART_PAD.left"
                  :y1="line.y"
                  :x2="CHART_WIDTH - CHART_PAD.right"
                  :y2="line.y"
                  class="grid-line"
                />
                <line
                  :x1="CHART_PAD.left"
                  :y1="CHART_HEIGHT - CHART_PAD.bottom"
                  :x2="CHART_WIDTH - CHART_PAD.right"
                  :y2="CHART_HEIGHT - CHART_PAD.bottom"
                  class="axis-line"
                />
                <line
                  :x1="CHART_PAD.left"
                  :y1="CHART_PAD.top"
                  :x2="CHART_PAD.left"
                  :y2="CHART_HEIGHT - CHART_PAD.bottom"
                  class="axis-line"
                />
                <path :d="smoothLinePath" class="line-path" />
                <circle
                  v-for="point in linePoints"
                  :key="point.hour"
                  :cx="point.x"
                  :cy="point.y"
                  r="3.5"
                  class="line-dot"
                >
                  <title>{{ formatHour(point.hour) }}：{{ point.count }} 次访问</title>
                </circle>
                <text
                  v-for="point in linePoints"
                  :key="`x-${point.hour}`"
                  :x="point.x"
                  :y="CHART_HEIGHT - 6"
                  text-anchor="middle"
                  class="x-axis-label"
                >
                  {{ formatXLabel(point.hour) }}
                </text>
              </svg>
            </section>
          </section>
          <p v-else class="hint empty-hint">暂无访问数据</p>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dashboard-page {
  width: 100%;
}

.pad {
  padding: 12px 0;
}

.hint {
  color: var(--text-sub);
  font-size: 13px;
}

.empty-hint {
  padding: 24px 0;
  text-align: center;
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

.screen-grid {
  display: grid;
  gap: 16px;
  padding: 16px 20px 20px;
}

.screen-grid-5 {
  grid-template-columns: repeat(5, 1fr);
}

@media (max-width: 1100px) {
  .screen-grid-5 {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .screen-grid-5 {
    grid-template-columns: 1fr 1fr;
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

.screen-value-sub {
  font-size: 32px;
}

.screen-unit {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-sub);
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 700;
}

.dashboard-split {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 20px;
  align-items: stretch;
}

@media (max-width: 960px) {
  .dashboard-split {
    grid-template-columns: 1fr;
  }
}

.rank-card,
.chart-card {
  padding: 20px;
  min-height: 360px;
}

.hot-rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.hot-rank-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f0;
}

.hot-rank-list li:last-child {
  border-bottom: none;
}

.rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eef2f0;
  color: var(--text-sub);
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank.top {
  background: linear-gradient(135deg, rgba(253, 224, 141, 0.9), rgba(155, 196, 176, 0.85));
  color: var(--brand-accent-dark);
}

.name {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.count {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-sub);
  font-weight: 600;
}

.chart-desc {
  margin: -8px 0 16px;
  font-size: 12px;
  color: var(--text-sub);
}

.chart-wrap {
  display: flex;
  gap: 8px;
  height: 260px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 28px;
  padding-bottom: 26px;
  flex-shrink: 0;
}

.y-tick {
  font-size: 11px;
  color: var(--text-sub);
  text-align: right;
  line-height: 1;
}

.chart-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.line-chart {
  width: 100%;
  height: 240px;
  display: block;
}

.grid-line {
  stroke: #e8ecf2;
  stroke-width: 1;
  stroke-dasharray: 4 4;
}

.axis-line {
  stroke: #d8dde6;
  stroke-width: 1;
}

.line-path {
  fill: none;
  stroke: #d32f2f;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
}

.line-dot {
  fill: #d32f2f;
  stroke: #fff;
  stroke-width: 1.5;
  vector-effect: non-scaling-stroke;
}

.x-axis-label {
  font-size: 10px;
  fill: #5c6583;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>
