import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const target = path.join(__dirname, '../src/views/LocationsView.vue')

const vue = `<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import LocationMapPicker from '@/components/LocationMapPicker.vue'
import {
  createLocation,
  deleteLocation,
  fetchLocations,
  updateLocation,
} from '@/api/admin'
import { mediaUrl, uploadFile } from '@/api/http'
import type { LocationItem, LocationUpsertPayload } from '@/types/api'
import { useToast } from '@/composables/useToast'

const { show } = useToast()

const loading = ref(false)
const saving = ref(false)
const uploadingImage = ref(false)
const uploadingAudio = ref(false)
const locations = ref<LocationItem[]>([])
const editingId = ref<number | null>(null)
const mapKey = ref(0)

const form = reactive({
  name: '',
  category: '',
  address: '',
  latitude: '',
  longitude: '',
  radiusMeters: '50',
  description: '',
  audioUrl: '',
  imageUrl: '',
})

onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    locations.value = await fetchLocations()
  } catch (e) {
    show(e instanceof Error ? e.message : '\u52a0\u8f7d\u8bb2\u89e3\u70b9\u5931\u8d25', 'error')
  } finally {
    loading.value = false
  }
}

async function selectItem(item: LocationItem) {
  editingId.value = item.id
  form.name = item.name
  form.category = item.category
  form.address = item.address ?? ''
  form.latitude = String(item.latitude)
  form.longitude = String(item.longitude)
  form.radiusMeters = String(item.radiusMeters)
  form.description = item.description
  form.audioUrl = item.audioUrl ?? ''
  form.imageUrl = item.imageUrl ?? ''
  mapKey.value += 1
  await nextTick()
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.category = ''
  form.address = ''
  form.latitude = ''
  form.longitude = ''
  form.radiusMeters = '50'
  form.description = ''
  form.audioUrl = ''
  form.imageUrl = ''
  mapKey.value += 1
}

function buildPayload(): LocationUpsertPayload | null {
  if (!form.name || !form.category || !form.latitude || !form.longitude || !form.radiusMeters || !form.description) {
    show('\u8bf7\u586b\u5199\u6240\u6709\u5fc5\u586b\u9879', 'error')
    return null
  }
  const lat = Number(form.latitude)
  const lng = Number(form.longitude)
  const radius = Number(form.radiusMeters)
  if (Number.isNaN(lat) || Number.isNaN(lng) || Number.isNaN(radius)) {
    show('\u7eac\u5ea6/\u7ecf\u5ea6/\u534a\u5f84\u683c\u5f0f\u9519\u8bef', 'error')
    return null
  }
  return {
    name: form.name.trim(),
    category: form.category.trim(),
    address: form.address.trim() || null,
    latitude: lat,
    longitude: lng,
    radiusMeters: radius,
    description: form.description.trim(),
    audioUrl: form.audioUrl.trim() || null,
    imageUrl: form.imageUrl.trim() || null,
  }
}

async function save() {
  const payload = buildPayload()
  if (!payload) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateLocation(editingId.value, payload)
      show('\u8bb2\u89e3\u70b9\u66f4\u65b0\u6210\u529f')
    } else {
      await createLocation(payload)
      show('\u8bb2\u89e3\u70b9\u521b\u5efa\u6210\u529f')
    }
    resetForm()
    await loadList()
  } catch (e) {
    show(e instanceof Error ? e.message : '\u4fdd\u5b58\u5931\u8d25', 'error')
  } finally {
    saving.value = false
  }
}

async function remove(item: LocationItem) {
  if (!window.confirm(\`\u786e\u5b9a\u5220\u9664\u300c\${item.name}\u300d\u5417\uff1f\`)) return
  try {
    await deleteLocation(item.id)
    show('\u5df2\u5220\u9664')
    if (editingId.value === item.id) resetForm()
    await loadList()
  } catch (e) {
    show(e instanceof Error ? e.message : '\u5220\u9664\u5931\u8d25', 'error')
  }
}

async function onImageSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingImage.value = true
  try {
    form.imageUrl = await uploadFile(file, 'image')
    show('\u56fe\u7247\u4e0a\u4f20\u6210\u529f')
  } catch (e) {
    show(e instanceof Error ? e.message : '\u56fe\u7247\u4e0a\u4f20\u5931\u8d25', 'error')
  } finally {
    uploadingImage.value = false
    input.value = ''
  }
}

async function onAudioSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingAudio.value = true
  try {
    form.audioUrl = await uploadFile(file, 'audio')
    show('\u8bed\u97f3\u4e0a\u4f20\u6210\u529f')
  } catch (e) {
    show(e instanceof Error ? e.message : '\u8bed\u97f3\u4e0a\u4f20\u5931\u8d25', 'error')
  } finally {
    uploadingAudio.value = false
    input.value = ''
  }
}
</script>

<template>
  <section class="locations-page">
    <p class="page-subtitle">\u6dfb\u52a0\u3001\u4fee\u6539\u8bb2\u89e3\u70b9\uff1a\u652f\u6301\u5730\u56fe\u9009\u70b9\u6216\u624b\u52a8\u8f93\u5165\u5750\u6807\uff0c\u53ef\u586b\u5199\u6587\u5b57\u4ecb\u7ecd\u5e76\u4e0a\u4f20\u8bed\u97f3</p>

    <section class="layout">
      <article class="card list-panel">
        <header class="panel-head">
          <h2>\u5730\u70b9\u5217\u8868</h2>
          <button type="button" class="btn btn-secondary btn-sm" :disabled="loading" @click="loadList">
            \u5237\u65b0
          </button>
        </header>

        <p v-if="loading" class="hint pad">\u52a0\u8f7d\u4e2d\u2026</p>
        <ul v-else class="location-list">
          <li
            v-for="item in locations"
            :key="item.id"
            class="location-item"
            :class="{ active: editingId === item.id }"
            @click="selectItem(item)"
          >
            <section class="item-main">
              <strong>{{ item.name }}</strong>
              <span class="meta">{{ item.category }} \u00b7 \u534a\u5f84 {{ item.radiusMeters }}m</span>
              <span class="meta">{{ item.latitude }}, {{ item.longitude }}</span>
            </section>
            <button type="button" class="btn btn-danger btn-xs" @click.stop="remove(item)">\u5220\u9664</button>
          </li>
        </ul>
        <p v-if="!loading && locations.length === 0" class="hint pad">\u6682\u65e0\u8bb2\u89e3\u70b9</p>
      </article>

      <article class="card form-panel">
        <header class="form-head">
          <h2>{{ editingId ? '\u7f16\u8f91\u8bb2\u89e3\u70b9' : '\u65b0\u589e\u8bb2\u89e3\u70b9' }}</h2>
          <button v-if="editingId" type="button" class="btn btn-secondary btn-sm" @click="resetForm">
            \u65b0\u5efa
          </button>
        </header>

        <form @submit.prevent="save">
          <section class="field">
            <label for="loc-name">\u540d\u79f0 <span class="req">*</span></label>
            <input id="loc-name" v-model="form.name" placeholder="\u5982\uff1a\u4e2d\u5fc3\u56fe\u4e66\u9986" />
          </section>

          <section class="field-row">
            <section class="field">
              <label for="loc-category">\u5206\u7c7b <span class="req">*</span></label>
              <input id="loc-category" v-model="form.category" placeholder="\u6559\u5b66\u697c / \u56fe\u4e66\u9986 / \u6821\u95e8" />
            </section>
            <section class="field">
              <label for="loc-address">\u5730\u5740</label>
              <input id="loc-address" v-model="form.address" placeholder="\u53ef\u9009" />
            </section>
          </section>

          <section class="field">
            <label>\u4f4d\u7f6e <span class="req">*</span></label>
            <LocationMapPicker
              :key="mapKey"
              :latitude="form.latitude"
              :longitude="form.longitude"
              @update:latitude="form.latitude = $event"
              @update:longitude="form.longitude = $event"
            />
          </section>

          <section class="field-row">
            <section class="field">
              <label for="loc-lat">\u7eac\u5ea6</label>
              <input id="loc-lat" v-model="form.latitude" type="number" step="any" placeholder="29.8215" />
            </section>
            <section class="field">
              <label for="loc-lng">\u7ecf\u5ea6</label>
              <input id="loc-lng" v-model="form.longitude" type="number" step="any" placeholder="106.4210" />
            </section>
          </section>

          <section class="field">
            <label for="loc-radius">\u89e6\u53d1\u534a\u5f84\uff08\u7c73\uff09<span class="req">*</span></label>
            <input id="loc-radius" v-model="form.radiusMeters" type="number" min="10" max="1000" />
          </section>

          <section class="field">
            <label for="loc-desc">\u6587\u5b57\u4ecb\u7ecd <span class="req">*</span></label>
            <textarea
              id="loc-desc"
              v-model="form.description"
              rows="5"
              placeholder="\u8bf7\u8f93\u5165\u8bb2\u89e3\u6587\u6848\uff0c\u6e38\u5ba2\u5230\u8fbe\u8be5\u5730\u70b9\u65f6\u5c06\u5c55\u793a\u6b64\u5185\u5bb9"
            />
          </section>

          <section class="field">
            <label>\u914d\u56fe\uff08\u53ef\u9009\uff09</label>
            <section class="upload-row">
              <label class="file-btn">
                \u9009\u62e9\u56fe\u7247
                <input type="file" accept="image/*" hidden @change="onImageSelected" />
              </label>
              <input v-model="form.imageUrl" placeholder="\u4e0a\u4f20\u540e\u81ea\u52a8\u586b\u5165\uff0c\u4e5f\u53ef\u7c98\u8d34\u56fe\u7247\u5730\u5740" />
            </section>
            <img v-if="form.imageUrl" :src="mediaUrl(form.imageUrl)" alt="\u914d\u56fe\u9884\u89c8" class="preview-img" />
            <p v-if="uploadingImage" class="hint">\u56fe\u7247\u4e0a\u4f20\u4e2d\u2026</p>
          </section>

          <section class="field">
            <label>\u97f3\u9891\u4ecb\u7ecd\uff08\u53ef\u9009\uff09</label>
            <section class="upload-row">
              <label class="file-btn">
                \u9009\u62e9\u97f3\u9891
                <input type="file" accept="audio/*" hidden @change="onAudioSelected" />
              </label>
              <input v-model="form.audioUrl" placeholder="\u4e0a\u4f20\u540e\u81ea\u52a8\u586b\u5165\uff0c\u4e5f\u53ef\u7c98\u8d34\u97f3\u9891\u5730\u5740" />
            </section>
            <audio v-if="form.audioUrl" :src="mediaUrl(form.audioUrl)" controls class="preview-audio" />
            <p v-if="uploadingAudio" class="hint">\u97f3\u9891\u4e0a\u4f20\u4e2d\u2026</p>
          </section>

          <section class="actions">
            <button type="submit" class="btn btn-login" :disabled="saving">
              {{ saving ? '\u4fdd\u5b58\u4e2d\u2026' : editingId ? '\u66f4\u65b0\u8bb2\u89e3\u70b9' : '\u4fdd\u5b58\u8bb2\u89e3\u70b9' }}
            </button>
            <button v-if="editingId" type="button" class="btn btn-secondary" @click="resetForm">\u53d6\u6d88\u7f16\u8f91</button>
          </section>
        </form>
      </article>
    </section>
  </section>
</template>

<style scoped>
.locations-page .layout {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(360px, 1.2fr);
  gap: 16px;
  align-items: start;
}

@media (max-width: 960px) {
  .locations-page .layout {
    grid-template-columns: 1fr;
  }
}

.list-panel,
.form-panel {
  padding: 18px;
}

.panel-head,
.form-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-head h2,
.form-head h2 {
  margin: 0;
  font-size: 16px;
}

.req {
  color: #c62828;
}

.location-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 640px;
  overflow-y: auto;
}

.location-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 6px;
}

.location-item:hover {
  background: rgba(253, 224, 141, 0.15);
}

.location-item.active {
  border-color: var(--brand-accent);
  background: rgba(155, 196, 176, 0.12);
}

.item-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.meta {
  font-size: 12px;
  color: var(--text-sub);
}

.btn-xs {
  padding: 4px 10px;
  font-size: 12px;
  flex-shrink: 0;
}

.upload-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  background: var(--surface-soft-alt);
  border: 1px solid var(--stroke-soft);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: var(--text-main);
}

.file-btn:hover {
  background: rgba(155, 196, 176, 0.2);
}

.preview-img {
  margin-top: 8px;
  max-width: 100%;
  max-height: 160px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}

.preview-audio {
  margin-top: 8px;
  width: 100%;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.pad {
  padding: 12px;
}

.hint {
  font-size: 12px;
  color: var(--text-sub);
  margin: 4px 0 0;
}
</style>
`

// Decode unicode escapes in template literals for template section only - the vue string uses \u in template part as literal backslash-u in file which is wrong.

fs.writeFileSync(target, vue.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))), 'utf8')
console.log('written', target)
