<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'


const props = defineProps<{
  latitude: string
  longitude: string
}>()

const emit = defineEmits<{
  'update:latitude': [value: string]
  'update:longitude': [value: string]
}>()

const CAMPUS_CENTER: L.LatLngExpression = [29.8215, 106.421]
const CAMPUS_ZOOM = 16

const mapRoot = ref<HTMLDivElement | null>(null)
let map: L.Map | null = null
let marker: L.Marker | null = null
let syncingFromMap = false

function fixLeafletIcons() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  delete (L.Icon.Default.prototype as any)._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
  })
}

function addTileLayer(target: L.Map) {
  L.tileLayer(
    'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&ltype=11&x={x}&y={y}&z={z}',
    {
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 19,
      attribution: '&copy; 高德地图',
    },
  ).addTo(target)
}

function parseCoord(value: string): number | null {
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function setMarker(lat: number, lng: number, pan = true) {
  if (!map) return
  const point: L.LatLngExpression = [lat, lng]
  if (marker) {
    marker.setLatLng(point)
  } else {
    marker = L.marker(point, { draggable: true }).addTo(map)
    marker.on('dragend', () => {
      const pos = marker?.getLatLng()
      if (!pos) return
      applyCoords(pos.lat, pos.lng)
    })
  }
  if (pan) {
    map.panTo(point)
  }
}

function applyCoords(lat: number, lng: number) {
  syncingFromMap = true
  emit('update:latitude', lat.toFixed(7))
  emit('update:longitude', lng.toFixed(7))
  syncingFromMap = false
}

function onMapClick(e: L.LeafletMouseEvent) {
  setMarker(e.latlng.lat, e.latlng.lng, false)
  applyCoords(e.latlng.lat, e.latlng.lng)
}

function syncFromInputs() {
  if (syncingFromMap || !map) return
  const lat = parseCoord(props.latitude)
  const lng = parseCoord(props.longitude)
  if (lat == null || lng == null) return
  setMarker(lat, lng, false)
}

function refreshMapSize() {
  if (!map) return
  map.invalidateSize(true)
}

onMounted(async () => {
  await nextTick()
  if (!mapRoot.value) return
  fixLeafletIcons()

  map = L.map(mapRoot.value, {
    center: CAMPUS_CENTER,
    zoom: CAMPUS_ZOOM,
    scrollWheelZoom: true,
    preferCanvas: true,
  })

  addTileLayer(map)
  map.on('click', onMapClick)
  syncFromInputs()

  window.setTimeout(refreshMapSize, 0)
  window.setTimeout(refreshMapSize, 200)
  window.setTimeout(refreshMapSize, 500)
})

watch(
  () => [props.latitude, props.longitude],
  () => syncFromInputs(),
)

onUnmounted(() => {
  map?.off('click', onMapClick)
  map?.remove()
  map = null
  marker = null
})

defineExpose({ refreshMapSize })
</script>

<template>
  <section class="map-picker">
    <p class="map-tip">在地图上点击选点，或拖动标记调整位置；也可在下方手动输入经纬度。</p>
    <div ref="mapRoot" class="map-canvas" />
  </section>
</template>

<style scoped>
.map-picker {
  margin-bottom: 14px;
}

.map-tip {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.5;
}

.map-canvas {
  width: 100%;
  height: 320px;
  min-height: 320px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--stroke-soft);
  background: #e8e4dc;
  z-index: 0;
}

:deep(.leaflet-container) {
  width: 100%;
  height: 100%;
  font-family: inherit;
}
</style>
