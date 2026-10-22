<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LocationMapPicker from '@/components/LocationMapPicker.vue'
import {
  createLocation,
  deleteLocationAudio,
  deleteLocationImage,
  fetchAdminLocations,
  updateLocation,
  uploadLocationAudio,
  uploadLocationImage,
} from '@/api/admin'
import { mediaUrl } from '@/api/http'
import type { LocationUpsertPayload } from '@/types/api'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const { show } = useToast()

const mapRef = ref<InstanceType<typeof LocationMapPicker> | null>(null)
const mapKey = ref(0)
const saving = ref(false)
const loading = ref(false)
const uploadingImage = ref(false)
const uploadingAudio = ref(false)
const deletingAudio = ref(false)

const imageUrls = ref<string[]>([])
const audioUrl = ref('')

const editingId = computed(() => {
  const raw = route.params.id
  if (raw == null || raw === '') return null
  const id = Number(raw)
  return Number.isFinite(id) ? id : null
})

const isEdit = computed(() => editingId.value != null)
const canManageMedia = computed(() => isEdit.value && editingId.value != null)

const form = reactive({
  name: '',
  category: '',
  address: '',
  latitude: '',
  longitude: '',
  radiusMeters: '50',
  cooldownSeconds: '120',
  description: '',
})

onMounted(async () => {
  if (isEdit.value && editingId.value) {
    await loadLocation(editingId.value)
  }
  await nextTick()
  mapRef.value?.refreshMapSize()
})

async function loadLocation(id: number) {
  loading.value = true
  try {
    const list = await fetchAdminLocations()
    const item = list.find((loc) => loc.id === id)
    if (!item) {
      show('讲解点不存在', 'error')
      router.replace('/locations')
      return
    }
    form.name = item.name
    form.category = item.category
    form.address = item.address ?? ''
    form.latitude = String(item.latitude)
    form.longitude = String(item.longitude)
    form.radiusMeters = String(item.radiusMeters)
    form.cooldownSeconds = String(item.cooldownSeconds ?? 120)
    form.description = item.description
    imageUrls.value = item.imageUrls?.length
      ? [...item.imageUrls]
      : item.imageUrl
        ? [item.imageUrl]
        : []
    audioUrl.value = item.audioUrl ?? ''
    mapKey.value += 1
  } catch (e) {
    show(e instanceof Error ? e.message : '加载失败', 'error')
    router.replace('/locations')
  } finally {
    loading.value = false
  }
}

function buildPayload(): LocationUpsertPayload | null {
  if (!form.name || !form.category || !form.latitude || !form.longitude || !form.radiusMeters || !form.description) {
    show('请填写所有必填项', 'error')
    return null
  }
  const lat = Number(form.latitude)
  const lng = Number(form.longitude)
  const radius = Number(form.radiusMeters)
  const cooldown = Number(form.cooldownSeconds)
  if (Number.isNaN(lat) || Number.isNaN(lng) || Number.isNaN(radius) || Number.isNaN(cooldown)) {
    show('纬度/经度/半径/冷却时间格式错误', 'error')
    return null
  }
  if (cooldown < 30 || cooldown > 3600) {
    show('冷却时间需在 30～3600 秒之间', 'error')
    return null
  }
  return {
    name: form.name.trim(),
    category: form.category.trim(),
    address: form.address.trim() || null,
    latitude: lat,
    longitude: lng,
    radiusMeters: radius,
    cooldownSeconds: cooldown,
    description: form.description.trim(),
  }
}

async function save() {
  const payload = buildPayload()
  if (!payload) return

  saving.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateLocation(editingId.value, payload)
      show('讲解点更新成功')
    } else {
      const created = await createLocation(payload)
      show('讲解点已创建，请继续上传图片与讲解录音')
      router.replace(`/locations/${created.id}/edit`)
      return
    }
    router.push('/locations')
  } catch (e) {
    show(e instanceof Error ? e.message : '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/locations')
}

