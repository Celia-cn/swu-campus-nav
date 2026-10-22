<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { saveRoute, fetchLocations } from '@/api/admin'
import type { WaypointRequest, LocationItem } from '@/types/api'
import { useToast } from '@/composables/useToast'

const { show } = useToast()
const router = useRouter()

const saving = ref(false)
const loading = ref(false)
const locations = ref<LocationItem[]>([])
const form = reactive({
  name: '',
  description: '',
  isPublic: false,
  waypoints: [] as WaypointRequest[]
})

onMounted(async () => {
  loading.value = true
  try {
    locations.value = await fetchLocations()
  } catch (e) {
    show('加载讲解点列表失败', 'error')
  } finally {
    loading.value = false
  }
})

function addWaypoint() {
  form.waypoints.push({
    latitude: 0,
    longitude: 0,
    locationId: undefined,
    stayDurationMinutes: 15,
    notes: ''
  })
}

function removeWaypoint(index: number) {
  form.waypoints.splice(index, 1)
}

function onLocationSelect(index: number, locationId: number) {
  const location = locations.value.find(l => l.id === locationId)
  if (location) {
    form.waypoints[index].locationId = location.id
    form.waypoints[index].latitude = location.latitude
    form.waypoints[index].longitude = location.longitude
  }
}

async function save() {
  if (!form.name) {
    show('请输入路线名称', 'error')
    return
  }

  if (form.waypoints.length < 2) {
    show('至少需要2个途经点', 'error')
    return
  }

  // 验证所有途经点都已选择地点
  for (let i = 0; i < form.waypoints.length; i++) {
    if (!form.waypoints[i].locationId) {
      show(`请选择第 ${i + 1} 个途经点的地点`, 'error')
      return
    }
  }

  saving.value = true
  try {
    // 构建符合后端要求的请求数据
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      isPublic: form.isPublic,
      waypoints: form.waypoints.map(wp => ({
        locationId: wp.locationId,
        latitude: wp.latitude,
        longitude: wp.longitude,
        stayDurationMinutes: wp.stayDurationMinutes || 15,
        notes: wp.notes?.trim() || undefined
      }))
    }

    console.log('发送保存请求:', JSON.stringify(payload, null, 2))

    await saveRoute(payload)
    show('路线保存成功')
    router.push('/routes')
  } catch (e: any) {
    console.error('保存失败:', e)
    const errorMsg = e?.response?.data?.message || e?.message || '保存失败'
    show(errorMsg, 'error')
  } finally {
    saving.value = false
  }
}

function getLocationCategory(locationId?: number) {
  if (!locationId) return ''
  const loc = locations.value.find(l => l.id === locationId)
  return loc ? loc.category : ''
}
</script>

