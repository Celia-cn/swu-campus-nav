<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearToken } from '@/api/http'
import { fetchMe } from '@/api/admin'
import HeaderDateWeather from '@/components/HeaderDateWeather.vue'
import type { UserProfile } from '@/types/api'

const router = useRouter()
const route = useRoute()
const profile = ref<UserProfile | null>(null)
const sidebarEl = ref<HTMLElement | null>(null)

const navItems = [
  { path: '/dashboard', label: '数据统计' },
  { path: '/locations', label: '讲解点管理' },
  { path: '/routes', label: '路线管理' },
  { path: '/users', label: '用户管理' },
  { path: '/feedback', label: '反馈中心' },
  { path: '/rag', label: 'AI 知识库' },
]

const pageTitle = computed(() => {
  if (route.meta.title && typeof route.meta.title === 'string') {
    return route.meta.title
  }
  const item = navItems.find((n) => n.path === route.path)
  return item?.label ?? '管理后台'
})

const username = computed(() => profile.value?.username ?? '')


const showBack = computed(
    () => route.name === 'location-create' || route.name === 'location-edit' || route.name === 'NewRoute',
)

function isNavActive(itemPath: string) {
  if (itemPath === '/locations') {
    return route.path === itemPath || route.path.startsWith('/locations/')
  }
  if (itemPath === '/routes') {
    return route.path === itemPath || route.path.startsWith('/routes/')
  }
  return route.path === itemPath
}

function goBack() {
  if (route.name === 'NewRoute' || route.path.startsWith('/routes/')) {
    router.push('/routes')
  } else {
    router.push('/locations')
  }
}

function navigateNav(path: string) {
  if (route.path === path) return
  router.push(path)
}

watch(
    () => route.path,
    () => {
      sidebarEl.value?.scrollTo({ top: 0 })
    },
)

onMounted(async () => {
  try {
    profile.value = await fetchMe()
  } catch {
    profile.value = null
  }
})

function logout() {
  clearToken()
  router.push('/login')
}
</script>

<template>
  <section class="admin-shell">
    <aside ref="sidebarEl" class="sidebar">
      <header class="brand">
        <img class="brand-logo" src="/app-logo.png" alt="西南大学智畅游" />
        <section class="brand-text">
          <strong>西南大学智畅游</strong>
          <small>管理后台</small>
        </section>
      </header>

      <nav class="nav">
        <button
            v-for="item in navItems"
            :key="item.path"
            type="button"
            class="nav-link"
            :class="{ active: isNavActive(item.path) }"
            @click="navigateNav(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>

      <footer class="sidebar-foot">
        <p v-if="profile" class="user-line">{{ username }}</p>
        <button type="button" class="btn-logout" @click="logout">退出登录</button>
      </footer>

    </aside>

    <section class="main-wrap">
      <main class="main">
        <header class="page-header">
          <section class="page-header-row">
            <section class="page-header-left">
              <button v-if="showBack" type="button" class="btn-back" @click="goBack">
                返回
              </button>
              <h1 class="page-title">{{ pageTitle }}</h1>
            </section>
            <HeaderDateWeather />
          </section>
          <hr class="page-divider" />
        </header>

        <section class="content">
          <RouterView />
        </section>
      </main>
    </section>
  </section>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background-color: var(--surface-soft);
  background-image: url('/background.jpg');
  /* 铺满视口，左上花与右上叶都落在屏幕角落，避免被裁切 */
  background-size: 100% 100%;
  background-position: center;
  background-repeat: no-repeat;
  /* 固定到视口，左下花、右上叶始终贴在屏幕角落 */
  background-attachment: fixed;
}

.sidebar {
  position: sticky;
  top: 0;
  align-self: flex-start;
  width: var(--sidebar-width);
  height: 100vh;
  max-height: 100vh;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  box-sizing: border-box;
  overflow-y: auto;
  background: linear-gradient(
      180deg,
      rgba(255, 254, 254, 0.72) 0%,
      rgba(255, 254, 254, 0.42) 50%,
      rgba(255, 254, 254, 0.12) 100%
  );
  border-right: 1px solid rgba(224, 228, 234, 0.75);
}

.brand {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 28px;
  padding: 6px 2px;
}

.brand-logo {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  object-fit: contain;
  flex-shrink: 0;
}

.brand-text strong {
  display: block;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
}

.brand-text small {
  display: block;
  margin-top: 5px;
  color: var(--text-sub);
  font-size: 14px;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-main);
  font: inherit;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}

.nav-link:hover {
  background: rgba(155, 196, 176, 0.18);
}

.nav-link.active {
  background: linear-gradient(90deg, rgba(253, 224, 141, 0.5), rgba(155, 196, 176, 0.35));
  color: var(--brand-accent-dark);
  font-weight: 600;
}

.sidebar-foot {
  flex-shrink: 0;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(217, 235, 208, 0.5);
  background: transparent;
}

.user-line {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text-sub);
}

.btn-logout {
  width: 100%;
  padding: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  background: #fff;
  border: 1px solid #e0e4ea;
  border-radius: 6px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(26, 31, 54, 0.06);
}

.btn-logout:hover {
  color: var(--text-main);
  border-color: var(--stroke-soft);
}

.main-wrap {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
  background: transparent;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.page-header {
  flex-shrink: 0;
  padding: 24px 28px 0;
  background: transparent;
}

.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-main);
  letter-spacing: 0.02em;
}

.page-divider {
  margin: 0;
  border: none;
  border-top: 1px solid var(--divider-color);
  height: 0;
}

.btn-back {
  flex-shrink: 0;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-accent-dark);
  background: rgba(155, 196, 176, 0.2);
  border: 1px solid var(--stroke-soft);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.btn-back:hover {
  background: rgba(155, 196, 176, 0.35);
}

.content {
  flex: 1;
  padding: 20px 28px 28px;
  overflow: auto;
}

@media (max-width: 900px) {
  .admin-shell {
    flex-direction: column;
  }

  .sidebar {
    position: static;
    width: 100%;
    height: auto;
    max-height: none;
    border-right: none;
    border-bottom: 1px solid #e0e4ea;
  }

  .nav {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .page-title {
    font-size: 22px;
  }

  .page-header-row {
    flex-wrap: wrap;
  }
}
</style>