async function onImageSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files?.length || !editingId.value) {
    if (!editingId.value) {
      show('请先保存讲解点，再上传图片', 'error')
    }
    return
  }
  uploadingImage.value = true
  try {
    for (const file of Array.from(files)) {
      const result = await uploadLocationImage(editingId.value, file)
      imageUrls.value = [...result.imageUrls]
      if (result.audioUrl) {
        audioUrl.value = result.audioUrl
      }
    }
    show('图片已上传')
  } catch (e) {
    show(e instanceof Error ? e.message : '图片上传失败', 'error')
  } finally {
    uploadingImage.value = false
    input.value = ''
  }
}

async function removeImage(url: string) {
  if (!editingId.value) return
  uploadingImage.value = true
  try {
    const result = await deleteLocationImage(editingId.value, url)
    imageUrls.value = [...result.imageUrls]
    show('图片已删除')
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  } finally {
    uploadingImage.value = false
  }
}

async function onAudioSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !editingId.value) {
    if (!editingId.value) {
      show('请先保存讲解点，再上传讲解录音', 'error')
    }
    return
  }
  uploadingAudio.value = true
  try {
    const result = await uploadLocationAudio(editingId.value, file)
    audioUrl.value = result.audioUrl ?? ''
    imageUrls.value = [...result.imageUrls]
    show('讲解录音已更新（将替换系统 TTS，仅播放您上传的录音）')
  } catch (e) {
    show(e instanceof Error ? e.message : '语音上传失败', 'error')
  } finally {
    uploadingAudio.value = false
    input.value = ''
  }
}

async function removeAudio() {
  if (!editingId.value || !audioUrl.value) return
  deletingAudio.value = true
  try {
    const result = await deleteLocationAudio(editingId.value)
    audioUrl.value = result.audioUrl ?? ''
    show('讲解录音已删除，游客端将使用文字 TTS 播报')
  } catch (e) {
    show(e instanceof Error ? e.message : '删除失败', 'error')
  } finally {
    deletingAudio.value = false
  }
}
</script>