<template>
  <section class="form-page">
    <article class="card form-panel">
      <h2>新建路线</h2>

      <form @submit.prevent="save">
        <section class="field">
          <label>路线名称 <span class="req">*</span></label>
          <input
              v-model="form.name"
              placeholder="如：校园一日游"
              maxlength="100"
          />
        </section>

        <section class="field">
          <label>描述</label>
          <textarea
              v-model="form.description"
              rows="3"
              placeholder="路线描述（可选）"
              maxlength="500"
          />
        </section>

        <section class="field checkbox-field">
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.isPublic" />
            <span>公开此路线</span>
          </label>
        </section>

        <section class="field">
          <label>途经点 <span class="req">*</span> <small>（至少2个）</small></label>

          <div v-if="loading" class="hint">加载讲解点列表中…</div>

          <div v-else class="waypoints-list">
            <div v-for="(wp, index) in form.waypoints" :key="index" class="waypoint-item">
              <div class="waypoint-order">{{ index + 1 }}</div>

              <div class="waypoint-fields">
                <select
                    :value="wp.locationId ?? ''"
                    @change="onLocationSelect(index, Number(($event.target as HTMLSelectElement).value))"
                    class="location-select"
                >
                  <option value="" disabled>请选择地点</option>
                  <option v-for="loc in locations" :key="loc.id" :value="loc.id">
                    {{ loc.name }}（{{ loc.category }}）
                  </option>
                </select>

                <div class="waypoint-details" v-if="wp.locationId">
                  <span class="detail-tag">{{ getLocationCategory(wp.locationId) }}</span>
                  <span class="detail-coords">
                    {{ form.waypoints[index].latitude.toFixed(6) }},
                    {{ form.waypoints[index].longitude.toFixed(6) }}
                  </span>
                </div>

                <div class="waypoint-extra">
                  <input
                      v-model.number="wp.stayDurationMinutes"
                      type="number"
                      min="5"
                      max="120"
                      placeholder="停留分钟"
                      class="input-small"
                  />
                  <input
                      v-model="wp.notes"
                      type="text"
                      placeholder="备注（可选）"
                      maxlength="500"
                      class="input-notes"
                  />
                </div>
              </div>

              <button
                  type="button"
                  class="btn-remove"
                  @click="removeWaypoint(index)"
                  :disabled="form.waypoints.length <= 2"
                  title="删除途经点"
              >
                ×
              </button>
            </div>

            <button
                type="button"
                class="btn btn-secondary btn-add"
                @click="addWaypoint"
            >
              + 添加途经点
            </button>
          </div>
        </section>

        <section class="actions">
          <button
              type="submit"
              class="btn btn-login"
              :disabled="saving || loading"
          >
            {{ saving ? '保存中…' : '保存路线' }}
          </button>
          <button
              type="button"
              class="btn btn-secondary"
              @click="router.push('/routes')"
          >
            取消
          </button>
        </section>
      </form>
    </article>
  </section>
</template>

<style scoped>
.form-page {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
}

.form-panel {
  padding: 28px;
}

.form-panel h2 {
  margin: 0 0 24px;
  font-size: 20px;
  font-weight: 700;
}

.req {
  color: #c62828;
}

.field {
  margin-bottom: 20px;
}

.field label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  font-size: 14px;
}

.field label small {
  font-weight: 400;
  color: var(--text-sub);
  font-size: 12px;
}

.field input[type="text"],
.field input[type="number"],
.field textarea,
.field select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--stroke-soft);
  border-radius: 6px;
  font-size: 14px;
  background: white;
  transition: border-color 0.15s;
}

.field input:focus,
.field textarea:focus,
.field select:focus {
  outline: none;
  border-color: var(--brand-accent);
}

.checkbox-field {
  margin-bottom: 24px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 500;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.waypoints-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.waypoint-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 16px;
  background: rgba(155, 196, 176, 0.08);
  border-radius: 8px;
  border: 1px solid rgba(155, 196, 176, 0.2);
}

.waypoint-order {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
  margin-top: 8px;
}

.waypoint-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.location-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--stroke-soft);
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.location-select:focus {
  outline: none;
  border-color: var(--brand-accent);
}

.waypoint-details {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.detail-tag {
  padding: 3px 10px;
  background: rgba(253, 224, 141, 0.3);
  border-radius: 12px;
  font-size: 12px;
  color: var(--brand-accent-dark);
  font-weight: 500;
}

.detail-coords {
  font-size: 12px;
  color: var(--text-sub);
  font-family: monospace;
}

.waypoint-extra {
  display: flex;
  gap: 10px;
}

.input-small {
  width: 120px;
}

.input-notes {
  flex: 1;
}

.btn-remove {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #ffebee;
  color: #c62828;
  font-size: 20px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
  margin-top: 4px;
}

.btn-remove:hover:not(:disabled) {
  background: #ffcdd2;
}

.btn-remove:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-add {
  margin-top: 8px;
  padding: 10px;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--divider-color);
}

.actions .btn {
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
}

.hint {
  font-size: 13px;
  color: var(--text-sub);
  padding: 12px;
  background: rgba(155, 196, 176, 0.08);
  border-radius: 6px;
}

@media (max-width: 768px) {
  .form-panel {
    padding: 20px;
  }

  .waypoint-item {
    flex-direction: column;
  }

  .waypoint-extra {
    flex-direction: column;
  }

  .input-small,
  .input-notes {
    width: 100%;
  }

  .actions {
    flex-direction: column;
  }

  .actions .btn {
    width: 100%;
  }
}
</style>