<template>
  <section class="form-page">
    <p v-if="loading" class="hint pad">加载中…</p>

    <article v-else class="card form-panel">
      <form @submit.prevent="save">
        <section class="field">
          <label for="loc-name">名称 <span class="req">*</span></label>
          <input id="loc-name" v-model="form.name" placeholder="如：中心图书馆" />
        </section>

        <section class="field-row">
          <section class="field">
            <label for="loc-category">分类 <span class="req">*</span></label>
            <input id="loc-category" v-model="form.category" placeholder="教学楼 / 图书馆 / 校门" />
          </section>
          <section class="field">
            <label for="loc-address">地址</label>
            <input id="loc-address" v-model="form.address" placeholder="可选" />
          </section>
        </section>

        <section class="field">
          <label>位置 <span class="req">*</span></label>
          <LocationMapPicker
            ref="mapRef"
            :key="mapKey"
            :latitude="form.latitude"
            :longitude="form.longitude"
            @update:latitude="form.latitude = $event"
            @update:longitude="form.longitude = $event"
          />
        </section>

        <section class="field-row">
          <section class="field">
            <label for="loc-lat">纬度</label>
            <input id="loc-lat" v-model="form.latitude" type="number" step="any" placeholder="29.8215" />
          </section>
          <section class="field">
            <label for="loc-lng">经度</label>
            <input id="loc-lng" v-model="form.longitude" type="number" step="any" placeholder="106.4210" />
          </section>
        </section>

        <section class="field">
          <label for="loc-radius">触发半径（米）<span class="req">*</span></label>
          <input id="loc-radius" v-model="form.radiusMeters" type="number" min="10" max="1000" />
        </section>

        <section class="field">
          <label for="loc-cooldown">自动讲解冷却（秒）<span class="req">*</span></label>
          <input id="loc-cooldown" v-model="form.cooldownSeconds" type="number" min="30" max="3600" />
          <p class="hint">游客离开后再进入该围栏，需间隔此时间才会再次自动播报（30～3600 秒）</p>
        </section>

        <section class="field">
          <label for="loc-desc">文字介绍 <span class="req">*</span></label>
          <textarea
            id="loc-desc"
            v-model="form.description"
            rows="5"
            placeholder="请输入讲解文案；若未上传讲解录音，游客端将用系统语音朗读此内容"
          />
        </section>

        <section class="field media-section">
          <label>景点配图（可多张，游客端轮播）</label>
          <p class="hint">
            文件保存在 <code>uploads_media/picture/loc景点ID/</code>，使用上传时的原始文件名（同一景点不可重名）。
          </p>
          <p v-if="!canManageMedia" class="hint warn">新建讲解点请先点击「保存讲解点」，保存成功后可上传图片。</p>
          <section v-else class="upload-row">
            <label class="file-btn" :class="{ disabled: uploadingImage }">
              添加图片
              <input type="file" accept="image/*" multiple hidden :disabled="uploadingImage" @change="onImageSelected" />
            </label>
          </section>
          <ul v-if="imageUrls.length" class="image-gallery">
            <li v-for="(url, index) in imageUrls" :key="url" class="gallery-item">
              <img :src="mediaUrl(url)" :alt="`配图 ${index + 1}`" class="preview-img" />
              <button
                v-if="canManageMedia"
                type="button"
                class="btn-remove"
                :disabled="uploadingImage"
                @click="removeImage(url)"
              >
                删除
              </button>
            </li>
          </ul>
          <p v-else-if="canManageMedia" class="hint">暂无配图，点击「添加图片」从电脑选择 jpg/png/webp 等。</p>
          <p v-if="uploadingImage" class="hint">图片处理中…</p>
        </section>

        <section class="field media-section">
          <label>讲解录音（每个景点一份，可更新）</label>
          <p class="hint">
            保存在 <code>uploads_media/listen/loc景点ID/</code>。上传专业录音后，游客端<strong>仅播放录音</strong>，不再使用系统 TTS。
          </p>
          <p v-if="!canManageMedia" class="hint warn">请先保存讲解点后再上传录音。</p>
          <section v-else class="upload-row audio-actions">
            <label class="file-btn" :class="{ disabled: uploadingAudio }">
              {{ audioUrl ? '更换录音' : '上传录音' }}
              <input type="file" accept="audio/*" hidden :disabled="uploadingAudio" @change="onAudioSelected" />
            </label>
            <button
              v-if="audioUrl"
              type="button"
              class="btn btn-secondary btn-sm"
              :disabled="deletingAudio || uploadingAudio"
              @click="removeAudio"
            >
              删除录音
            </button>
          </section>
          <audio v-if="audioUrl" :src="mediaUrl(audioUrl)" controls class="preview-audio" />
          <p v-if="uploadingAudio" class="hint">录音上传中（将自动删除旧文件）…</p>
        </section>

        <section class="actions">
          <button type="submit" class="btn btn-login" :disabled="saving">
            {{ saving ? '保存中…' : isEdit ? '更新讲解点' : '保存讲解点' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="goBack">返回列表</button>
        </section>
      </form>
    </article>
  </section>
</template>

<style scoped>
.form-page {
  width: 100%;
}

.form-panel {
  width: 100%;
  padding: 20px;
}

.req {
  color: #c62828;
}

.upload-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.audio-actions {
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
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

.file-btn:hover:not(.disabled) {
  background: rgba(155, 196, 176, 0.2);
}

.file-btn.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.gallery-item {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--stroke-soft);
  background: var(--surface-soft-alt);
}

.preview-img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.btn-remove {
  width: 100%;
  padding: 6px;
  border: none;
  border-top: 1px solid var(--stroke-soft);
  background: rgba(198, 40, 40, 0.08);
  color: #c62828;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn-remove:hover:not(:disabled) {
  background: rgba(198, 40, 40, 0.15);
}

.preview-audio {
  margin-top: 8px;
  width: 100%;
}

.media-section code {
  font-size: 11px;
  background: var(--surface-soft-alt);
  padding: 1px 4px;
  border-radius: 4px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.btn-sm {
  padding: 8px 12px;
  font-size: 13px;
}

.pad {
  padding: 12px;
}

.hint {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 4px;
}

.hint.warn {
  color: #b45309;
}
</style>
